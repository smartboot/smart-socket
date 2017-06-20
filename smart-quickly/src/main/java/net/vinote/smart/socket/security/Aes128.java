package net.vinote.smart.socket.security;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES128加解密
 * 
 * @author Seer
 * @version Aes128.java, v 0.1 2015年8月27日 下午3:09:43 Seer Exp.
 */
public class Aes128 {
	public static final String MODE_ECB = "ECB";
	public static final String MODE_CBC = "CBC";
	public static final String MODE_PCBC = "PCBC";
	public static final String MODE_CTR = "CTR";
	public static final String MODE_CTS = "CTS";
	public static final String MODE_CFB = "CFB";

	public static final String NO_PADDING = "NoPadding";
	public static final String PK_CS5_PADDING = "PKCS5Padding";
	public static final String ISO_10126_PADDING = "ISO10126Padding";

	/**
	 * @param data
	 *            待解密的数据
	 * @param key
	 *            秘钥
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
		return decrypt(data, key, MODE_ECB, PK_CS5_PADDING);
	}

	/**
	 * @param data
	 *            待解密的数据
	 * @param key
	 *            秘钥
	 * @param mode
	 *            工作模式
	 * @param padding
	 *            填充方式
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] data, byte[] key, String mode,
			String padding) throws Exception {
		Key k = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/" + mode + "/" + padding);
		cipher.init(Cipher.DECRYPT_MODE, k);
		return cipher.doFinal(data);
	}

	/**
	 * @param data
	 *            待加密的数据
	 * @param key
	 *            秘钥
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
		return encrypt(data, key, MODE_ECB, PK_CS5_PADDING);
	}

	/**
	 * @param data
	 *            待加密的数据
	 * @param key
	 *            秘钥
	 * @param mode
	 *            工作模式
	 * @param padding
	 *            填充方式
	 * @return 加密后的byte数组
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] data, byte[] key, String mode,
			String padding) throws Exception {
		Key k = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/" + mode + "/" + padding);
		cipher.init(Cipher.ENCRYPT_MODE, k);
		return cipher.doFinal(data);
	}
}