package com.etheller.warsmash.parsers.mdlx.mdl;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class MdlTokenInputStream {
	private final ByteBuffer buffer;
	private int index;

	public MdlTokenInputStream(final ByteBuffer buffer) {
		this.buffer = buffer;
		this.index = 0;
	}

	public String read() {
		boolean inComment = false;
		boolean inString = false;
		final StringBuilder token = new StringBuilder();
		final int length = this.buffer.remaining();

		while (this.index < length) {
			// Note: cast from 'byte' to 'char' will cause Java incompatibility with Chinese
			// and Russian/Cyrillic and others
			final char c = (char) this.buffer.get(this.buffer.position() + this.index++);

			if (inComment) {
				if (c == '\n') {
					inComment = false;
				}
			}
			else if (inString) {
				if (c == '"') {
					return token.toString();
				}
				else {
					token.append(c);
				}
			}
			else if ((c == ' ') || (c == ',') || (c == '\t') || (c == '\n') || (c == ':') || (c == '\r')) {
				if (token.length() > 0) {
					return token.toString();
				}
			}
			else if ((c == '{') || (c == '}')) {
				if (token.length() > 0) {
					this.index--;
					return token.toString();
				}
				else {
					return Character.toString(c);
				}
			}
			else if ((c == '/') && (this.buffer.get(this.buffer.position() + this.index) == '/')) {
				if (token.length() > 0) {
					this.index--;
					return token.toString();
				}
				else {
					inComment = true;
				}
			}
			else if (c == '"') {
				if (token.length() > 0) {
					this.index--;
					return token.toString();
				}
				else {
					inString = true;
				}
			}
			else {
				token.append(c);
			}
		}
		return null;
	}

	public String peek() {
		final int index = this.index;
		final String value = this.read();

		this.index = index;
		return value;
	}

	public long readUInt32() {
		return Long.parseLong(this.read());
	}

	public int readInt() {
		return Integer.parseInt(this.read());
	}

	public float readFloat() {
		return Float.parseFloat(this.read());
	}

	public void readIntArray(final long[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = this.readInt();
		}

		this.read(); // }
	}

	public float[] readFloatArray(final float[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = this.readFloat();
		}

		this.read(); // }
		return values;
	}

	/**
	 * Read an MDL keyframe value. If the value is a scalar, it is just the number.
	 * If the value is a vector, it is enclosed with curly braces.
	 *
	 * @param {Float32Array|Uint32Array} value
	 */
	public void readKeyframe(final float[] values) {
		if (values.length == 1) {
			values[0] = this.readFloat();
		}
		else {
			this.readFloatArray(values);
		}
	}

	public float[] readVectorArray(final float[] array, final int vectorLength) {
		this.read(); // {

		for (int i = 0, l = array.length / vectorLength; i < l; i++) {
			this.read(); // {

			for (int j = 0; j < vectorLength; j++) {
				array[(i * vectorLength) + j] = this.readFloat();
			}

			this.read(); // }
		}

		this.read(); // }
		return array;
	}

	public Iterable<String> readBlock() {
		this.read(); // {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					String current;
					private boolean hasLoaded = false;

					@Override
					public String next() {
						if (!this.hasLoaded) {
							hasNext();
						}
						this.hasLoaded = false;
						return this.current;
					}

					@Override
					public boolean hasNext() {
						this.current = read();
						this.hasLoaded = true;
						return (this.current != null) && !this.current.equals("}");
					}
				};
			}
		};
	}

	public int[] readUInt16Array(final int[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = this.readInt();
		}

		this.read(); // }

		return values;
	}

	public short[] readUInt8Array(final short[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = Short.parseShort(this.read());
		}

		this.read(); // }

		return values;
	}

	public void readColor(final float[] color) {
		this.read(); // {

		color[2] = this.readFloat();
		color[1] = this.readFloat();
		color[0] = this.readFloat();

		this.read(); // }
	}
}
