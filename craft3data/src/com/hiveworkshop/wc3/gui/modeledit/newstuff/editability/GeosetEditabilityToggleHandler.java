package com.hiveworkshop.wc3.gui.modeledit.newstuff.editability;

import java.util.List;

import com.hiveworkshop.wc3.gui.modeledit.newstuff.listener.EditabilityToggleHandler;
import com.hiveworkshop.wc3.mdl.Geoset;
import com.hiveworkshop.wc3.mdl.v2.ModelViewManager;

public class GeosetEditabilityToggleHandler implements EditabilityToggleHandler {
	private final List<Geoset> geosets;
	private final ModelViewManager modelViewManager;

	public GeosetEditabilityToggleHandler(final List<Geoset> geosets, final ModelViewManager modelViewManager) {
		this.geosets = geosets;
		this.modelViewManager = modelViewManager;
	}

	@Override
	public void makeEditable() {
		for (final Geoset geoset : geosets) {
			modelViewManager.makeGeosetEditable(geoset);
		}
	}

	@Override
	public void makeNotEditable() {
		for (final Geoset geoset : geosets) {
			modelViewManager.makeGeosetNotEditable(geoset);
		}
	}

}
