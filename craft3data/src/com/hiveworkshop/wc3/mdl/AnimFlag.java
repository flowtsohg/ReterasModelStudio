package com.hiveworkshop.wc3.mdl;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.etheller.warsmash.parsers.mdlx.AnimationMap;
import com.etheller.warsmash.parsers.mdlx.InterpolationType;
import com.etheller.warsmash.parsers.mdlx.timeline.MdlxFloatArrayTimeline;
import com.etheller.warsmash.parsers.mdlx.timeline.MdlxFloatTimeline;
import com.etheller.warsmash.parsers.mdlx.timeline.MdlxTimeline;
import com.etheller.warsmash.parsers.mdlx.timeline.MdlxUInt32Timeline;
import com.etheller.warsmash.util.MdlUtils;
import com.hiveworkshop.wc3.gui.animedit.BasicTimeBoundProvider;
import com.hiveworkshop.wc3.gui.modelviewer.AnimatedRenderEnvironment;
import com.hiveworkshop.wc3.units.objectdata.War3ID;
import com.hiveworkshop.wc3.util.MathUtils;

/**
 * A java class for MDL "motion flags," such as Alpha, Translation, Scaling, or
 * Rotation. AnimFlags are not "real" things from an MDL and are given this name
 * by me, as an invented java class to simplify the programming
 *
 * Eric Theller 11/5/2011
 */
public class AnimFlag {
	// Types of AnimFlags:
	// 0 Alpha
	public static final int ALPHA = 0;
	// 1 Scaling
	public static final int SCALING = 1;
	// 2 Rotation
	public static final int ROTATION = 2;
	// 3 Translation
	public static final int TRANSLATION = 3;
	// 4 Color
	public static final int COLOR = 4;
	// 5 TextureID
	public static final int TEXTUREID = 5;

	/**
	 * Use for titles like "Intensity", "AmbIntensity", and other extraneous things
	 * not included in the options above.
	 */
	public static final int OTHER_TYPE = 0;

	String name;
	InterpolationType interpolationType = InterpolationType.DONT_INTERP;
	Integer globalSeq;
	int globalSeqId = -1;
	boolean hasGlobalSeq = false;
	List<Integer> times = new ArrayList<>();
	List<Object> values = new ArrayList<>();
	List<Object> inTans = new ArrayList<>();
	List<Object> outTans = new ArrayList<>();
	int typeid = 0;
	int vectorSize = 1;
	boolean isFloat = true;

	public boolean equals(final AnimFlag o) {
		boolean does = o instanceof AnimFlag;
		if (!does) {
			return false;
		}
		final AnimFlag af = o;
		does = (name.equals(af.getName())) ||
			   (hasGlobalSeq == af.hasGlobalSeq) ||
			   (values.equals(af.values) && (globalSeq == null ? af.globalSeq == null : globalSeq.equals(af.globalSeq))
				&& (interpolationType == af.interpolationType)
				&& (inTans == null ? af.inTans == null : inTans.equals(af.inTans))
				&& (outTans == null ? af.outTans == null : outTans.equals(af.outTans)) && (typeid == af.typeid));
		return does;
	}

	public void setGlobSeq(final Integer inte) {
		globalSeq = inte;
		hasGlobalSeq = inte != null;
	}

	public Integer getGlobalSeq() {
		return globalSeq;
	}

	public void setGlobalSeq(final Integer globalSeq) {
		this.globalSeq = globalSeq;
	}
	
	public AnimFlag(final MdlxTimeline<?> timeline) {
		name = AnimationMap.ID_TO_TAG.get(timeline.name).getMdlToken();
		generateTypeId();

		interpolationType = timeline.interpolationType;

		int globalSequenceId = timeline.globalSequenceId;
		if (globalSequenceId >= 0) {
			setGlobalSeqId(globalSequenceId);
			setHasGlobalSeq(true);
		}

		long[] frames = timeline.frames;
		Object[] values = timeline.values;
		Object[] inTans = timeline.inTans;
		Object[] outTans = timeline.outTans;

		if (frames.length > 0) {
			final boolean hasTangents = interpolationType.tangential();

			Object firstValue = values[0];

			if (firstValue instanceof float[]) {
				vectorSize = ((float[])firstValue).length;
			} else {
				isFloat = false;
				vectorSize = ((long[])firstValue).length;
			}

			for (int i = 0, l = frames.length; i < l; i++) {
				Object value = values[i];
				Object valueAsObject;
				Object inTanAsObject = null;
				Object outTanAsObject = null;

				if (isFloat) {
					float[] valueAsArray = (float[])value;

					if (vectorSize == 1) {
						valueAsObject = Double.valueOf(valueAsArray[0]);

						if (hasTangents) {
							inTanAsObject = Double.valueOf(((float[])inTans[i])[0]);
							outTanAsObject = Double.valueOf(((float[])outTans[i])[0]);
						}
					} else if (vectorSize == 3) {
						valueAsObject = new Vertex(valueAsArray);

						if (hasTangents) {
							inTanAsObject = new Vertex((float[])inTans[i]);
							outTanAsObject = new Vertex((float[])outTans[i]);
						}
					} else {
						valueAsObject = new QuaternionRotation(valueAsArray);

						if (hasTangents) {
							inTanAsObject = new QuaternionRotation((float[])inTans[i]);
							outTanAsObject = new QuaternionRotation((float[])outTans[i]);
						}
					}
				} else {
					valueAsObject = Integer.valueOf((int)((long[])value)[0]);

					if (hasTangents) {
						inTanAsObject = Integer.valueOf((int)((long[])inTans[i])[0]);
						outTanAsObject = Integer.valueOf((int)((long[])outTans[i])[0]);
					}
				}

				addEntry((int)frames[i], valueAsObject, inTanAsObject, outTanAsObject);
			}
		}
	}

