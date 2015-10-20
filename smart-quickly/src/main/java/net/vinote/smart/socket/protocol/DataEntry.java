package net.vinote.smart.socket.protocol;

import java.net.ProtocolException;
import java.util.Arrays;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import net.vinote.smart.socket.exception.DecodeException;

/**
 * 数据报文的存储实体
 *
 * @author Seer
 * @version DataEntry.java, v 0.1 2015年8月28日 下午4:33:59 Seer Exp.
 */
public abstract class DataEntry {

	/** 完整的数据流 */
	private byte[] data;

	/** 是否发生修改 */
	private boolean modified = false;

	/** 当前数据存储区处于的操作模式 */
	private MODE mode;

	/** 当前索引 */
	private int index;

	/** 限制索引,当limit>0且index>limit,进行读写操作都跑异常 */
	private int limit = -1;

	public static final int DEFAULT_DATA_LENGTH = 1024;

	/** 数据流临时缓存区 */
	private byte[] tempData = new byte[DEFAULT_DATA_LENGTH];

	/**
	 * 读取一个布尔值
	 *
	 * @return
	 */
	public final boolean readBoolen() {
		assertMode(MODE.READ);
		assertLimit(index);
		return data[index++] == 1;
	}

	/**
	 * 从数据块中当前位置开始读取一个byte长度的整形值
	 *
	 * @return
	 */
	public final byte readByte() {
		assertMode(MODE.READ);
		assertLimit(index);
		return data[index++];
	}

	/**
	 * 从数据块中当前位置开始读取一个byte数值
	 *
	 * @return
	 */
	public final byte[] readBytes() {
		int size = readInt();
		if (size < 0) {
			return null;
		}
		assertLimit(index + size - 1);
		byte[] bytes = new byte[size];
		System.arraycopy(data, index, bytes, 0, size);
		index += size;
		return bytes;
	}

	/**
	 * 从数据块中反序列化对象
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T> T readObjectByProtobuf() {
		byte[] bytes = readBytes();
		if (bytes == null) {
			return null;
		}
		SerializableBean bean = new SerializableBean();
		Schema<SerializableBean> schema = RuntimeSchema.getSchema(SerializableBean.class);
		ProtobufIOUtil.mergeFrom(bytes, bean, schema);
		return (T) bean.getBean();
	}

	/**
	 * 从数据块中当前位置开始读取一个int长度的整形值
	 *
	 * @return
	 */
	public final int readInt() {
		assertMode(MODE.READ);
		assertLimit(index + 3);
		return ((data[index++] & 0xff) << 24) + ((data[index++] & 0xff) << 16) + ((data[index++] & 0xff) << 8)
			+ (data[index++] & 0xff);
	}

	/**
	 * 重数据块中读取一个short长度的整形值
	 *
	 * @return
	 */
	public final short readShort() {
		assertMode(MODE.READ);
		assertLimit(index + 1);
		return (short) (((data[index++] & 0xff) << 8) + (data[index++] & 0xff));
	}

	/**
	 * 输出布尔值
	 *
	 * @param flag
	 */
	public final void writeBoolean(boolean flag) {
		writeByte(flag ? (byte) 1 : 0);
	}

	/**
	 * 往数据块中输入byte数值
	 *
	 * @param i
	 */
	public final void writeByte(byte i) {
		modified = true;
		ensureCapacity(index + 1);
		assertLimit(index);
		tempData[index++] = i;
	}

	/**
	 * 将对象进行序列化输出
	 *
	 * @param object
	 */
	public final <T> void writeObjectByProtobuf(T object) {
		Schema<SerializableBean> schema = RuntimeSchema.getSchema(SerializableBean.class);
		// 缓存buff
		LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
		// 序列化成protobuf的二进制数据
		SerializableBean bean = new SerializableBean();
		bean.setBean(object);
		byte[] data = ProtobufIOUtil.toByteArray(bean, schema, buffer);
		writeBytes(data);
	}

	/**
	 * 往数据块中输出byte数组
	 *
	 * @param data
	 */
	public final void writeBytes(byte[] data) {
		if (data != null) {
			assertLimit(index + data.length + 3);
			writeInt(data.length);
			ensureCapacity(index + data.length);
			System.arraycopy(data, 0, tempData, index, data.length);
			index += data.length;
		} else {
			writeInt(-1);
		}
	}

