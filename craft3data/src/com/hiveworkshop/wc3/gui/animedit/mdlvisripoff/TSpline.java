package com.hiveworkshop.wc3.gui.animedit.mdlvisripoff;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.etheller.warsmash.parsers.mdlx.InterpolationType;
import com.hiveworkshop.wc3.mdl.AnimFlag;
import com.hiveworkshop.wc3.mdl.QuaternionRotation;

import net.miginfocom.swing.MigLayout;

public class TSpline extends JPanel {
	private final TTan der; // in mdlvis this was just called der, and whatever, I'm copying them right now
	private int currentFrame;
	private final CurveRenderer curveRenderer;
	private final JSpinner tensionSpinner;
	private final JSpinner continuitySpinner;
	private final JSpinner biasSpinner;
	private AnimFlag timeline;

	public TSpline(final TTan der) {
		super(new MigLayout("fillx, filly", "[grow][]", "[][grow][][][]"));
		this.der = der;
//		der = new TTan();
//		der.cur = new AnimFlag.Entry(null, null);
//		der.next = new AnimFlag.Entry(null, null);
//		der.prev = new AnimFlag.Entry(null, null);
//		der.tang = new AnimFlag.Entry(null, null);
		add(new JLabel("Curve properties"), "growx");
		add(new JButton("-"), "wrap");
		curveRenderer = new CurveRenderer();
		curveRenderer.setBackground(Color.WHITE);
		add(curveRenderer, "wrap");

		add(new JLabel("Tension:"), "growx");
		tensionSpinner = new JSpinner(new SpinnerNumberModel(0.0, Long.MIN_VALUE, Long.MAX_VALUE, 0.01));
		add(tensionSpinner, "wrap");
		add(new JLabel("Continuity:"), "growx");
		continuitySpinner = new JSpinner(new SpinnerNumberModel(0.0, Long.MIN_VALUE, Long.MAX_VALUE, 0.01));
		add(continuitySpinner, "wrap");
		add(new JLabel("Bias:"), "growx");
		biasSpinner = new JSpinner(new SpinnerNumberModel(0.0, Long.MIN_VALUE, Long.MAX_VALUE, 0.01));
		add(biasSpinner, "wrap");
	}

	public void setTimeline(final AnimFlag timeline) {
		this.timeline = timeline;
	}

	public void initFromKF() {
		if (timeline == null) {
			setVisible(false);
			return;
		}
		int len, num;
		len = timeline.size();
		num = timeline.ceilIndex(currentFrame);
		if ((num == 0) || (num >= (len - 1)) || (timeline.getInterpType() != InterpolationType.HERMITE)) {
			setVisible(false);
			return;
		}
		if ((num >= len) || (num < 0) || (timeline.getTimes().get(num) != currentFrame)) {
			setVisible(false);
			return;
		}

		der.tang = timeline.getEntry(num);
		der.cur = timeline.getEntry(num);
		der.prev = timeline.getEntry(num - 1);
		der.next = timeline.getEntry(num + 1);
		der.calcSplineParameters(timeline.getValues().get(0) instanceof QuaternionRotation, 4);

		tensionSpinner.setValue(Math.round(der.tension * 100));
		continuitySpinner.setValue(Math.round(der.continuity * 100));
		biasSpinner.setValue(Math.round(der.bias * 100));

		setVisible(true);

		curveRenderer.repaint();
	}

	public float getTension() {
		return der.tension;
	}

	public float getContinuity() {
		return der.continuity;
	}

	public float getBias() {
		return der.bias;
	}