	public MdlxTimeline<Object> toMdlx(final TimelineContainer container) {
		MdlxTimeline timeline;

		if (isFloat) {
			if (vectorSize == 1) {
				timeline = new MdlxFloatTimeline();
			} else if (vectorSize == 3) {
				timeline = new MdlxFloatArrayTimeline(3);
			} else {
				timeline = new MdlxFloatArrayTimeline(4);
			}
		} else {
			timeline = new MdlxUInt32Timeline();
		}

		timeline.name = getWar3ID(container);
		timeline.interpolationType = interpolationType;
		timeline.globalSequenceId = getGlobalSeqId();

		List<Integer> times = getTimes();
		List<Object> values = getValues();
		List<Object> inTans = getInTans();
		List<Object> outTans = getOutTans();

		long[] tempFrames = new long[times.size()];
		Object[] tempValues = new Object[times.size()];
		Object[] tempInTans = new Object[times.size()];
		Object[] tempOutTans = new Object[times.size()];

		final boolean hasTangents = timeline.interpolationType.tangential();

		for (int i = 0, l = times.size(); i < l; i++) {
			Object value = values.get(i);

			tempFrames[i] = times.get(i).longValue();
			
			if (isFloat) {
				if (vectorSize == 1) {
					tempValues[i] = new float[] { ((Double)value).floatValue() };

					if (hasTangents) {
						tempInTans[i] = new float[] { ((Double)inTans.get(i)).floatValue() };
						tempOutTans[i] = new float[] { ((Double)outTans.get(i)).floatValue() };
					} else {
						tempInTans[i] = new float[] { 0 };
						tempOutTans[i] = new float[] { 0 };
					}
				} else if (vectorSize == 3) {
					tempValues[i] = ((Vertex)value).toFloatArray();

					if (hasTangents) {
						tempInTans[i] = ((Vertex)inTans.get(i)).toFloatArray();
						tempOutTans[i] = ((Vertex)outTans.get(i)).toFloatArray();
					} else {
						tempInTans[i] = (new Vertex()).toFloatArray();
						tempOutTans[i] = (new Vertex()).toFloatArray();
					}
				} else {
					tempValues[i] = ((QuaternionRotation)value).toFloatArray();

					if (hasTangents) {
						tempInTans[i] = ((QuaternionRotation)inTans.get(i)).toFloatArray();
						tempOutTans[i] = ((QuaternionRotation)outTans.get(i)).toFloatArray();
					} else {
						tempInTans[i] = (new QuaternionRotation()).toFloatArray();
						tempOutTans[i] = (new QuaternionRotation()).toFloatArray();
					}
				}
			} else {
				tempValues[i] = (new long[] { ((Integer)value).longValue() });

				if (hasTangents) {
					tempInTans[i] = new long[] { ((Integer)inTans.get(i)).longValue() };
					tempOutTans[i] = new long[] { ((Integer)outTans.get(i)).longValue() };
				} else {
					tempInTans[i] = new long[] { 0 };
					tempOutTans[i] = new long[] { 0 };
				}
			}
		}

		timeline.frames = tempFrames;
		timeline.values = tempValues;
		timeline.inTans = tempInTans;
		timeline.outTans = tempOutTans;

		return timeline;
	}

	// end special constructors
	public AnimFlag(final String title, final List<Integer> times, final List values) {
		this.name = title;
		this.times = times;
		this.values = values;
	}

	public AnimFlag(final String title) {
		this.name = title;
	}

	public War3ID getWar3ID(final TimelineContainer container) {
		AnimationMap id = getAnimationMap(container);

		if (id == null) {
			throw new RuntimeException("Got an unknown timeline name: " + name);
		}

		return id.getWar3id();
	}

	public AnimationMap getAnimationMap(final TimelineContainer container) {
		if (container instanceof Layer) {
			switch (name) {
			case MdlUtils.TOKEN_TEXTURE_ID: return AnimationMap.KMTF;
			case MdlUtils.TOKEN_ALPHA: return AnimationMap.KMTA;
			case MdlUtils.TOKEN_EMISSIVE_GAIN: return AnimationMap.KMTE;
			case MdlUtils.TOKEN_FRESNEL_COLOR: return AnimationMap.KFC3;
			case MdlUtils.TOKEN_FRESNEL_OPACITY: return AnimationMap.KFCA;
			case MdlUtils.TOKEN_FRESNEL_TEAM_COLOR: return AnimationMap.KFTC;
			}
		} else if (container instanceof TextureAnim) {
			switch (name) {
			case MdlUtils.TOKEN_TRANSLATION: return AnimationMap.KTAT;
			case MdlUtils.TOKEN_ROTATION: return AnimationMap.KTAR;
			case MdlUtils.TOKEN_SCALING: return AnimationMap.KTAS;
			}
		} else if (container instanceof GeosetAnim) {
			switch (name) {
			case MdlUtils.TOKEN_ALPHA: return AnimationMap.KGAO;
			case MdlUtils.TOKEN_COLOR: return AnimationMap.KGAC;
			}
		} else if (container instanceof Light) {
			switch (name) {
			case MdlUtils.TOKEN_ATTENUATION_START: return AnimationMap.KLAS;
			case MdlUtils.TOKEN_ATTENUATION_END: return AnimationMap.KLAE;
			case MdlUtils.TOKEN_COLOR: return AnimationMap.KLAC;
			case MdlUtils.TOKEN_INTENSITY: return AnimationMap.KLAI;
			case MdlUtils.TOKEN_AMB_INTENSITY: return AnimationMap.KLBI;
			case MdlUtils.TOKEN_AMB_COLOR: return AnimationMap.KLBC;
			case MdlUtils.TOKEN_VISIBILITY: return AnimationMap.KLAV;
			}
		} else if (container instanceof Attachment) {
			switch (name) {
			case MdlUtils.TOKEN_VISIBILITY: return AnimationMap.KATV;
			}
		} else if (container instanceof ParticleEmitter) {
			switch (name) {
			case MdlUtils.TOKEN_EMISSION_RATE: return AnimationMap.KPEE;
			case MdlUtils.TOKEN_GRAVITY: return AnimationMap.KPEG;
			case MdlUtils.TOKEN_LONGITUDE: return AnimationMap.KPLN;
			case MdlUtils.TOKEN_LATITUDE: return AnimationMap.KPLT;
			case MdlUtils.TOKEN_LIFE_SPAN: return AnimationMap.KPEL;
			case MdlUtils.TOKEN_INIT_VELOCITY: return AnimationMap.KPES;
			case MdlUtils.TOKEN_VISIBILITY: return AnimationMap.KPEV;
			}
		} else if (container instanceof ParticleEmitter2) {
			switch (name) {
			case MdlUtils.TOKEN_SPEED: return AnimationMap.KP2S;
			case MdlUtils.TOKEN_VARIATION: return AnimationMap.KP2R;
			case MdlUtils.TOKEN_LATITUDE: return AnimationMap.KP2L;
			case MdlUtils.TOKEN_GRAVITY: return AnimationMap.KP2G;
			case MdlUtils.TOKEN_EMISSION_RATE: return AnimationMap.KP2E;
			case MdlUtils.TOKEN_LENGTH: return AnimationMap.KP2N;
			case MdlUtils.TOKEN_WIDTH: return AnimationMap.KP2W;
			case MdlUtils.TOKEN_VISIBILITY: return AnimationMap.KP2V;
			}
		} else if (container instanceof ParticleEmitterPopcorn) {
			switch (name) {
			case MdlUtils.TOKEN_ALPHA: return AnimationMap.KPPA;
			case MdlUtils.TOKEN_COLOR: return AnimationMap.KPPC;
			case MdlUtils.TOKEN_EMISSION_RATE: return AnimationMap.KPPE;
			case MdlUtils.TOKEN_LIFE_SPAN: return AnimationMap.KPPL;
			case MdlUtils.TOKEN_SPEED: return AnimationMap.KPPS;
			case MdlUtils.TOKEN_VISIBILITY: return AnimationMap.KPPV;
			}
		} else if (container instanceof RibbonEmitter) {
			switch (name) {
			case MdlUtils.TOKEN_HEIGHT_ABOVE: return AnimationMap.KRHA;
			case MdlUtils.TOKEN_HEIGHT_BELOW: return AnimationMap.KRHB;
			case MdlUtils.TOKEN_ALPHA: return AnimationMap.KRAL;
			case MdlUtils.TOKEN_COLOR: return AnimationMap.KRCO;
			case MdlUtils.TOKEN_TEXTURE_SLOT: return AnimationMap.KRTX;
			case MdlUtils.TOKEN_VISIBILITY: return AnimationMap.KRVS;
			}
		} else if (container instanceof Camera.SourceNode) {
			switch (name) {
			case MdlUtils.TOKEN_TRANSLATION: return AnimationMap.KCTR;
			case MdlUtils.TOKEN_ROTATION: return AnimationMap.KCRL;
			}
		} else if (container instanceof Camera.TargetNode) {
			switch (name) {
			case MdlUtils.TOKEN_TRANSLATION: return AnimationMap.KTTR;
			}
		} 
	
		if (container instanceof IdObject) {
			switch (name) {
			case MdlUtils.TOKEN_TRANSLATION: return AnimationMap.KGTR;
			case MdlUtils.TOKEN_ROTATION: return AnimationMap.KGRT;
			case MdlUtils.TOKEN_SCALING: return AnimationMap.KGSC;
			}
		}

		return null;
	}

