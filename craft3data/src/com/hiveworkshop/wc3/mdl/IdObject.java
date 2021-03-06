package com.hiveworkshop.wc3.mdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.etheller.warsmash.parsers.mdlx.MdlxGenericObject;
import com.hiveworkshop.wc3.gui.modeledit.CoordinateSystem;
import com.hiveworkshop.wc3.mdl.v2.visitor.IdObjectVisitor;

/**
 * Write a description of class ObjectId here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public abstract class IdObject extends AnimatedNode implements Named {
	public static final int DEFAULT_CLICK_RADIUS = 8;

	protected String name = "";
	protected int objectId = -1;
	protected int parentId = -1;
	protected boolean dontInheritTranslation = false;
	protected boolean dontInheritRotation = false;
	protected boolean dontInheritScaling = false;
	protected boolean billboarded = false;
	protected boolean billboardLockX = false;
	protected boolean billboardLockY = false;
	protected boolean billboardLockZ = false;
	protected Vertex pivotPoint;
	protected IdObject parent;
	protected final List<IdObject> childrenNodes = new ArrayList<>();
	protected float[] bindPose;

	public IdObject() {

	}

	public IdObject(final IdObject host) {
		copyObject(host);
	}

	public void loadObject(final MdlxGenericObject object) {
		name = object.name;
		objectId = object.objectId;
		parentId = object.parentId;

		if ((object.flags & 0x1) != 0) {
			dontInheritTranslation = true;
		}

		if ((object.flags & 0x2) != 0) {
			dontInheritRotation = true;
		}

		if ((object.flags & 0x4) != 0) {
			dontInheritScaling = true;
		}

		if ((object.flags & 0x8) != 0) {
			billboarded = true;
		}

		if ((object.flags & 0x10) != 0) {
			billboardLockX = true;
		}

		if ((object.flags & 0x20) != 0) {
			billboardLockY = true;
		}

		if ((object.flags & 0x40) != 0) {
			billboardLockZ = true;
		}

		loadTimelines(object);
	}

	public void objectToMdlx(final MdlxGenericObject object) {
		object.name = getName();
		object.objectId = getObjectId();
		object.parentId = getParentId();
		
		if (dontInheritTranslation) {
			object.flags |= 0x1;
		}

		if (dontInheritRotation) {
			object.flags |= 0x2;
		}

		if (dontInheritScaling) {
			object.flags |= 0x4;
		}

		if (billboarded) {
			object.flags |= 0x8;
		}

		if (billboardLockX) {
			object.flags |= 0x10;
		}

		if (billboardLockY) {
			object.flags |= 0x20;
		}

		if (billboardLockZ) {
			object.flags |= 0x40;
		}

		timelinesToMdlx(object);
	}
	
	public void setName(final String text) {
		name = text;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setPivotPoint(final Vertex p) {
		pivotPoint = p;
	}

	public void setParent(final IdObject p) {
		if (parent != null) {
			parent.childrenNodes.remove(this);
		}
		parent = p;
		if (parent != null) {
			parent.childrenNodes.add(this);
		}
	}

	public IdObject copy() {
		return null;
	}

	public void copyObject(final IdObject other) {
		name = other.name;
		objectId = other.objectId;
		parentId = other.parentId;
		dontInheritTranslation = other.dontInheritTranslation;
		dontInheritRotation = other.dontInheritRotation;
		dontInheritScaling = other.dontInheritScaling;
		billboarded = other.billboarded;
		billboardLockX = other.billboardLockX;
		billboardLockY = other.billboardLockY;
		billboardLockZ = other.billboardLockZ;
		pivotPoint = new Vertex(other.getPivotPoint());
		setParent(other.getParent());
		addAll(other.getAnimFlags());
	}

	public boolean childOf(final IdObject other) {
		if (parent != null) {
			if (parent == other) {
				return true;
			} else {
				return parent.childOf(other);
			}
		}
		return false;
	}

	public abstract double getClickRadius(CoordinateSystem coordinateSystem);

	public boolean parentOf(final IdObject other, final Map<IdObject, List<IdObject>> childMap) {
		final List<IdObject> children = childMap.get(this);
		if (children != null) {
			if (children.contains(other)) {
				return true;
			} else {
				boolean deepChild = false;
				for (int i = 0; !deepChild && (i < children.size()); i++) {
					deepChild = children.get(i).parentOf(other, childMap);
				}
				return deepChild;
			}
		}
		return false;
	}

	public List<IdObject> getAllChildren(final Map<IdObject, List<IdObject>> childMap) {
		final List<IdObject> children = childMap.get(this);
		final List<IdObject> allChildren = new ArrayList<>();
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				final IdObject child = children.get(i);
				if (!allChildren.contains(child)) {
					allChildren.add(child);
					allChildren.addAll(child.getAllChildren(childMap));
				}
			}
		}

		return allChildren;
	}

	/**
	 *
	 *
	 * @return The Object ID
	 * @deprecated Note that all object IDs are deleted and regenerated at save
	 */
	@Deprecated
	public int getObjectId() {
		return objectId;
	}

	/**
	 * @param objectId New object ID value
	 * @deprecated Note that all object IDs are deleted and regenerated at save
	 */
	@Deprecated
	public void setObjectId(final int objectId) {
		this.objectId = objectId;
	}

	/**
	 * @return Parent ID
	 * @deprecated Note that all object IDs are deleted and regenerated at save
	 */
	@Deprecated
	public int getParentId() {
		return parentId;
	}

	/**
	 * @param parentId new Parent ID
	 * @deprecated IF UNSURE, YOU SHOULD USE setParent(), note that all object IDs
	 *             are deleted and regenerated at save
	 */
	@Deprecated
	public void setParentId(final int parentId) {
		this.parentId = parentId;
	}

	public void setDontInheritTranslation(boolean dontInheritTranslation) {
		this.dontInheritTranslation = dontInheritTranslation;
	}

	public boolean getDontInheritTranslation() {
		return dontInheritTranslation;
	}

	public void setDontInheritRotation(boolean dontInheritRotation) {
		this.dontInheritRotation = dontInheritRotation;
	}

	public boolean getDontInheritRotation() {
		return dontInheritRotation;
	}

	public void setDontInheritScaling(boolean dontInheritScaling) {
		this.dontInheritScaling = dontInheritScaling;
	}

	public boolean getDontInheritScaling() {
		return dontInheritScaling;
	}

	public void setBillboarded(boolean billboarded) {
		this.billboarded = billboarded;
	}

	public boolean getBillboarded() {
		return billboarded;
	}

	public void setBillboardLockX(boolean billboardLockX) {
		this.billboardLockX = billboardLockX;
	}

	public boolean getBillboardLockX() {
		return billboardLockX;
	}

	public void setBillboardLockY(boolean billboardLockY) {
		this.billboardLockY = billboardLockY;
	}

	public boolean getBillboardLockY() {
		return billboardLockY;
	}

	public void setBillboardLockZ(boolean billboardLockZ) {
		this.billboardLockZ = billboardLockZ;
	}

	public boolean getBillboardLockZ() {
		return billboardLockZ;
	}

	@Override
	public Vertex getPivotPoint() {
		return pivotPoint;
	}

	@Override
	public IdObject getParent() {
		return parent;
	}

	public abstract void apply(IdObjectVisitor visitor);

	@Override
	public List<IdObject> getChildrenNodes() {
		return childrenNodes;
	}

	public float[] getBindPose() {
		return bindPose;
	}

	public void setBindPose(final float[] bindPose) {
		this.bindPose = bindPose;
	}
}
