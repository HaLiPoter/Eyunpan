package com.eyunpan.exception;


import com.eyunpan.entity.enums.ResponseCodeEnum;

/**
 * 自定义错误类，继承自RuntimeException
 */
public class CustomException extends RuntimeException {

    //响应码枚举类
    private ResponseCodeEnum codeEnum;

    //响应码
    private Integer code;

    //消息
    private String message;

    //各种构造方法
    public CustomException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

    public CustomException(String message) {
        super(message);
        this.message = message;
    }

    public CustomException(Throwable e) {
        super(e);
    }

    public CustomException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    public CustomException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public ResponseCodeEnum getCodeEnum() {
        return codeEnum;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
