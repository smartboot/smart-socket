package net.vinote.smart.socket.lang;

import java.util.Arrays;

public class SmartByteBuffer {
	/**
	 * The value is used for character storage.
	 */
	private byte value[];

	/**
	 * The count is the number of characters used.
	 */
	private int count;

	private boolean locked = false;

	public SmartByteBuffer() {
		value = new byte[16];
	}

	public int length() {
		return count;
	}

	public byte byteAt(int index) {
		if ((index < 0) || (index >= count))
			throw new StringIndexOutOfBoundsException(index);
		return value[index];
	}

	public SmartByteBuffer append(byte c) {
		assertLock();
		int newCount = count + 1;
		if (newCount > value.length)
			expandCapacity(newCount);
		value[count++] = c;
		return this;
	}

	private void assertLock() {
		if (locked) {
			throw new RuntimeException(
					"current smartByteBuffer has been locked");
		}
	}

	public SmartByteBuffer append(byte[] bytes) {
		assertLock();
		int newCount = count + bytes.length;
		if (newCount > value.length)
			expandCapacity(newCount);
		System.arraycopy(bytes, 0, value, count, bytes.length);
		count = newCount;
		return this;
	}

	/**
	 * This implements the expansion semantics of ensureCapacity with no size
	 * check or synchronization.
	 */
	private void expandCapacity(int minimumCapacity) {
		int newCapacity = (value.length + 1) * 2;
		if (newCapacity < 0) {
			newCapacity = Integer.MAX_VALUE;
		} else if (minimumCapacity > newCapacity) {
			newCapacity = minimumCapacity;
		}
		value = Arrays.copyOf(value, newCapacity);
	}

	public void lock() {
		locked = true;
	}

	public String toString() {
		return new String(value, 0, count);
	}
}