	/**
	 * 往数据块中输入int数值
	 *
	 * @param i
	 */
	public final void writeInt(int i) {
		modified = true;
		assertMode(MODE.WRITE);
		assertLimit(index + 3);
		ensureCapacity(index + 4);
		tempData[index++] = (byte) ((0xff000000 & i) >> 24);
		tempData[index++] = (byte) ((0xff0000 & i) >> 16);
		tempData[index++] = (byte) ((0xff00 & i) >> 8);
		tempData[index++] = (byte) (0xff & i);
	}

	/**
	 * 往数据块中输入short数值
	 *
	 * @param i
	 */
	public final void writeShort(int i) {
		modified = true;
		assertMode(MODE.WRITE);
		assertLimit(index + 1);
		ensureCapacity(index + 2);
		tempData[index++] = (byte) ((0xff00 & i) >> 8);
		tempData[index++] = (byte) (0xff & i);
	}

	protected final void reset(MODE mode) {
		index = 0;
		this.mode = mode;
		if (mode == MODE.WRITE) {
			modified = true;
		}
	}

	/**
	 * 输出字符串至数据体,以0x00作为结束标识符
	 *
	 * @param str
	 */
	public final void writeString(String str) {
		assertMode(MODE.WRITE);
		if (str == null) {
			str = "";
		}
		modified = true;
		byte[] bytes = str.getBytes();
		assertLimit(index + bytes.length);
		ensureCapacity(index + 1 + bytes.length);
		for (byte b : bytes) {
			tempData[index++] = b;
		}
		tempData[index++] = 0x00;
	}

	/**
	 * 从数据块的当前位置开始读取字符串
	 *
	 * @return
	 */
	public final String readString() {
		assertMode(MODE.READ);
		int curIndex = index;
		if (limit > 0) {// 减少判断次数
			do {
				assertLimit(index);
			} while (data[index++] != 0x00);
		} else {
			while (data[index++] != 0x00) {
				;
			}
		}
		return new String(data, curIndex, index - curIndex - 1);
	}

	/**
	 * 定位至数据流中的第n+1位
	 *
	 * @param n
	 */
	public final void position(int n) {
		ensureCapacity(n + 1);
		index = n;
	}

	/**
	 * 当前游标位置
	 */
	public final int getPosition() {
		return index;
	}

	/**
	 * 若当前数据体处于read模式,则直接获取data,否则从临时数据区writeData拷贝至data中再返回
	 * <p>
	 * 在read模式下请确保已经完成了解密才可调用getData方法，否则会造成未完成解码的数据丢失
	 *
	 * @return
	 */
	public final byte[] getData() {
		if (mode == MODE.WRITE && modified) {
			data = new byte[index];
			System.arraycopy(tempData, 0, data, 0, index);
			modified = false;// 避免每次调用getData都进行数组拷贝
		} else if (mode == MODE.READ && modified) {
			byte[] d = new byte[index];
			System.arraycopy(data, 0, d, 0, index);
			data = d;
			modified = false;// 避免每次调用getData都进行数组拷贝
		}
		return data;
	}

	public final void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * 断言当前数据库所处的模式为read or write
	 *
	 * @param mode
	 */
	private final void assertMode(MODE mode) {
		if (mode != this.mode) {
			throw new RuntimeException("current mode is " + this.mode + ", can not " + mode);
		}
	}

	/**
	 * 预期操作的数据是否超过限制索引
	 *
	 * @param lockIndex
	 */
	private final void assertLimit(int lockIndex) {
		if (limit > -1 && lockIndex > limit) {
			throw new ArrayIndexOutOfBoundsException(limit);
		}
	}

	/**
	 * 从当前索引位置开始锁定操作位
	 *
	 * @param size
	 */
	protected final void limitFromCurrentIndex(int size) {
		limit = index + size - 1;
	}

	/**
	 * 限制操作范围
	 *
	 * @param size
	 */
	protected final void limitIndex(int limit) {
		this.limit = limit;
	}

	/**
	 * 清除限制
	 */
	public void clearLimit() {
		limit = -1;
	}

	/**
	 * 确保足够的存储容量
	 *
	 * @param minCapacity
	 */
	private void ensureCapacity(int minCapacity) {
		int oldCapacity = tempData.length;
		if (minCapacity > oldCapacity) {
			int newCapacity = oldCapacity * 3 / 2 + 1;
			if (newCapacity < minCapacity) {
				newCapacity = minCapacity;
			}
			tempData = Arrays.copyOf(tempData, newCapacity);
		}
	}

	public abstract byte[] encode() throws ProtocolException;

	public abstract void decode() throws DecodeException;

	public enum MODE {
		READ, WRITE;
	}

	protected void setModified(boolean modified) {
		this.modified = modified;
	}

}
