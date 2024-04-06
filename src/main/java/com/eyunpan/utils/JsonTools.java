package com.eyunpan.utils;

import com.alibaba.fastjson.JSONObject;

public class JsonTools {

    public static <T> T convertJson2Obj(String json,Class<T> classz){
        return JSONObject.parseObject(json,classz);
    }

    public static <T> String convertObj2Json(T obj){
        return JSONObject.toJSONString(obj);
    }

}
