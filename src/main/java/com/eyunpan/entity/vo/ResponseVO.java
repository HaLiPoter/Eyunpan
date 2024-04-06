package com.eyunpan.entity.vo;

import com.eyunpan.entity.enums.ResponseCodeEnum;
import lombok.Data;

@Data
public class ResponseVO<T> {

    public static final String STATUC_SUCCESS = "success";

    public static final String STATUC_ERROR = "error";
    private String status;
    private Integer code;
    private String info;
    private T data;

    public static <T> ResponseVO<T> success(T data){
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setData(data);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setStatus(STATUC_SUCCESS);
        return responseVO;
    }

}
