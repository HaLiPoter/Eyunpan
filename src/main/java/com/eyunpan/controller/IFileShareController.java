package com.eyunpan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eyunpan.annotation.CheckParam;
import com.eyunpan.annotation.MInterceptor;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.SessionShareDto;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.enums.FileDelFlagEnums;
import com.eyunpan.entity.enums.ResponseCodeEnum;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.po.FileShareInfo;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.vo.FileInfoVO;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.entity.vo.ResponseVO;
import com.eyunpan.entity.vo.ShareInfoVO;
import com.eyunpan.exception.CustomException;
import com.eyunpan.service.FileInfoService;
import com.eyunpan.service.FileShareService;
import com.eyunpan.service.UserInfoService;
import com.eyunpan.utils.CopyTools;
import com.eyunpan.utils.StringTools;
import com.eyunpan.utils.WrapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController("iFileShareController")
@RequestMapping("/showShare")
public class IFileShareController extends BaseFileController {

    @Autowired
    private FileShareService fileShareService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 获取分享登录信息
     *
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareLoginInfo")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO getShareLoginInfo(HttpSession session, @CheckParam(required = true) String shareId) {
        SessionShareDto shareSessionDto = getSessionShareFromSession(session, shareId);
        if (shareSessionDto == null) {
            return getSuccessResponseVO(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        //判断是否是当前用户分享的文件
        SessionUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null && userDto.getUserId().equals(shareSessionDto.getShareUserId())) {
            shareInfoVO.setCurrentUser(true);
        } else {
            shareInfoVO.setCurrentUser(false);
        }
        return getSuccessResponseVO(shareInfoVO);
    }

    /**
     * 根据shareId获取 shareInfoVO
     * 包括了该分享链接的绝大多数信息
     * 分享人信息，文件信息
     * @param shareId
     * @return
     */
    public ShareInfoVO getShareInfoCommon(String shareId) {
        FileShareInfo share = fileShareService.getById(shareId);
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new CustomException(ResponseCodeEnum.CODE_902.getMsg());
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(), share.getUserId());
        if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new CustomException(ResponseCodeEnum.CODE_902.getMsg());
        }
        shareInfoVO.setFileName(fileInfo.getFileName());
        UserInfo userInfo = userInfoService.getById(shareInfoVO.getUserId());
        shareInfoVO.setNickName(userInfo.getNickName());
        shareInfoVO.setUserId(userInfo.getUserId());
        return shareInfoVO;
    }

    public SessionShareDto getSessionShareFromSession(HttpSession session, String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(Constants.SESSION_SHARE_KEY + shareId);
        return sessionShareDto;
    }

    /**
     * 获取分享信息
     *
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareInfo")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO getShareInfo(@CheckParam(required = true) String shareId) {
        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    /**
     * 校验分享码
     *
     * @param session
     * @param shareId
     * @param code
     * @return
     */
    @RequestMapping("/checkShareCode")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO checkShareCode(HttpSession session,
                                     @CheckParam(required = true) String shareId,
                                     @CheckParam(required = true) String code) {
        SessionShareDto shareSessionDto = fileShareService.checkShareCode(shareId, code);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, shareSessionDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取文件列表
     *
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/loadFileList")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO loadFileList(HttpSession session,
                                   @CheckParam(required = true) String shareId, String filePid) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        FileInfoQO query = new FileInfoQO();
        if (!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)) {
            fileInfoService.checkRootFilePid(shareSessionDto.getFileId(), shareSessionDto.getShareUserId(), filePid);
            query.setFilePid(filePid);
        } else {
            query.setFileId(shareSessionDto.getFileId());
        }

        query.setUserId(shareSessionDto.getShareUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, FileInfoVO.class));
    }

    /**
     * 校验分享是否失效
     *
     * @param session
     * @param shareId
     * @return
     */
    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto shareSessionDto = getSessionShareFromSession(session, shareId);
        if (shareSessionDto == null) {
            throw new CustomException(ResponseCodeEnum.CODE_903);
        }
        if (shareSessionDto.getExpireTime() != null && new Date().after(shareSessionDto.getExpireTime())) {
            throw new CustomException(ResponseCodeEnum.CODE_902);
        }
        return shareSessionDto;
    }

    /**
     * 获取目录信息
     *
     * @param session
     * @param shareId
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO getFolderInfo(HttpSession session,
                                    @CheckParam(required = true) String shareId,
                                    @CheckParam(required = true) String path) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        return super.getFolderInfo(path, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/getFile/{shareId}/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session,
                        @PathVariable("shareId") @CheckParam(required = true) String shareId,
                        @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    public void getVideoInfo(HttpServletResponse response,
                             HttpSession session,
                             @PathVariable("shareId") @CheckParam(required = true) String shareId,
                             @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareSessionDto.getShareUserId());
    }

    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO createDownloadUrl(HttpSession session,
                                        @PathVariable("shareId") @CheckParam(required = true) String shareId,
                                        @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        return super.createDownloadUrl(fileId, shareSessionDto.getShareUserId());
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/download/{code}")
    @MInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code") @CheckParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    /**
     * 保存分享
     *
     * @param session
     * @param shareId
     * @param shareFileIds
     * @param myFolderId
     * @return
     */
    @RequestMapping("/saveShare")
    @MInterceptor(checkParams = true)
    public ResponseVO saveShare(HttpSession session,
                                @CheckParam(required = true) String shareId,
                                @CheckParam(required = true) String shareFileIds,
                                @CheckParam(required = true) String myFolderId) {
        SessionShareDto shareSessionDto = checkShare(session, shareId);
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        if (shareSessionDto.getShareUserId().equals(webUserDto.getUserId())) {
            throw new CustomException("自己分享的文件无法保存到自己的网盘");
        }
        fileInfoService.saveShare(shareSessionDto.getFileId(), shareFileIds, myFolderId, shareSessionDto.getShareUserId(), webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
