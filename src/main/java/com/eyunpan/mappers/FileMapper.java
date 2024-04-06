package com.eyunpan.mappers;

import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMapper extends IBaseMapper<FileInfo, FileInfoQO> {


    Long selectUseSpace(@Param("userId") String userId);

}