	public static AnimFlag createEmpty2018(final String title, final InterpolationType interpolationType,
			final Integer globalSeq) {
		final AnimFlag flag = new AnimFlag();
		flag.name = title;
		flag.interpolationType = interpolationType;
		flag.generateTypeId();
		flag.setGlobSeq(globalSeq);
		return flag;
	}

	public void setInterpType(final InterpolationType interpolationType) {
		this.interpolationType = interpolationType;
	}

	public InterpolationType getInterpType() {
		return interpolationType;
	}

	public int size() {
		return times.size();
	}

	public int length() {
		return times.size();
	}

	public AnimFlag(final AnimFlag af) {
		name = af.name;
		globalSeq = af.globalSeq;
		globalSeqId = af.globalSeqId;
		hasGlobalSeq = af.hasGlobalSeq;
		interpolationType = af.interpolationType;
		typeid = af.typeid;
		times = new ArrayList<>(af.times);
		values = deepCopy(af.values);
		inTans = deepCopy(af.inTans);
		outTans = deepCopy(af.outTans);
	}

	public static AnimFlag buildEmptyFrom(final AnimFlag af) {
		final AnimFlag na = new AnimFlag(af.name);
		na.globalSeq = af.globalSeq;
		na.globalSeqId = af.globalSeqId;
		na.hasGlobalSeq = af.hasGlobalSeq;
		na.typeid = af.typeid;
		return na;
	}

	public void generateTypeId() {
		typeid = 0;
		if (name.equals("Scaling")) {
			typeid = 1;
		} else if (name.equals("Rotation")) {
			typeid = 2;
		} else if (name.equals("Translation")) {
			typeid = 3;
		} else if (name.equals("TextureID"))// aflg.title.equals("Visibility")
												// || -- 100.088% visible in
												// UndeadCampaign3D OutTans! Go
												// look!
		{
			typeid = 5;
		} else if (name.contains("Color"))// AmbColor
		{
			typeid = 4;
		}
	}

	public int getGlobalSeqId() {
		return globalSeqId;
	}

	public void setGlobalSeqId(final int globalSeqId) {
		this.globalSeqId = globalSeqId;
	}

	public boolean hasGlobalSeq() {
		return hasGlobalSeq;
	}

	public void setHasGlobalSeq(final boolean hasGlobalSeq) {
		this.hasGlobalSeq = hasGlobalSeq;
	}

	public void addEntry(final Integer time, final Object value) {
		times.add(time);
		values.add(value);
	}

	public void addEntry(final Integer time, final Object value, final Object inTan, final Object outTan) {
		times.add(time);
		values.add(value);

		if (inTan != null && outTan != null) {
			inTans.add(inTan);
			outTans.add(outTan);
		}
	}

	public void setEntry(final Integer time, final Object value) {
		for (int index = 0; index < times.size(); index++) {
			if (times.get(index).equals(time)) {
				values.set(index, value);
				if (tans()) {
					inTans.set(index, value);
					outTans.set(index, value);
				}
			}
		}
	}

	/**
	 * This class is a small shell of an example for how my "AnimFlag" class
	 * should've been implemented. It's currently only used for the
	 * {@link AnimFlag#getEntry(int)} function.
	 *
	 * @author Eric
	 *
	 */
	public static class Entry {
		public Integer time;
		public Object value, inTan, outTan;

		public Entry(final Integer time, final Object value, final Object inTan, final Object outTan) {
			super();
			this.time = time;
			this.value = value;
			this.inTan = inTan;
			this.outTan = outTan;
		}

		public Entry(final Integer time, final Object value) {
			super();
			this.time = time;
			this.value = value;
		}

		public void set(final Entry other) {
			time = other.time;
			value = other.value;
			inTan = other.inTan;
			outTan = other.outTan;
		}
	}

	public Entry getEntry(final int index) {
		if (tans()) {
			return new Entry(times.get(index), values.get(index), inTans.get(index), outTans.get(index));
		} else {
			return new Entry(times.get(index), values.get(index));
		}
	}

