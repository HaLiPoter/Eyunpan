package com.eyunpan.entity.qo;


import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 
 * 用户信息参数
 * 
 */
@Data
public class UserInfoQO extends BaseParam {


	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 昵称
	 */
	private String nickName;

	private String nickNameFuzzy;

	/**
	 * 邮箱
	 */
	private String email;

	private String emailFuzzy;

	/**
	 * 密码
	 */
	private String password;

	private String passwordFuzzy;

	/**
	 * 加入时间
	 */
	private String joinTime;

	private String joinTimeStart;

	private String joinTimeEnd;

	/**
	 * 最后登录时间
	 */
	private String lastLoginTime;

	private String lastLoginTimeStart;

	private String lastLoginTimeEnd;

	/**
	 * 0:禁用 1:正常
	 */
	private Integer status;

	/**
	 * 
	 */
	private Long useSpace;

	/**
	 * 
	 */
	private Long totalSpace;
}
