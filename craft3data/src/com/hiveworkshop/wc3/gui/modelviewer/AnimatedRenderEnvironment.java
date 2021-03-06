package com.hiveworkshop.wc3.gui.modelviewer;

import com.hiveworkshop.wc3.gui.animedit.BasicTimeBoundProvider;

public interface AnimatedRenderEnvironment {
	/* This is a rather nonsense constant for emitters regarding how many frames through time the emitter
	should move when it's .update() is called. For all other model components, current state is not a function
	of previous state, and this is ignored.
	When skipping through time with the time slider, this will hopefully cause particles to drift outward slowly
	despite the time skips.
	 */
	int FRAMES_PER_UPDATE = 1000/60;

	int getAnimationTime();

	BasicTimeBoundProvider getCurrentAnimation(); // nullable

	int getGlobalSeqTime(int length); // for glob seq
}
