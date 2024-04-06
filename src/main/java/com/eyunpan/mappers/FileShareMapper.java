package com.eyunpan.mappers;

import com.eyunpan.entity.po.FileShareInfo;
import com.eyunpan.entity.qo.FileShareQO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileShareMapper extends IBaseMapper<FileShareInfo, FileShareQO> {
}
