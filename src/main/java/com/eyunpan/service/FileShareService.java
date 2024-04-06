package com.eyunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eyunpan.entity.dto.SessionShareDto;
import com.eyunpan.entity.po.FileShareInfo;
import com.eyunpan.entity.qo.FileShareQO;
import com.eyunpan.entity.vo.PaginationResultVO;

import java.util.List;

public interface FileShareService extends IService<FileShareInfo> {
    PaginationResultVO findListByPage(FileShareQO fileShareQO);

    List<FileShareInfo> findListByParam(FileShareQO param);

    void deleteFileShareBatch(String[] split, String userId);

    SessionShareDto checkShareCode(String shareId, String code);

    void saveShareFile(FileShareInfo share);
}
