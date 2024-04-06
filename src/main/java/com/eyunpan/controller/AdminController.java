package com.eyunpan.controller;

import com.eyunpan.annotation.CheckParam;
import com.eyunpan.annotation.MInterceptor;
import com.eyunpan.component.RedisComponent;
import com.eyunpan.entity.dto.SystemSettingDto;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.qo.UserInfoQO;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.entity.vo.ResponseVO;
import com.eyunpan.entity.vo.UserInfoVO;
import com.eyunpan.service.FileInfoService;
import com.eyunpan.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController("adminController")
@RequestMapping("/admin")
public class AdminController extends BaseFileController{

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private RedisComponent redisComponent;


    @RequestMapping("/saveSysSettings")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO saveSysSettings(
            @CheckParam(required = true) String registerEmailTitle,
            @CheckParam(required = true) String registerEmailContent,
            @CheckParam(required = true) Integer userInitUseSpace) {
        SystemSettingDto sysSettingsDto = new SystemSettingDto();
        sysSettingsDto.setRegisterEmailTitle(registerEmailTitle);
        sysSettingsDto.setRegisterEmailContent(registerEmailContent);
        sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
        redisComponent.setSysSettingsDto(sysSettingsDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/getSysSettings")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO getSysSettings() {
        return getSuccessResponseVO(redisComponent.getSysSettingsDto());
    }

    @RequestMapping("/loadUserList")
    @MInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadUserList(UserInfoQO userInfoQO){
        userInfoQO.setOrderBy("join_time desc");
        PaginationResultVO<UserInfo> page = userInfoService.getListByPage(userInfoQO);
        return getSuccessResponseVO(convert2PaginationVO(page, UserInfoVO.class));
    }

    @RequestMapping("/updateUserStatus")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO updateUserStatus(@CheckParam(required = true) String userId, @CheckParam(required = true) Integer status) {
        userInfoService.updateUserStatus(userId, status);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/updateUserSpace")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO updateUserSpace(@CheckParam(required = true) String userId, @CheckParam(required = true) Integer changeSpace) {
        userInfoService.changeUserSpace(userId, changeSpace);
        return getSuccessResponseVO(null);
    }

    /**
     * 查询所有文件
     *
     * @param query
     * @return
     */
    @RequestMapping("/loadFileList")
    @MInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadDataList(FileInfoQO query) {
        query.setOrderBy("last_update_time desc");
        query.setQueryNickName(true);
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getFolderInfo")
    @MInterceptor(checkLogin = false,checkAdmin = true, checkParams = true)
    public ResponseVO getFolderInfo(@CheckParam(required = true) String path) {
        return super.getFolderInfo(path, null);
    }

    @RequestMapping("/getFile/{userId}/{fileId}")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public void getFile(HttpServletResponse response,
                        @PathVariable("userId") @CheckParam(required = true) String userId,
                        @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        super.getFile(response, fileId, userId);
    }
    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("userId") @CheckParam(required = true) String userId,
                             @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        super.getFile(response, fileId, userId);
    }

    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO createDownloadUrl(@PathVariable("userId") @CheckParam(required = true) String userId,
                                        @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        return super.createDownloadUrl(fileId, userId);
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

    @RequestMapping("/delFile")
    @MInterceptor(checkParams = true, checkAdmin = true)
    public ResponseVO delFile(@CheckParam(required = true) String fileIdAndUserIds) {
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(itemArray[0], itemArray[1], true);
        }
        return getSuccessResponseVO(null);
    }
}
