package com.hiveworkshop.wc3.gui.modeledit;

public final class BasicCoordinateSystem implements CoordinateSystem {
	private final byte dimension1;
	private final byte dimension2;
	private final double cameraX;
	private final double cameraY;
	private final double zoom;
	private final int width;
	private final int height;

	public BasicCoordinateSystem(final byte dimension1, final byte dimension2, final double cameraX,
			final double cameraY, final double zoom, final int width, final int height) {
		this.dimension1 = dimension1;
		this.dimension2 = dimension2;
		this.cameraX = cameraX;
		this.cameraY = cameraY;
		this.zoom = zoom;
		this.width = width;
		this.height = height;
	}

	@Override
	public double convertX(final double x) {
		return (x + cameraX) * zoom + width / 2;
	}

	@Override
	public double convertY(final double y) {
		return ((-y + cameraY) * zoom) + height / 2;
	}

	@Override
	public double geomX(final double x) {
		return (x - width / 2) / zoom - cameraX;
	}

	@Override
	public double geomY(final double y) {
		return -((y - height / 2) / zoom - cameraY);
	}

	@Override
	public byte getPortFirstXYZ() {
		return dimension1;
	}

	@Override
	public byte getPortSecondXYZ() {
		return dimension2;
	}

	@Override
	public CoordinateSystem copy() {
		return new BasicCoordinateSystem(dimension1, dimension2, cameraX, cameraY, zoom, width, height);
	}
}
