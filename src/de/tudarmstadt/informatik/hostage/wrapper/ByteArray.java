package de.tudarmstadt.informatik.hostage.wrapper;

public class ByteArray {

	private final byte[] array;

	public ByteArray() {
		this.array = null;
	}

	public ByteArray(byte[] array) {
		this.array = array;
	}

	public byte[] get() {
		return array;
	}

	public int size() {
		return array.length;
	}

	@Override
	public String toString() {
		return new String(array);
	}

}
