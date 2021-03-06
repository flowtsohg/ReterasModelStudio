package com.hiveworkshop.wc3.mdl;

import java.util.List;

import com.etheller.warsmash.parsers.mdlx.MdlxSequence;

import com.hiveworkshop.wc3.gui.animedit.BasicTimeBoundProvider;

/**
 * A java object to represent MDL "Sequences" ("Animations").
 *
 * Eric Theller 11/5/2011
 */
public class Animation implements BasicTimeBoundProvider {
	private String name = "";
	private int intervalStart = 0;
	private int intervalEnd = -1;
	private ExtLog extents;
	float moveSpeed = 0;
	boolean nonLooping = false;
	float rarity = 0;

	public Animation(final String name, final int intervalStart, final int intervalEnd) {
		this.name = name;
		this.intervalStart = intervalStart;
		this.intervalEnd = intervalEnd;
		extents = new ExtLog(ExtLog.DEFAULT_MINEXT, ExtLog.DEFAULT_MAXEXT, ExtLog.DEFAULT_BOUNDSRADIUS);
	}

	public Animation(final String name, final int intervalStart, final int intervalEnd, final Vertex minimumExt,
			final Vertex maximumExt, final double boundsRad) {
		this.name = name;
		this.intervalStart = intervalStart;
		this.intervalEnd = intervalEnd;
		extents = new ExtLog(minimumExt, maximumExt, boundsRad);
	}

	// construct for simple animation object, within geoset
	public Animation(final ExtLog extents) {
		name = "";
		this.extents = extents;
	}

	public Animation(final Animation other) {
		this.name = other.name;
		intervalStart = other.intervalStart;
		intervalEnd = other.intervalEnd;
		moveSpeed = other.moveSpeed;
		nonLooping = other.nonLooping;
		rarity = other.rarity;
		extents = new ExtLog(other.extents);
	}

	public Animation(final MdlxSequence sequence) {
		long[] interval = sequence.interval;

		name = sequence.name;
		intervalStart = (int)interval[0];
		intervalEnd = (int)interval[1];
		extents = new ExtLog(sequence.extent);
		moveSpeed = sequence.moveSpeed;

		if (sequence.flags == 1) {
			nonLooping = true;
		}

		rarity = sequence.rarity;
	}

	public MdlxSequence toMdlx() {
		MdlxSequence sequence = new MdlxSequence();

		sequence.name = name;
		sequence.interval[0] = intervalStart;
		sequence.interval[1] = intervalEnd;
		sequence.extent = extents.toMdlx();
		sequence.moveSpeed = moveSpeed;

		if (nonLooping) {
			sequence.flags = 1;
		}

		sequence.rarity = rarity;

		return sequence;
	}

	public boolean equals(final Animation other) {
		return other.name.equals(name) && (other.intervalStart == intervalStart)
				&& (other.intervalEnd == intervalEnd) && (other.moveSpeed == moveSpeed)
				&& (other.nonLooping == nonLooping) && (other.rarity == rarity);
	}

	public float getRarity() {
		return rarity;
	}

	public void setRarity(final float rarity) {
		this.rarity = rarity;
	}

	public void setMoveSpeed(final float moveSpeed) {
		this.moveSpeed = moveSpeed;
	}

	public float getMoveSpeed() {
		return moveSpeed;
	}

	public ExtLog getExtents() {
		return extents;
	}

	public void setExtents(final ExtLog extents) {
		this.extents = extents;
	}

	public void setName(final String text) {
		this.name = text;
	}

	public String getName() {
		return this.name;
	}

	public boolean isNonLooping() {
		return nonLooping;
	}

	public void setNonLooping(final boolean nonLooping) {
		this.nonLooping = nonLooping;
	}

	public int length() {
		return intervalEnd - intervalStart;
	}

	public void setInterval(final int start, final int end) {
		intervalStart = start;
		intervalEnd = end;
	}

	public void setIntervalStart(final int intervalStart) {
		this.intervalStart = intervalStart;
	}

