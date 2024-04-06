package com.eyunpan.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eyunpan.entity.po.FileShareInfo;
import com.eyunpan.entity.qo.FileShareQO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface FileShareMapper extends IBaseMapper<FileShareInfo, FileShareQO> {
}
