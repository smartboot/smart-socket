package org.smartboot.socket.protocol.p2p.enums;

import org.smartboot.socket.protocol.p2p.util.StringUtils;

/**
 * 状态返回码枚举
 * 
 * @author 三刀
 * @version ReturnCodeEnum.java, v 0.1 2015年8月24日 下午6:24:03 Seer Exp.
 */
public enum ReturnCodeEnum {

	/** 成功 */
	SUCCESS("SUCCESS", "成功"),
	/** 待鉴权 */
	NEED_AUTH("NEED_AUTH", "待鉴权"), ;

	/** 状态码 */
	private String code;

	/** 状态描述 */
	private String desc;

	/**
	 * 构造函数
	 * 
	 * @param code
	 * @param desc
	 */
	private ReturnCodeEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	/**
	 * 根据状态获取 CategoryStatusEnum
	 * 
	 * @param code
	 *            状态码
	 * @return
	 */
	public static ReturnCodeEnum getReturnEnumByCode(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}
		for (ReturnCodeEnum codeEnum : ReturnCodeEnum.values()) {
			if (StringUtils.equals(codeEnum.getCode(), code)) {
				return codeEnum;
			}
		}
		return null;
	}

	/**
	 * Getter method for property <tt>code</tt>.
	 * 
	 * @return property value of code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Setter method for property <tt>code</tt>.
	 * 
	 * @param code
	 *            value to be assigned to property code
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Getter method for property <tt>desc</tt>.
	 * 
	 * @return property value of desc
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * Setter method for property <tt>desc</tt>.
	 * 
	 * @param desc
	 *            value to be assigned to property desc
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

}