	public void setIntervalEnd(final int intervalEnd) {
		this.intervalEnd = intervalEnd;
	}

	public void copyToInterval(final int start, final int end, final List<AnimFlag> flags,
			final List<EventObject> eventObjs, final List<AnimFlag> newFlags, final List<EventObject> newEventObjs) {
		for (final AnimFlag af : newFlags) {
			if (!af.hasGlobalSeq) {
				af.copyFrom(flags.get(newFlags.indexOf(af)), intervalStart, intervalEnd, start, end);
			}
		}
		for (final EventObject e : newEventObjs) {
			if (!e.hasGlobalSeq) {
				e.copyFrom(eventObjs.get(newEventObjs.indexOf(e)), intervalStart, intervalEnd, start, end);
			}
		}
	}

	public void copyToInterval(final int start, final int end, final List<AnimFlag> flags,
			final List<EventObject> eventObjs) {
		for (final AnimFlag af : flags) {
			if (!af.hasGlobalSeq) {
				af.copyFrom(new AnimFlag(af), intervalStart, intervalEnd, start, end);
			}
		}
		for (final EventObject e : eventObjs) {
			if (!e.hasGlobalSeq) {
				e.copyFrom(e.copy(), intervalStart, intervalEnd, start, end);
			}
		}
	}

	public void setInterval(final int start, final int end, final List<AnimFlag> flags,
			final List<EventObject> eventObjs) {
		for (final AnimFlag af : flags) {
			if (!af.hasGlobalSeq) {
				af.timeScale(intervalStart, intervalEnd, start, end);
			}
		}
		for (final EventObject e : eventObjs) {
			if (!e.hasGlobalSeq) {
				e.timeScale(intervalStart, intervalEnd, start, end);
			}
		}
		intervalStart = start;
		intervalEnd = end;
	}

	public void reverse(final List<AnimFlag> flags, final List<EventObject> eventObjs) {
		for (final AnimFlag af : flags) {
			if (!af.hasGlobalSeq && ((af.getTypeId() == 1) || (af.getTypeId() == 2) || (af.getTypeId() == 3))) {
				af.timeScale(intervalStart, intervalEnd, intervalEnd, intervalStart);
			}
		}
		for (final EventObject e : eventObjs) {
			e.timeScale(intervalStart, intervalEnd, intervalEnd, intervalStart);
		}
		// for( AnimFlag af: flags )
		// {
		// if( !af.hasGlobalSeq && (af.getTypeId() == 1 || af.getTypeId() == 2
		// || af.getTypeId() == 3 ) ) // wouldn't want to mess THAT up...
		// af.timeScale(m_intervalStart, m_intervalEnd, m_intervalStart+30,
		// m_intervalStart+2);
		// }
		// for( EventObject e: eventObjs )
		// {
		// e.timeScale(m_intervalStart, m_intervalEnd, m_intervalStart+30,
		// m_intervalStart+2);
		// }
	}

	public void clearData(final List<AnimFlag> flags, final List<EventObject> eventObjs) {
		for (final AnimFlag af : flags) {
			if (((af.getTypeId() == 1) || (af.getTypeId() == 2) || (af.getTypeId() == 3))) {
				// !af.hasGlobalSeq && was above before
				af.deleteAnim(this);// timeScale(m_intervalStart, m_intervalEnd,
									// m_intervalEnd, m_intervalStart);
			}
		}
		for (final EventObject e : eventObjs) {
			e.deleteAnim(this);
		}
	}

	public void setInterval(final int start, final int end, final EditableModel mdlr) {
		final List<AnimFlag> aniFlags = mdlr.getAllAnimFlags();
		final List<EventObject> eventObjs = mdlr.sortedIdObjects(EventObject.class);
		setInterval(start, end, aniFlags, eventObjs);
	}

	@Override
	public int getStart() {
		return intervalStart;
	}

	@Override
	public int getEnd() {
		return intervalEnd;
	}

	@Override
	public String toString() {
		return getName();
	}
}
