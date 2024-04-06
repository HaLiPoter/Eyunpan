package com.eyunpan.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.vo.PaginationResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface FileMapper extends IBaseMapper<FileInfo, FileInfoQO> {


    Long selectUseSpace(@Param("userId") String userId);

}