	public static Object cloneValue(final Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Integer) {
			return value;
		} else if (value instanceof Double) {
			return value;
		} else if (value instanceof Vertex) {
			final Vertex vertex = (Vertex) value;
			final Vertex clonedVertex = new Vertex(vertex);
			return clonedVertex;
		} else if (value instanceof QuaternionRotation) {
			final QuaternionRotation vertex = (QuaternionRotation) value;
			final QuaternionRotation clonedVertex = new QuaternionRotation(vertex);
			return clonedVertex;
		} else {
			throw new IllegalStateException(value.getClass().getName());
		}
	}

	public static Object cloneValueAsEmpty(final Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Integer) {
			return 0;
		} else if (value instanceof Double) {
			return 0.0;
		} else if (value instanceof Vertex) {
			final Vertex vertex = (Vertex) value;
			final Vertex clonedVertex = new Vertex(0, 0, 0);
			return clonedVertex;
		} else if (value instanceof QuaternionRotation) {
			final QuaternionRotation vertex = (QuaternionRotation) value;
			final QuaternionRotation clonedVertex = new QuaternionRotation(0, 0, 0, 1);
			return clonedVertex;
		} else {
			throw new IllegalStateException(value.getClass().getName());
		}
	}

	public Object valueAt(final Integer time) {
		for (int i = 0; i < times.size(); i++) {
			if (times.get(i).equals(time)) {
				return values.get(i);
			}
		}
		return null;
	}

	public Object inTanAt(final Integer time) {
		for (int i = 0; i < times.size(); i++) {
			if (times.get(i).equals(time)) {
				return inTans.get(i);
			}
		}
		return null;
	}

	public Object outTanAt(final Integer time) {
		for (int i = 0; i < times.size(); i++) {
			if (times.get(i).equals(time)) {
				return outTans.get(i);
			}
		}
		return null;
	}

	public void setValuesTo(final AnimFlag af) {
		name = af.name;
		globalSeq = af.globalSeq;
		globalSeqId = af.globalSeqId;
		hasGlobalSeq = af.hasGlobalSeq;
		interpolationType = af.interpolationType;
		typeid = af.typeid;
		times = new ArrayList<>(af.times);
		values = deepCopy(af.values);
		inTans = deepCopy(af.inTans);
		outTans = deepCopy(af.outTans);
	}

	private <T> List<T> deepCopy(final List<T> source) {

		final List<T> copy = new ArrayList<>();
		for (final T item : source) {
			T toAdd = item;
			if (item instanceof Vertex) {
				final Vertex v = (Vertex) item;
				toAdd = (T) v;
			} else if (item instanceof QuaternionRotation) {
				final QuaternionRotation r = (QuaternionRotation) item;
				toAdd = (T) r;
			}
			copy.add(toAdd);
		}
		return copy;
	}

	public String getName() {
		return name;
	}

	public int getTypeId() {
		return typeid;
	}

	private AnimFlag() {

	}

	public void updateGlobalSeqRef(final EditableModel mdlr) {
		if (hasGlobalSeq) {
			globalSeq = mdlr.getGlobalSeq(globalSeqId);
		}
	}

	public void updateGlobalSeqId(final EditableModel mdlr) {
		if (hasGlobalSeq) {
			globalSeqId = mdlr.getGlobalSeqId(globalSeq);
		}
	}

	public String flagToString(final Object o) {
		if (o.getClass() == double.class) {
			return ((Double)o).toString();
		} else if (o.getClass() == Double.class) {
			return ((Double)o).toString();
		} else {
			return o.toString();
		}
	}

	public void flipOver(final byte axis) {
		if (typeid == 2) {
			// Rotation
			for (int k = 0; k < values.size(); k++) {
				final QuaternionRotation rot = (QuaternionRotation) values.get(k);
				final Vertex euler = rot.toEuler();
				switch (axis) {
				case 0:
					euler.setCoord((byte) 0, -euler.getCoord((byte) 0));
					euler.setCoord((byte) 1, -euler.getCoord((byte) 1));
					break;
				case 1:
					euler.setCoord((byte) 0, -euler.getCoord((byte) 0));
					euler.setCoord((byte) 2, -euler.getCoord((byte) 2));
					break;
				case 2:
					euler.setCoord((byte) 1, -euler.getCoord((byte) 1));
					euler.setCoord((byte) 2, -euler.getCoord((byte) 2));
					break;
				}
				values.set(k, new QuaternionRotation(euler));
			}
			for (int k = 0; k < inTans.size(); k++) {
				final QuaternionRotation rot = (QuaternionRotation) inTans.get(k);
				final Vertex euler = rot.toEuler();
				switch (axis) {
				case 0:
					euler.setCoord((byte) 0, -euler.getCoord((byte) 0));
					euler.setCoord((byte) 1, -euler.getCoord((byte) 1));
					break;
				case 1:
					euler.setCoord((byte) 0, -euler.getCoord((byte) 0));
					euler.setCoord((byte) 2, -euler.getCoord((byte) 2));
					break;
				case 2:
					euler.setCoord((byte) 1, -euler.getCoord((byte) 1));
					euler.setCoord((byte) 2, -euler.getCoord((byte) 2));
					break;
				}
				inTans.set(k, new QuaternionRotation(euler));
			}
			for (int k = 0; k < outTans.size(); k++) {
				final QuaternionRotation rot = (QuaternionRotation) outTans.get(k);
				final Vertex euler = rot.toEuler();
				switch (axis) {
				case 0:
					euler.setCoord((byte) 0, -euler.getCoord((byte) 0));
					euler.setCoord((byte) 1, -euler.getCoord((byte) 1));
					break;
				case 1:
					euler.setCoord((byte) 0, -euler.getCoord((byte) 0));
					euler.setCoord((byte) 2, -euler.getCoord((byte) 2));
					break;
				case 2:
					euler.setCoord((byte) 1, -euler.getCoord((byte) 1));
					euler.setCoord((byte) 2, -euler.getCoord((byte) 2));
					break;
				}
				outTans.set(k, new QuaternionRotation(euler));
			}
		} else if (typeid == 3) {
			// Translation
			for (int k = 0; k < values.size(); k++) {
				final Vertex trans = (Vertex) values.get(k);
				// trans.setCoord(axis,-trans.getCoord(axis));
				switch (axis) {
				// case 0:
				// trans.setCoord((byte)2,-trans.getCoord((byte)2));
				// break;
				// case 1:
				// trans.setCoord((byte)0,-trans.getCoord((byte)0));
				// break;
				// case 2:
				// trans.setCoord((byte)1,-trans.getCoord((byte)1));
				// break;
				case 0:
					trans.setCoord((byte) 0, -trans.getCoord((byte) 0));
					break;
				case 1:
					trans.setCoord((byte) 1, -trans.getCoord((byte) 1));
					break;
				case 2:
					trans.setCoord((byte) 2, -trans.getCoord((byte) 2));
					break;
				}
			}
			for (int k = 0; k < inTans.size(); k++) {
				final Vertex trans = (Vertex) inTans.get(k);
				// trans.setCoord(axis,-trans.getCoord(axis));
				switch (axis) {
				// case 0:
				// trans.setCoord((byte)2,-trans.getCoord((byte)2));
				// break;
				// case 1:
				// trans.setCoord((byte)0,-trans.getCoord((byte)0));
				// break;
				// case 2:
				// trans.setCoord((byte)1,-trans.getCoord((byte)1));
				// break;
				case 0:
					trans.setCoord((byte) 0, -trans.getCoord((byte) 0));
					break;
				case 1:
					trans.setCoord((byte) 1, -trans.getCoord((byte) 1));
					break;
				case 2:
					trans.setCoord((byte) 2, -trans.getCoord((byte) 2));
					break;
				}
			}
			for (int k = 0; k < outTans.size(); k++) {
				final Vertex trans = (Vertex) outTans.get(k);
				// trans.setCoord(axis,-trans.getCoord(axis));
				switch (axis) {
				// case 0:
				// trans.setCoord((byte)2,-trans.getCoord((byte)2));
				// break;
				// case 1:
				// trans.setCoord((byte)0,-trans.getCoord((byte)0));
				// break;
				// case 2:
				// trans.setCoord((byte)1,-trans.getCoord((byte)1));
				// break;
				case 0:
					trans.setCoord((byte) 0, -trans.getCoord((byte) 0));
					break;
				case 1:
					trans.setCoord((byte) 1, -trans.getCoord((byte) 1));
					break;
				case 2:
					trans.setCoord((byte) 2, -trans.getCoord((byte) 2));
					break;
				}
			}
		}
	}

	public AnimFlag getMostVisible(final AnimFlag partner) {
		if (partner != null) {
			if ((typeid == 0) && (partner.typeid == 0)) {
				final List<Integer> atimes = new ArrayList<>(times);
				final List<Integer> btimes = new ArrayList<>(partner.times);
				final List<Double> avalues = new ArrayList(values);
				final List<Double> bvalues = new ArrayList(partner.values);
				AnimFlag mostVisible = null;
				for (int i = atimes.size() - 1; i >= 0; i--)
				// count down from top, meaning that removing the current value
				// causes no harm
				{
					final Integer currentTime = atimes.get(i);
					final Double currentVal = avalues.get(i);

					if (btimes.contains(currentTime)) {
						final Double partVal = bvalues.get(btimes.indexOf(currentTime));
						if (partVal.doubleValue() > currentVal.doubleValue()) {
							if (mostVisible == null) {
								mostVisible = partner;
							} else if (mostVisible == this) {
								return null;
							}
						} else if (partVal.doubleValue() < currentVal.doubleValue()) {
							if (mostVisible == null) {
								mostVisible = this;
							} else if (mostVisible == partner) {
								return null;
							}
						} else {
							// System.out.println("Equal entries spell success");
						}
						// btimes.remove(currentTime);
						// bvalues.remove(partVal);
					} else if (currentVal.doubleValue() < 1) {
						if (mostVisible == null) {
							mostVisible = partner;
						} else if (mostVisible == this) {
							return null;
						}
					}
				}
				for (int i = btimes.size() - 1; i >= 0; i--)
				// count down from top, meaning that removing the current value
				// causes no harm
				{
					final Integer currentTime = btimes.get(i);
					final Double currentVal = bvalues.get(i);

					if (atimes.contains(currentTime)) {
						final Double partVal = avalues.get(atimes.indexOf(currentTime));
						if (partVal.doubleValue() > currentVal.doubleValue()) {
							if (mostVisible == null) {
								mostVisible = this;
							} else if (mostVisible == partner) {
								return null;
							}
						} else if (partVal.doubleValue() < currentVal.doubleValue()) {
							if (mostVisible == null) {
								mostVisible = partner;
							} else if (mostVisible == this) {
								return null;
							}
						}
					} else if (currentVal.doubleValue() < 1) {
						if (mostVisible == null) {
							mostVisible = this;
						} else if (mostVisible == partner) {
							return null;
						}
					}
				}
				if (mostVisible == null) {
					return partner;// partner has priority!
				} else {
					return mostVisible;
				}
			} else {
				JOptionPane.showMessageDialog(null,
						"Error: Program attempted to compare visibility with non-visibility animation component.\nThis... probably means something is horribly wrong. Save your work, if you can.");
			}
		}
		return null;
	}

	public boolean tans() {
		return interpolationType.tangential() && inTans.size() > 0;
	}

	public void linearize() {
		if (interpolationType.tangential()) {
			interpolationType = InterpolationType.LINEAR;
			inTans.clear();
			outTans.clear();
		}
	}

	public void copyFrom(final AnimFlag source) {
		times.addAll(source.times);
		values.addAll(source.values);
		final boolean stans = source.tans();
		final boolean mtans = tans();
		if (stans && mtans) {
			inTans.addAll(source.inTans);
			outTans.addAll(source.outTans);
		} else if (mtans) {
			JOptionPane.showMessageDialog(null,
					"Some animations will lose complexity due to transfer incombatibility. There will probably be no visible change.");
			inTans.clear();
			outTans.clear();
			interpolationType = InterpolationType.LINEAR;
			// Probably makes this flag linear, but certainly makes it more like
			// the copy source
		}
	}

	public void deleteAnim(final Animation anim) {
		if (!hasGlobalSeq) {
			final boolean tans = tans();
			for (int index = times.size() - 1; index >= 0; index--) {
				final Integer inte = times.get(index);
				final int i = inte.intValue();
				// int index = times.indexOf(inte);
				if ((i >= anim.getStart()) && (i <= anim.getEnd())) {
					// If this "i" is a part of the anim being removed

					times.remove(index);
					values.remove(index);
					if (tans) {
						inTans.remove(index);
						outTans.remove(index);
					}
				}
			}
		} else {
			System.out.println("KeyFrame deleting was blocked by a GlobalSequence");
		}

		// BOOM magic happens
	}

	public void deleteAt(final int index) {
		times.remove(index);
		values.remove(index);
		if (tans()) {
			inTans.remove(index);
			outTans.remove(index);
		}
	}

	/**
	 * Copies time track data from a certain interval into a different, new
	 * interval. The AnimFlag source of the data to copy cannot be same AnimFlag
	 * into which the data is copied, or else a ConcurrentModificationException will
	 * be thrown.
	 *
	 * @param source
	 * @param sourceStart
	 * @param sourceEnd
	 * @param newStart
	 * @param newEnd
	 */
	public void copyFrom(final AnimFlag source, final int sourceStart, final int sourceEnd, final int newStart,
			final int newEnd) {
		// Timescales a part of the AnimFlag from the source into the new time
		// "newStart" to "newEnd"
		boolean tans = source.tans();
		if (tans && interpolationType == InterpolationType.LINEAR) {
			final int x = JOptionPane.showConfirmDialog(null,
					"ERROR! A source was found to have Linear and Nonlinear motion simultaneously. Does the following have non-zero data? "
							+ source.inTans,
					"Help This Program!", JOptionPane.YES_NO_OPTION);
			if (x == JOptionPane.NO_OPTION) {
				tans = false;
			}
		}
		for (final Integer inte : source.times) {
			final int i = inte.intValue();
			final int index = source.times.indexOf(inte);
			if ((i >= sourceStart) && (i <= sourceEnd)) {
				// If this "i" is a part of the anim being rescaled
				final double ratio = (double) (i - sourceStart) / (double) (sourceEnd - sourceStart);
				times.add(Integer.valueOf((int) (newStart + (ratio * (newEnd - newStart)))));
				values.add(cloneValue(source.values.get(index)));
				if (tans) {
					inTans.add(cloneValue(source.inTans.get(index)));
					outTans.add(cloneValue(source.outTans.get(index)));
				}
			}
		}

		sort();

		// BOOM magic happens
	}

	public void timeScale(final int start, final int end, final int newStart, final int newEnd) {
		// Timescales a part of the AnimFlag from section "start" to "end" into
		// the new time "newStart" to "newEnd"
		// if( newEnd > newStart )
		// {
		for (int z = 0; z < times.size(); z++)// Integer inte: times )
		{
			final Integer inte = times.get(z);
			final int i = inte.intValue();
			if ((i >= start) && (i <= end)) {
				// If this "i" is a part of the anim being rescaled
				final double ratio = (double) (i - start) / (double) (end - start);
				times.set(z, Integer.valueOf((int) (newStart + (ratio * (newEnd - newStart)))));
			}
		}
		// }
		// else
		// {
		// for( Integer inte: times )
		// {
		// int i = inte.intValue();
		// if( i >= end && i <= start )
		// {
		// //If this "i" is a part of the anim being rescaled
		// double ratio = (double)(i-start)/(double)(end-start);
		// times.set(times.indexOf(inte),Integer.valueOf((int)(newStart + ( ratio *
		// ( newStart - newEnd ) ) ) ) );
		// }
		// }
		// }

		sort();

		// BOOM magic happens
	}

	public void sort() {
		final int low = 0;
		final int high = times.size() - 1;
		if (size() > 1) {
			quicksort(low, high);
		}
	}

	private void quicksort(final int low, final int high) {
		// Thanks to Lars Vogel for the quicksort concept code (something to
		// look at), found on google
		// (re-written by Eric "Retera" for use in AnimFlags)
		int i = low, j = high;
		final Integer pivot = times.get(low + ((high - low) / 2));

		while (i <= j) {
			while (times.get(i).intValue() < pivot.intValue()) {
				i++;
			}
			while (times.get(j).intValue() > pivot.intValue()) {
				j--;
			}
			if (i <= j) {
				exchange(i, j);
				i++;
				j--;
			}
		}

		if (low < j) {
			quicksort(low, j);
		}
		if (i < high) {
			quicksort(i, high);
		}
	}

	private void exchange(final int i, final int j) {
		final Integer iTime = times.get(i);
		final Object iValue = values.get(i);

		times.set(i, times.get(j));
		try {
			values.set(i, values.get(j));
		} catch (final Exception e) {
			e.printStackTrace();
			// System.out.println(getName()+":
			// "+times.size()+","+values.size());
			// System.out.println(times.get(0)+": "+values.get(0));
			// System.out.println(times.get(1));
		}

		times.set(j, iTime);
		values.set(j, iValue);

		if (inTans.size() > 0)// if we have to mess with Tans
		{
			final Object iInTan = inTans.get(i);
			final Object iOutTan = outTans.get(i);

			inTans.set(i, inTans.get(j));
			outTans.set(i, outTans.get(j));

			inTans.set(j, iInTan);
			outTans.set(j, iOutTan);
		}
	}

	public List<Integer> getTimes() {
		return times;
	}

	public List<Object> getValues() {
		return values;
	}

	public List<Object> getInTans() {
		return inTans;
	}

	public List<Object> getOutTans() {
		return outTans;
	}

	public int ceilIndex(final int time) {
		if (times.size() == 0) {
			return 0;
		}
		final int ceilIndex = ceilIndex(time, 0, times.size() - 1);
		if (ceilIndex == -1) {
			return times.size() - 1;
		}
		return ceilIndex;
	}

	/*
	 * Rather than spending time visualizing corner cases for these, I borrowed
	 * logic from: https://www.geeksforgeeks.org/ceiling-in-a-sorted-array/
	 */
	private int ceilIndex(final int time, final int lo, final int hi) {
		if (time <= times.get(lo)) {
			return lo;
		}
		if (time > times.get(hi)) {
			return -1;
		}
		final int mid = (lo + hi) / 2;
		final Integer midTime = times.get(mid);
		if (midTime == time) {
			return mid;
		} else if (midTime < time) {
			if (((mid + 1) <= hi) && (time <= times.get(mid + 1))) {
				return mid + 1;
			} else {
				return ceilIndex(time, mid + 1, hi);
			}
		} else {
			if (((mid - 1) >= lo) && (time > times.get(mid - 1))) {
				return mid;
			} else {
				return ceilIndex(time, lo, mid - 1);
			}
		}
	}

	public int floorIndex(final int time) {
		if (times.size() == 0) {
			return 0;
		}
		final int floorIndex = floorIndex(time, 0, times.size() - 1);
		if (floorIndex == -1) {
			return 0;
		}
		return floorIndex;
	}

	/*
	 * Rather than spending time visualizing corner cases for these, I borrowed
	 * logic from: https://www.geeksforgeeks.org/floor-in-a-sorted-array/
	 */
	private int floorIndex(final int time, final int lo, final int hi) {
		if (lo > hi) {
			return -1;
		}
		if (time >= times.get(hi)) {
			return hi;
		}
		final int mid = (lo + hi) / 2;
		final Integer midTime = times.get(mid);
		if (times.get(mid) == time) {
			return mid;
		}
		if ((mid > 0) && (times.get(mid - 1) <= time) && (time < midTime)) {
			return mid - 1;
		}
		if (time > midTime) {
			return floorIndex(time, mid + 1, hi);
		} else {
			return floorIndex(time, lo, mid - 1);
		}
	}

	public static final QuaternionRotation ROTATE_IDENTITY = new QuaternionRotation(0, 0, 0, 1);
	public static final Vertex SCALE_IDENTITY = new Vertex(1, 1, 1);
	public static final Vertex TRANSLATE_IDENTITY = new Vertex(0, 0, 0);

	private Object identity(final int typeid) {
		switch (typeid) {
		case ALPHA | OTHER_TYPE: {
			return 1.;
		}
		case TRANSLATION:
			return TRANSLATE_IDENTITY;
		case SCALING:
		case COLOR: {
			return SCALE_IDENTITY;
		}
		case ROTATION: {
			return ROTATE_IDENTITY;
		}
		case TEXTUREID:
		// Integer
		{
			System.err.println("Texture identity used in renderer... TODO make this function more intelligent.");
			return 0;
		}
		default:
			throw new IllegalStateException();
		}
	}

	/**
	 * Interpolates at a given time. The lack of generics on this function is
	 * abysmal, but currently this is how the codebase is.
	 *
	 * @param time
	 * @param animation
	 * @return
	 */
	public Object interpolateAt(final AnimatedRenderEnvironment animatedRenderEnvironment) {
		if ((animatedRenderEnvironment == null) || (animatedRenderEnvironment.getCurrentAnimation() == null)) {
			if (values.size() > 0) {
				return values.get(0);
			}
			return identity(typeid);
		}
		int localTypeId = typeid;
		if ((localTypeId == ROTATION) && (size() > 0) && (values.get(0) instanceof Double)) {
			localTypeId = ALPHA; // magic Camera rotation!
		}
		if (times.isEmpty()) {
			return identity(localTypeId);
		}
		// TODO ghostwolf says to stop using binary search, because linear walking is
		// faster for the small MDL case
		int time;
		int ceilIndex;
		int floorIndex;
		Object floorInTan;
		Object floorOutTan;
		Object floorValue;
		Object ceilValue;
		Integer floorIndexTime;
		Integer ceilIndexTime;
		if (hasGlobalSeq() && (getGlobalSeq() >= 0)) {
			time = animatedRenderEnvironment.getGlobalSeqTime(getGlobalSeq());
			final int floorAnimStartIndex = floorIndex(1);
			final int floorAnimEndIndex = floorIndex(getGlobalSeq());
			floorIndex = floorIndex(time);
			ceilIndex = ceilIndex(time);
			if (ceilIndex < floorIndex) {
				// retarded repeated keyframes issue, see Peasant's Bone_Chest
				// at time 18300
				ceilIndex = floorIndex;
			}
			floorValue = values.get(floorIndex);
			floorInTan = tans() ? inTans.get(floorIndex) : null;
			floorOutTan = tans() ? outTans.get(floorIndex) : null;
			ceilValue = values.get(ceilIndex);
			floorIndexTime = times.get(floorIndex);
			ceilIndexTime = times.get(ceilIndex);
			if (ceilIndexTime < 0) {
				return identity(localTypeId);
			}
			if (floorIndexTime > getGlobalSeq()) {
				if (values.size() > 0) {
					// out of range global sequences end up just using the higher value keyframe
					return values.get(floorIndex);
				}
				return identity(localTypeId);
			}
			if ((floorIndexTime < 0) && (ceilIndexTime > getGlobalSeq())) {
				return identity(localTypeId);
			} else if (floorIndexTime < 0) {
				floorValue = identity(localTypeId);
				floorInTan = floorOutTan = identity(localTypeId);
			} else if (ceilIndexTime > getGlobalSeq()) {
				ceilValue = values.get(floorAnimStartIndex);
				ceilIndex = floorAnimStartIndex;
			}
			if (floorIndex == ceilIndex) {
				return floorValue;
			}
		} else {
			final BasicTimeBoundProvider animation = animatedRenderEnvironment.getCurrentAnimation();
			time = animation.getStart() + animatedRenderEnvironment.getAnimationTime();
			final int floorAnimStartIndex = floorIndex(animation.getStart() + 1);
			final int floorAnimEndIndex = floorIndex(animation.getEnd());
			floorIndex = floorIndex(time);
			ceilIndex = ceilIndex(time);
			if (ceilIndex < floorIndex) {
				// retarded repeated keyframes issue, see Peasant's Bone_Chest
				// at time 18300
				ceilIndex = floorIndex;
			}
			floorValue = values.get(floorIndex);
			floorInTan = tans() ? inTans.get(floorIndex) : null;
			floorOutTan = tans() ? outTans.get(floorIndex) : null;
			ceilValue = values.get(ceilIndex);
			floorIndexTime = times.get(floorIndex);
			ceilIndexTime = times.get(ceilIndex);
			final boolean blockAllowingNoInterp = false;
			if (ceilIndexTime < animation.getStart()) {
				return identity(localTypeId);
			}
			if (floorIndexTime > animation.getEnd()) {
				return identity(localTypeId);
			}
			if ((floorIndexTime < animation.getStart()) && (ceilIndexTime > animation.getEnd())) {
				return identity(localTypeId);
			} else if (floorIndexTime < animation.getStart()) {
				if (times.get(floorAnimEndIndex) == animation.getEnd()) {
					floorIndex = floorAnimEndIndex;
					floorValue = values.get(floorAnimEndIndex);
					floorIndexTime = animation.getStart();
					if (tans()) {
						floorInTan = inTans.get(floorAnimEndIndex);
						floorOutTan = inTans.get(floorAnimEndIndex);
//						floorIndexTime = times.get(floorAnimEndIndex);
					}
				} else {
					floorValue = identity(localTypeId);
					floorInTan = floorOutTan = identity(localTypeId);
					floorIndexTime = animation.getStart();
				}
			} else if ((ceilIndexTime > animation.getEnd())
					|| ((ceilIndexTime < time) && (times.get(floorAnimEndIndex) < time))) {
				if (times.get(floorAnimStartIndex) == animation.getStart()) {
					ceilValue = values.get(floorAnimStartIndex);
					ceilIndex = floorAnimStartIndex;
					ceilIndexTime = animation.getEnd();
				} else {
					ceilIndex = ceilIndex(animation.getStart());
					ceilValue = values.get(ceilIndex);
					ceilIndexTime = times.get(ceilIndex);
				}
				// NOTE: we just let it be in this case, based on
				// Water Elemental's birth
			}
			if (floorIndex == ceilIndex) {
				return floorValue;
			}
		}
		switch (localTypeId) {
		case ALPHA | OTHER_TYPE: {
			// Double
			final Double previous = (Double) floorValue;
			final Double next = (Double) ceilValue;
			switch (interpolationType) {
			case BEZIER: {
				final Double previousOutTan = (Double) floorOutTan;
				final Double nextInTan = (Double) inTans.get(ceilIndex);
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final double bezier = MathUtils.bezier(previous, previousOutTan, nextInTan, next,
						(float) (time - floorTime) / (float) (ceilTime - floorTime));
				return bezier;
			}
			case DONT_INTERP:
				return floorValue;
			case HERMITE: {
				final Double previousOutTan = (Double) floorOutTan;
				final Double nextInTan = (Double) inTans.get(ceilIndex);
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final double hermite = MathUtils.hermite(previous, previousOutTan, nextInTan, next,
						(float) (time - floorTime) / (float) (ceilTime - floorTime));
				return hermite;
			}
			case LINEAR:
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final double lerp = MathUtils.lerp(previous, next,
						(float) (time - floorTime) / (float) (ceilTime - floorTime));
				return lerp;
			default:
				throw new IllegalStateException();
			}
		}
		case TRANSLATION:
		case SCALING:
		case COLOR: {
			// Vertex
			final Vertex previous = (Vertex) floorValue;
			final Vertex next = (Vertex) ceilValue;
			switch (interpolationType) {
			case BEZIER: {
				final Vertex previousOutTan = (Vertex) floorOutTan;
				final Vertex nextInTan = (Vertex) inTans.get(ceilIndex);
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final float timeFactor = (float) (time - floorTime) / (float) (ceilTime - floorTime);
				final Vertex bezier = new Vertex(
						MathUtils.bezier(previous.x, previousOutTan.x, nextInTan.x, next.x, timeFactor),
						MathUtils.bezier(previous.y, previousOutTan.y, nextInTan.y, next.y, timeFactor),
						MathUtils.bezier(previous.z, previousOutTan.z, nextInTan.z, next.z, timeFactor));
				return bezier;
			}
			case DONT_INTERP:
				return floorValue;
			case HERMITE: {
				final Vertex previousOutTan = (Vertex) floorOutTan;
				final Vertex nextInTan = (Vertex) inTans.get(ceilIndex);
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final float timeFactor = (float) (time - floorTime) / (float) (ceilTime - floorTime);
				final Vertex hermite = new Vertex(
						MathUtils.hermite(previous.x, previousOutTan.x, nextInTan.x, next.x, timeFactor),
						MathUtils.hermite(previous.y, previousOutTan.y, nextInTan.y, next.y, timeFactor),
						MathUtils.hermite(previous.z, previousOutTan.z, nextInTan.z, next.z, timeFactor));
				return hermite;
			}
			case LINEAR:
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final float timeFactor = (float) (time - floorTime) / (float) (ceilTime - floorTime);
				final Vertex lerp = new Vertex(MathUtils.lerp(previous.x, next.x, timeFactor),
						MathUtils.lerp(previous.y, next.y, timeFactor), MathUtils.lerp(previous.z, next.z, timeFactor));
				return lerp;
			default:
				throw new IllegalStateException();
			}
		}
		case ROTATION: {
			// Quat
			final QuaternionRotation previous = (QuaternionRotation) floorValue;
			final QuaternionRotation next = (QuaternionRotation) ceilValue;
			switch (interpolationType) {
			case BEZIER: {
				final QuaternionRotation previousOutTan = (QuaternionRotation) floorOutTan;
				final QuaternionRotation nextInTan = (QuaternionRotation) inTans.get(ceilIndex);
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final float timeFactor = (float) (time - floorTime) / (float) (ceilTime - floorTime);
				final QuaternionRotation result = new QuaternionRotation(0, 0, 0, 0);
				return QuaternionRotation.ghostwolfSquad(result, previous, previousOutTan, nextInTan, next, timeFactor);
			}
			case DONT_INTERP:
				return floorValue;
			case HERMITE: {
				final QuaternionRotation previousOutTan = (QuaternionRotation) floorOutTan;
				final QuaternionRotation nextInTan = (QuaternionRotation) inTans.get(ceilIndex);
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final float timeFactor = (float) (time - floorTime) / (float) (ceilTime - floorTime);
				final QuaternionRotation result = new QuaternionRotation(0, 0, 0, 0);
				return QuaternionRotation.ghostwolfSquad(result, previous, previousOutTan, nextInTan, next, timeFactor);
			}
			case LINEAR:
				final Integer floorTime = floorIndexTime;
				final Integer ceilTime = ceilIndexTime;
				final float timeFactor = (float) (time - floorTime) / (float) (ceilTime - floorTime);
				final QuaternionRotation result = new QuaternionRotation(0, 0, 0, 0);
				return QuaternionRotation.slerp(result, previous, next, timeFactor);
			default:
				throw new IllegalStateException();
			}
		}
		case TEXTUREID:
		// Integer
		{
			final Integer previous = (Integer) floorValue;
			switch (interpolationType) {
			case DONT_INTERP:
			case BEZIER: // dont use bezier on these, does that even make any sense?
			case HERMITE: // dont use hermite on these, does that even make any sense?
			case LINEAR: // dont use linear on these, does that even make any sense?
				return previous;
			default:
				throw new IllegalStateException();
			}
		}
		}
		throw new IllegalStateException();
	}

	public void removeKeyframe(final int trackTime) {
		final int keyframeIndex = floorIndex(trackTime);
		if ((keyframeIndex >= size()) || (times.get(keyframeIndex) != trackTime)) {
			throw new IllegalStateException("Attempted to remove keyframe, but no keyframe was found (" + keyframeIndex
					+ " @ time " + trackTime + ")");
		} else {
			times.remove(keyframeIndex);
			values.remove(keyframeIndex);
			if (tans()) {
				inTans.remove(keyframeIndex);
				outTans.remove(keyframeIndex);
			}
		}
	}

	public void addKeyframe(final int trackTime, final Object value) {
		int keyframeIndex = ceilIndex(trackTime);
		if (keyframeIndex == (times.size() - 1)) {
			if (times.isEmpty()) {
				keyframeIndex = 0;
			} else if (trackTime > times.get(times.size() - 1)) {
				keyframeIndex = times.size();
			}
		}
		times.add(keyframeIndex, trackTime);
		values.add(keyframeIndex, value);
	}

	public void addKeyframe(final int trackTime, final Object value, final Object inTan, final Object outTan) {
		int keyframeIndex = ceilIndex(trackTime);
		if (keyframeIndex == (times.size() - 1)) {
			if (times.isEmpty()) {
				keyframeIndex = 0;
			} else if (trackTime > times.get(times.size() - 1)) {
				keyframeIndex = times.size();
			}
		}
		times.add(keyframeIndex, trackTime);
		values.add(keyframeIndex, value);
		inTans.add(keyframeIndex, inTan);
		outTans.add(keyframeIndex, outTan);
	}

	public void setKeyframe(final Integer time, final Object value) {
		if (tans()) {
			throw new IllegalStateException();
		}
		// TODO maybe binary search, ghostwolf says it's not worth it
		for (int index = 0; index < times.size(); index++) {
			if (times.get(index).equals(time)) {
				values.set(index, value);
			}
		}
	}

	public void setKeyframe(final Integer time, final Object value, final Object inTan, final Object outTan) {
		if (!tans()) {
			throw new IllegalStateException();
		}
		for (int index = 0; index < times.size(); index++) {
			if (times.get(index).equals(time)) {
				values.set(index, value);
				inTans.set(index, inTan);
				outTans.set(index, outTan);
			}
		}
	}

	public void slideKeyframe(final int startTrackTime, final int endTrackTime) {
		if (times.size() < 1) {
			throw new IllegalStateException("Unable to slide keyframe: no frames exist");
		}
		final int startIndex = floorIndex(startTrackTime);
		final int endIndex = floorIndex(endTrackTime);
		if (times.get(endIndex) == endTrackTime) {
			throw new IllegalStateException("Sliding this keyframe would create duplicate entries at one time!");
		}
		times.set(startIndex, endTrackTime);
		sort();
	}

	public void setName(final String title) {
		this.name = title;
	}
}
