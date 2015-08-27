package net.vinote.smart.socket.security;

/**
 * 
 * 处理密码,该类用以处理密码的相关操作
 * 
 * @author xKF20126
 * @version [V100R002C01L00001, 2009-8-22]
 * @see [相关的方法]
 * @since dms
 */
public final class Base64 {
	static final int Z = 0xff;

	static final int X = 0x3f;

	/**
	 * This array is a lookup table that translates 6-bit positive integer index
	 * values into their "Base64 Alphabet" equivalents as specified in Table 1
	 * of RFC 2045.
	 */
	private static final char[] INTTOBASE64 = { 'A', 'B', 'C', 'D', 'E', 'F',
			'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
			'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
			'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
			't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', '+', '/' };

	/**
	 * This array is a lookup table that translates 6-bit positive integer index
	 * values into their "Alternate Base64 Alphabet" equivalents. This is NOT
	 * the real Base64 Alphabet as per in Table 1 of RFC 2045. This alternate
	 * alphabet does not use the capital letters. It is designed for use in
	 * environments where "case folding" occurs.
	 */
	private static final char[] INTTOALTBASE64 = { '!', '"', '#', '$', '%',
			'&', '\'', '(', ')', ',', '-', '.', ':', ';', '<', '>', '@', '[',
			']', '^', '`', '_', '{', '|', '}', '~', 'a', 'b', 'c', 'd', 'e',
			'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
			's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9', '+', '?' };

	/**
	 * This array is a lookup table that translates unicode characters drawn
	 * from the "Base64 Alphabet" (as specified in Table 1 of RFC 2045) into
	 * their 6-bit positive integer equivalents. Characters that are not in the
	 * Base64 alphabet but fall within the bounds of the array are translated to
	 * -1.
	 */
	private static final byte[] BASE64TOINT = { -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1,
			-1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
			13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1,
			-1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
			41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 };

	/**
	 * This array is the analogue of base64ToInt, but for the nonstandard
	 * variant that avoids the use of uppercase alphabetic characters.
	 */
	private static final byte[] ALTBASE64TOINT = { -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, -1,
			62, 9, 10, 11, -1, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 12, 13,
			14, -1, 15, 63, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 17, -1, 18,
			19, 21, 20, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
			40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 22, 23, 24, 25 };

	private Base64() {

	}

	/**
	 * byteArrayToBase64
	 * 
	 * @param a
	 *            字节数组
	 * @return String 字符�?
	 */
	public static String byteArrayToBase64(byte[] a) {
		return byteArrayToBase64(a, false);
	}

	/**
	 * byteArrayToAltBase64
	 * 
	 * @param a
	 *            字节数组
	 * @return string 字符�?
	 */
	public static String byteArrayToAltBase64(byte[] a) {
		return byteArrayToBase64(a, true);
	}

	private static String byteArrayToBase64(byte[] a, boolean alternate) {
		int aLen = a.length;
		int numFullGroups = aLen / 3;
		int numBytesInPartialGroup = aLen - 3 * numFullGroups;
		int resultLen = 4 * ((aLen + 2) / 3);
		StringBuffer result = new StringBuffer(resultLen);
		char[] intToAlpha = alternate ? INTTOALTBASE64 : INTTOBASE64;

		int inCursor = 0;
		int byte0 = 0;
		int byte1 = 0;
		int byte2 = 0;

		for (int i = 0; i < numFullGroups; i++) {
			byte0 = a[inCursor++] & Z;
			byte1 = a[inCursor++] & Z;
			byte2 = a[inCursor++] & Z;
			result.append(intToAlpha[byte0 >> 2]);
			result.append(intToAlpha[(byte0 << 4) & X | (byte1 >> 4)]);
			result.append(intToAlpha[(byte1 << 2) & X | (byte2 >> 6)]);
			result.append(intToAlpha[byte2 & X]);
		}

		if (numBytesInPartialGroup != 0) {
			byte0 = a[inCursor++] & Z;
			result.append(intToAlpha[byte0 >> 2]);
			if (numBytesInPartialGroup == 1) {
				result.append(intToAlpha[(byte0 << 4) & X]);
				result.append("==");
			} else {
				byte1 = a[inCursor++] & Z;
				result.append(intToAlpha[(byte0 << 4) & X | (byte1 >> 4)]);
				result.append(intToAlpha[(byte1 << 2) & X]);
				result.append('=');
			}
		}
		return result.toString();
	}

	/**
	 * base64ToByteArray
	 * 
	 * @param s
	 *            字符
	 * @return 字节数组
	 */
	public static byte[] base64ToByteArray(String s) {
		return base64ToByteArray(s, false);
	}

	/**
	 * altBase64ToByteArray
	 * 
	 * @param s
	 *            字符
	 * @return 字节数组
	 */
	public static byte[] altBase64ToByteArray(String s) {
		return base64ToByteArray(s, true);
	}

