package com.eyunpan.controller;

import com.eyunpan.annotation.CheckParam;
import com.eyunpan.annotation.MInterceptor;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.enums.FileDelFlagEnums;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.vo.FileInfoVO;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.entity.vo.ResponseVO;
import com.eyunpan.service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController("recycleController")
@RequestMapping("/recycle")
public class RecycleController extends IBaseController{

    @Autowired
    private FileInfoService fileInfoService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadRecycleList")
    @MInterceptor(checkParams = true)
    public ResponseVO loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize) {
        FileInfoQO query = new FileInfoQO();
        query.setPageSize(pageSize);
        query.setPageNo(pageNo);
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("recovery_time desc");
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    @RequestMapping("/recoverFile")
    @MInterceptor(checkParams = true)
    public ResponseVO recoverFile(HttpSession session, @CheckParam(required = true) String fileIds) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.recoverFileBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delFile")
    @MInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session, @CheckParam(required = true) String fileIds) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.delFileBatch(webUserDto.getUserId(), fileIds,false);
        return getSuccessResponseVO(null);
    }
}