	public void setTCB(final float tension, final float continuity, final float bias) {
		final AnimFlag.Entry it;
		final int i;

		der.tension = tension;
		der.continuity = continuity;
		der.bias = bias;

		i = timeline.ceilIndex(currentFrame);
		der.cur = timeline.getEntry(i);
		der.prev = timeline.getEntry(i - 1);
		der.next = timeline.getEntry(i + 1);

		der.isLogsReady = false;
		if (timeline.getValues().get(0) instanceof QuaternionRotation) {
			der.calcDerivative4D();
		} else {
			der.calcDerivativeXD(TTan.getSizeOfElement(timeline.getValues().get(0)));
		}

		throw new UnsupportedOperationException(
				"Not finished here, need to have shared access to storing keyframe data and UndoManager");
	}

	private final class CurveRenderer extends JPanel {

		private final AnimFlag.Entry itd = new AnimFlag.Entry(0, 0.0, 0.0, 0.0);
		private final AnimFlag.Entry its = new AnimFlag.Entry(0, 0.0, 0.0, 0.0);

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);
			final Rectangle rect = getBounds();
			int i;
			final float pixPerUnitX = 0.005f * rect.width;
			final float pixPerUnitY = rect.height / 130f;
			float renderX = rect.x, renderY = rect.y + rect.height;

			g.setColor(Color.BLUE);
			g.drawRect(rect.x, rect.y, rect.width, rect.height);

			TTan.assignSubscript(der.prev.value, 0, 0);
			TTan.assignSubscript(der.cur.value, 0, 100);
			TTan.assignSubscript(der.next.value, 0, 0);
			der.calcDerivativeXD(1);
			g.setColor(Color.BLACK);
			i = 0;
			final InterpolationType interpType = timeline.getInterpType();
			do {
				itd.set(der.tang);
				TTan.assignSubscript(itd.value, 0, 100);
				itd.time = 100;

				its.time = 0;
				TTan.assignSubscript(its.value, 0, 0);
				TTan.assignSubscript(its.inTan, 0, 0);
				TTan.assignSubscript(its.outTan, 0, 0);

				switch (interpType) {
				case HERMITE:
					TTan.spline(i, its, itd);
					break;
				case BEZIER:
					TTan.bezInterp(i, its, itd);
					break;
				default:
					break;
				}
				final float newRenderX = Math.round(pixPerUnitX * i);
				final float newRenderY = rect.height
						- Math.round(pixPerUnitY * TTan.getSubscript(itd.value, 0).floatValue());
				g.drawLine((int) renderX, (int) renderY, (int) newRenderX, (int) newRenderY);
				renderX = newRenderX;
				renderY = newRenderY;
				i += 2;
			} while (i <= 100);

			// Second half of the Curve (Spline?)

			i = 100;
			do {
				TTan.assignSubscript(itd.value, 0, 0);
				itd.time = 200;
				TTan.assignSubscript(itd.inTan, 0, 0);
				TTan.assignSubscript(itd.outTan, 0, 0);

				its.set(der.tang);
				its.time = 100;
				TTan.assignSubscript(its.value, 0, 100);

				switch (interpType) {
				case HERMITE:
					TTan.spline(i, its, itd);
					break;
				case BEZIER:
					TTan.bezInterp(i, its, itd);
					break;
				default:
					break;
				}
				final float newRenderX = Math.round(pixPerUnitX * i);
				final float newRenderY = rect.height
						- Math.round(pixPerUnitY * TTan.getSubscript(itd.value, 0).floatValue());
				g.drawLine((int) renderX, (int) renderY, (int) newRenderX, (int) newRenderY);
				renderX = newRenderX;
				renderY = newRenderY;
				i += 2;
			} while (i <= 100);

			// Central line
			g.setColor(Color.RED);
			g.drawLine((Math.round(pixPerUnitX * 100)), rect.height, Math.round(pixPerUnitX * 100),
					rect.height - Math.round(pixPerUnitY * 100));
		}
	}

	public void setSelection(final int currentTime, final AnimFlag timeline) {
		currentFrame = currentTime;
		this.timeline = timeline;
		initFromKF();
	}

	public void setEmptySelection() {
		this.timeline = null;
		initFromKF();
	}
}
