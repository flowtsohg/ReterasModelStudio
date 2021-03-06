package com.hiveworkshop.wc3.mdl;

import java.util.HashSet;

import com.etheller.warsmash.parsers.mdlx.MdlxLight;
import com.etheller.warsmash.parsers.mdlx.MdlxLight.Type;
import com.hiveworkshop.wc3.gui.modeledit.CoordinateSystem;
import com.hiveworkshop.wc3.mdl.v2.visitor.IdObjectVisitor;

/**
 * Write a description of class Light here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class Light extends IdObject {
	Type type = Type.OMNIDIRECTIONAL;
	int attenuationStart = 0;
	int attenuationEnd = 0;
	double intensity = 0;
	Vertex staticColor = new Vertex();
	double ambIntensity = 0;
	Vertex staticAmbColor = new Vertex();

	public Light() {

	}

	public Light(final String name) {
		this.name = name;
	}

	public Light(final Light light) {
		copyObject(light);

		type = light.type;
		attenuationStart = light.attenuationStart;
		attenuationEnd = light.attenuationEnd;
		intensity = light.intensity;
		staticColor = light.staticColor;
		ambIntensity = light.ambIntensity;
		staticAmbColor = light.staticAmbColor;
	}

	public Light(final MdlxLight light) {
		if ((light.flags & 512) != 512) {
			System.err.println("MDX -> MDL error: A light '" + light.name + "' not flagged as light in MDX!");
		}

		loadObject(light);

		type = light.type;
		setAttenuationStart((int)light.attenuation[0]);
		setAttenuationEnd((int)light.attenuation[1]);
		setStaticColor(new Vertex(light.color, true));
		setIntensity(light.intensity);
		setStaticAmbColor(new Vertex(light.ambientColor, true));
		setAmbIntensity(light.ambientIntensity);
	}

	public MdlxLight toMdlx() {
		MdlxLight light = new MdlxLight();

		objectToMdlx(light);

		light.type = type;
		light.attenuation[0] = getAttenuationStart();
		light.attenuation[1] = getAttenuationEnd();
		light.color = MdlxUtils.flipRGBtoBGR(getStaticColor().toFloatArray());
		light.intensity = (float)getIntensity();
		light.ambientColor = MdlxUtils.flipRGBtoBGR(getStaticAmbColor().toFloatArray());
		light.ambientIntensity = (float)getAmbIntensity();
		
		return light;
	}

	@Override
	public Light copy() {
		return new Light(this);
	}

	public String getVisTagname() {
		return "light";// geoset.getName();
	}

	public int getAttenuationStart() {
		return attenuationStart;
	}

	public void setAttenuationStart(final int attenuationStart) {
		this.attenuationStart = attenuationStart;
	}

	public int getAttenuationEnd() {
		return attenuationEnd;
	}

	public void setAttenuationEnd(final int attenuationEnd) {
		this.attenuationEnd = attenuationEnd;
	}

	public double getIntensity() {
		return intensity;
	}

	public void setIntensity(final double intensity) {
		this.intensity = intensity;
	}

	public Vertex getStaticColor() {
		return staticColor;
	}

	public void setStaticColor(final Vertex staticColor) {
		this.staticColor = staticColor;
	}

	public double getAmbIntensity() {
		return ambIntensity;
	}

	public void setAmbIntensity(final double ambIntensity) {
		this.ambIntensity = ambIntensity;
	}

	public Vertex getStaticAmbColor() {
		return staticAmbColor;
	}

	public void setStaticAmbColor(final Vertex staticAmbColor) {
		this.staticAmbColor = staticAmbColor;
	}

	@Override
	public void apply(final IdObjectVisitor visitor) {
		visitor.light(this);
	}

	@Override
	public double getClickRadius(final CoordinateSystem coordinateSystem) {
		return DEFAULT_CLICK_RADIUS / CoordinateSystem.Util.getZoom(coordinateSystem);
	}
}
