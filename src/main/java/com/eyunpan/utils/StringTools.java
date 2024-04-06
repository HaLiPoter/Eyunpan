package com.eyunpan.utils;

import com.eyunpan.entity.constants.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class StringTools {

    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }
    public static String encodeByMD5(String str){
        return StringTools.isEmpty(str)?null: DigestUtils.md5Hex(str);
    }

    public static boolean isEmpty(String str){
        if (str==null||"".equals(str)||str.equals("null")){
            return true;
        }else if("".equals(str.trim())){
            return true;
        }
        return false;
    }

    public static String getFileNameNoSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return fileName.substring(index);
    }

    public static String rename(String name){
        String fileNameNoSuffix = getFileNameNoSuffix(name);
        String fileSuffix = getFileSuffix(name);
        return fileNameNoSuffix+"_"+getRandomString(Constants.LENGTH_5)+fileSuffix;
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }
}
