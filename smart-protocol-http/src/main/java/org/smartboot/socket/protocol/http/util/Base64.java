package org.smartboot.socket.protocol.http.util;

/**
 * Base 64 implementation.
 * 
 * TODO add encode function, and test unit around this.
 */
public final class Base64 {

	private static byte B64_DECODE_ARRAY[] = new byte[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, // Plus
			// sign
			-1, -1, -1, 63, // Slash
			52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers
			-1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Large
			// letters
			-1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Small
			// letters
			-1, -1, -1, -1 };

	/**
	 * Expects the classic base64 "abcdefgh=" syntax (equals padded) and decodes
	 * it to original form
	 * 
	 * @param input
	 * @return
	 */
	public static String decode(final String input) {
		final char[] inBytes = input.toCharArray();
		final byte[] outBytes = new byte[(int) (inBytes.length * 0.75f)]; // always
																			// mod
																			// 4
																			// =
																			// 0
		final int length = Base64.decode(inBytes, outBytes, 0, inBytes.length, 0);
		return new String(outBytes, 0, length);
	}

	/**
	 * Decodes a byte array from base64
	 */
	public static int decode(final char[] input, final byte[] output, final int inOffset, final int inLength, final int outOffset) {
		if (inLength == 0) {
			return 0;
		}

		int outIndex = outOffset;
		for (int inIndex = inOffset; inIndex < inLength;) {
			// Decode four bytes
			int thisPassInBytes = Math.min(inLength - inIndex, 4);
			while ((thisPassInBytes > 1) && (input[(inIndex + thisPassInBytes) - 1] == '=')) {
				thisPassInBytes--;
			}

			if (thisPassInBytes == 2) {
				final int outBuffer = ((Base64.B64_DECODE_ARRAY[input[inIndex]] & 0xFF) << 18) | ((Base64.B64_DECODE_ARRAY[input[inIndex + 1]] & 0xFF) << 12);
				output[outIndex] = (byte) ((outBuffer >> 16) & 0xFF);
				outIndex += 1;
			} else if (thisPassInBytes == 3) {
				final int outBuffer = ((Base64.B64_DECODE_ARRAY[input[inIndex]] & 0xFF) << 18) | ((Base64.B64_DECODE_ARRAY[input[inIndex + 1]] & 0xFF) << 12) | ((Base64.B64_DECODE_ARRAY[input[inIndex + 2]] & 0xFF) << 6);
				output[outIndex] = (byte) ((outBuffer >> 16) & 0xFF);
				output[outIndex + 1] = (byte) ((outBuffer >> 8) & 0xFF);
				outIndex += 2;
			} else if (thisPassInBytes == 4) {
				final int outBuffer = ((Base64.B64_DECODE_ARRAY[input[inIndex]] & 0xFF) << 18) | ((Base64.B64_DECODE_ARRAY[input[inIndex + 1]] & 0xFF) << 12) | ((Base64.B64_DECODE_ARRAY[input[inIndex + 2]] & 0xFF) << 6)
						| (Base64.B64_DECODE_ARRAY[input[inIndex + 3]] & 0xFF);
				output[outIndex] = (byte) ((outBuffer >> 16) & 0xFF);
				output[outIndex + 1] = (byte) ((outBuffer >> 8) & 0xFF);
				output[outIndex + 2] = (byte) (outBuffer & 0xFF);
				outIndex += 3;
			}
			inIndex += thisPassInBytes;
		}
		return outIndex;
	}

}
