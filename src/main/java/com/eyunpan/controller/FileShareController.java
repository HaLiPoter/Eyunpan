package com.eyunpan.controller;

import com.eyunpan.annotation.CheckParam;
import com.eyunpan.annotation.MInterceptor;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.po.FileShareInfo;
import com.eyunpan.entity.qo.FileShareQO;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.entity.vo.ResponseVO;
import com.eyunpan.service.FileShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController("fileShareController")
@RequestMapping("/share")
public class FileShareController extends IBaseController{

    @Autowired
    private FileShareService fileShareService;

    @RequestMapping("/loadShareList")
    @MInterceptor(checkParams = true)
    public ResponseVO loadShareList(HttpSession session, FileShareQO fileShareQO){

        fileShareQO.setOrderBy("share_time desc");
        SessionUserDto userDto = getUserInfoFromSession(session);
        fileShareQO.setUserId(userDto.getUserId());
        fileShareQO.setQueryFileName(true);
        PaginationResultVO resultVO = this.fileShareService.findListByPage(fileShareQO);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/shareFile")
    @MInterceptor(checkParams = true)
    public ResponseVO shareFile(HttpSession session,
                                @CheckParam(required = true) String fileId,
                                @CheckParam(required = true) Integer validType,
                                String code) {
        SessionUserDto userDto = getUserInfoFromSession(session);
        FileShareInfo share = new FileShareInfo();
        share.setFileId(fileId);
        share.setValidType(validType);
        share.setCode(code);
        share.setUserId(userDto.getUserId());
        fileShareService.save(share);
        return getSuccessResponseVO(share);
    }

    @RequestMapping("/cancelShare")
    @MInterceptor(checkParams = true)
    public ResponseVO cancelShare(HttpSession session, @CheckParam(required = true) String shareIds) {
        SessionUserDto userDto = getUserInfoFromSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), userDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