	private static byte[] base64ToByteArray(String s, boolean alternate) {
		byte[] alphaToInt = alternate ? ALTBASE64TOINT : BASE64TOINT;
		int sLen = s.length();
		int numGroups = sLen / 4;
		if (4 * numGroups != sLen) {
			throw new IllegalArgumentException(
					"String length must be a multiple of four.");
		}
		int missingBytesInLastGroup = 0;
		int numFullGroups = numGroups;
		if (sLen != 0) {
			if (s.charAt(sLen - 1) == '=') {
				missingBytesInLastGroup++;
				numFullGroups--;
			}
			if (s.charAt(sLen - 2) == '=') {
				missingBytesInLastGroup++;
			}
		}
		byte[] result = new byte[3 * numGroups - missingBytesInLastGroup];
		int inCursor = 0;
		int outCursor = 0;
		int ch0 = 0;
		int ch1 = 0;
		int ch2 = 0;
		int ch3 = 0;
		for (int i = 0; i < numFullGroups; i++) {
			ch0 = base64toInts(s.charAt(inCursor++), alphaToInt);
			ch1 = base64toInts(s.charAt(inCursor++), alphaToInt);
			ch2 = base64toInts(s.charAt(inCursor++), alphaToInt);
			ch3 = base64toInts(s.charAt(inCursor++), alphaToInt);
			result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));
			result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
			result[outCursor++] = (byte) ((ch2 << 6) | ch3);
		}
		if (missingBytesInLastGroup != 0) {
			ch0 = base64toInts(s.charAt(inCursor++), alphaToInt);
			ch1 = base64toInts(s.charAt(inCursor++), alphaToInt);
			result[outCursor++] = (byte) ((ch0 << 2) | (ch1 >> 4));

			if (missingBytesInLastGroup == 1) {
				ch2 = base64toInts(s.charAt(inCursor++), alphaToInt);
				result[outCursor++] = (byte) ((ch1 << 4) | (ch2 >> 2));
			}
		}
		return result;
	}

	/**
	 * base64toInt
	 * 
	 * @param c
	 *            字符
	 * @param alphaToInt
	 *            字节数组
	 * @return results int
	 */
	private static int base64toInts(char c, byte[] alphaToInt) {
		int result = alphaToInt[c];
		if (result < 0) {
			throw new IllegalArgumentException("Illegal character " + c);
		}
		return result;
	}

	/**
	 * Base64解码�?
	 * 
	 * @param code
	 *            用Base64编码的ASCII字符�?
	 * @return 解码后的字节数据
	 */
	public static byte[] decode(String code) {

		// �?��参数合法�?
		if (code == null) {
			return null;
		}
		int len = code.length();

		if (len % 4 != 0) {
			throw new IllegalArgumentException(
					"Base64 string length must be 4*n");
		}
		if (code.length() == 0) {
			return new byte[0];
		}

		// 统计填充的等号个�?
		int pad = 0;
		if (code.charAt(len - 1) == '=') {
			pad++;
		}

		if (code.charAt(len - 2) == '=') {
			pad++;
		}

		// 根据填充等号的个数来计算实际数据长度
		int retLen = len / 4 * 3 - pad;

		// 分配字节数组空间
		byte[] ret = new byte[retLen];

		final int num6 = 0xFF0000;
		final int num7 = 0x00FF00;
		final int num8 = 0x0000FF;
		// 查表解码
		char ch1 = '\u0000', ch2 = '\u0000', ch3 = '\u0000', ch4 = '\u0000';
		int i = 0;
		int j = 0;
		int tmp = 0;
		for (i = 0; i < len; i += 4) {
			j = i / 4 * 3;
			ch1 = code.charAt(i);
			ch2 = code.charAt(i + 1);
			ch3 = code.charAt(i + 2);
			ch4 = code.charAt(i + 3);
			tmp = (base64Decode[ch1] << 18) | (base64Decode[ch2] << 12)
					| (base64Decode[ch3] << 6) | (base64Decode[ch4]);
			ret[j] = (byte) ((tmp & num6) >> 16);
			if (i < len - 4) {
				ret[j + 1] = (byte) ((tmp & num7) >> 8);
				ret[j + 2] = (byte) ((tmp & num8));
			} else {
				if (j + 1 < retLen) {
					ret[j + 1] = (byte) ((tmp & num7) >> 8);
				}
				if (j + 2 < retLen) {
					ret[j + 2] = (byte) ((tmp & num8));
				}
			}
		}
		return ret;
	}

	private static byte[] base64Decode = { -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1,
			-1,
			-1, // 注意两个63，为兼容SMP�?
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, 63,
			-1,
			63, // �?”和�?”都翻译�?3�?
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, 0, -1, -1, -1,
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
			14, // 注意两个0�?
			15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1,
			-1, // “A”和�?”都翻译�?�?
			-1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
			42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, };

}