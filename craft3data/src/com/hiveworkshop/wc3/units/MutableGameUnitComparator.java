package com.hiveworkshop.wc3.units;

import java.util.Comparator;

import com.hiveworkshop.wc3.units.objectdata.MutableObjectData.MutableGameObject;
import com.hiveworkshop.wc3.units.objectdata.War3ID;

public class MutableGameUnitComparator implements Comparator<MutableGameObject> {
	private static final War3ID UNIT_LEVEL = War3ID.fromString("ulev");

	@Override
	public int compare(final MutableGameObject a, final MutableGameObject b) {
		if (a.readSLKTag("unitClass").equals("") && !b.readSLKTag("unitClass").equals("")) {
			return 1;
		} else if (b.readSLKTag("unitClass").equals("") && !a.readSLKTag("unitClass").equals("")) {
			return -1;
		}
		final int comp1 = a.readSLKTag("unitClass").compareTo(b.readSLKTag("unitClass"));
		if (comp1 == 0) {
			final int comp2 = Integer.valueOf(a.getFieldAsInteger(UNIT_LEVEL, 0))
					.compareTo(Integer.valueOf(b.getFieldAsInteger(UNIT_LEVEL, 0)));
			if (comp2 == 0) {
				return a.getName().compareTo(b.getName());
			}
			return comp2;
		}
		return comp1;
	}
}
