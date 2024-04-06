package com.eyunpan.utils;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.po.FileShareInfo;
import com.eyunpan.entity.po.UserInfo;

public class WrapperFactory {

    public static LambdaQueryWrapper<UserInfo> userInfoQueryWrapper(){
        return new LambdaQueryWrapper<UserInfo>();
    }
    public static LambdaQueryWrapper<FileInfo> fileInfoQueryWrapper(){
        return new LambdaQueryWrapper<FileInfo>();
    }
    public static LambdaQueryWrapper<FileShareInfo> fileShareInfoQueryWrapper(){
        return new LambdaQueryWrapper<FileShareInfo>();
    }

    public static LambdaUpdateWrapper<UserInfo> userInfoUpdateWrapper(){
        return new LambdaUpdateWrapper<UserInfo>();
    }
    public static LambdaUpdateWrapper<FileInfo> fileInfoUpdateWrapper(){
        return new LambdaUpdateWrapper<FileInfo>();
    }
    public static LambdaUpdateWrapper<FileShareInfo> fileShareInfoUpdateWrapper(){
        return new LambdaUpdateWrapper<FileShareInfo>();
    }
}
