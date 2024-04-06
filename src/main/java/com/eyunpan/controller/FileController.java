package com.eyunpan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eyunpan.annotation.CheckParam;
import com.eyunpan.annotation.MInterceptor;
import com.eyunpan.component.RedisComponent;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.dto.UploadResultDto;
import com.eyunpan.entity.enums.FileCategoryEnums;
import com.eyunpan.entity.enums.FileDelFlagEnums;
import com.eyunpan.entity.enums.FileFolderTypeEnums;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.qo.SimplePage;
import com.eyunpan.entity.vo.FileInfoVO;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.entity.vo.ResponseVO;
import com.eyunpan.tuple.ITuple2;
import com.eyunpan.utils.CopyTools;
import com.eyunpan.utils.StringTools;
import com.eyunpan.utils.WrapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

@RestController("fileController")
@RequestMapping("/file")
public class FileController extends BaseFileController{

    @RequestMapping("/loadDataList")
    @MInterceptor(checkParams = true)
    public ResponseVO loadDataList(HttpSession session, FileInfoQO query, String category) {
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 上传文件
     * 前端做好分片和计算md5
     * @param session
     * @param fileId 文件ID
     * @param file 文件分片
     * @param fileName  文件名
     * @param filePid   文件的父级目录
     * @param fileMd5   文件的md5，前端先计算好了的
     * @param chunkIndex    分片的id
     * @param chunks    总共的分片数
     * @return
     */
    @RequestMapping("/uploadFile")
    @MInterceptor(checkParams = true)
    public ResponseVO uploadFile(HttpSession session,
                                 String fileId,
                                 MultipartFile file,
                                 @CheckParam(required = true) String fileName,
                                 @CheckParam(required = true) String filePid,
                                 @CheckParam(required = true) String fileMd5,
                                 @CheckParam(required = true) Integer chunkIndex,
                                 @CheckParam(required = true) Integer chunks) {

        SessionUserDto webUserDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return getSuccessResponseVO(resultDto);
    }

    /**
     * 加载图片
     * @param response
     * @param imageFolder
     * @param imageName
     */
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }

    /**
     * 预览视频
     * @param response
     * @param session
     * @param fileId
     */
    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 预览其他文件类型
     * @param response
     * @param session
     * @param fileId
     */
    @RequestMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }


    @RequestMapping("/getFolderInfo")
    @MInterceptor(checkParams = true)
    public ResponseVO getFolderInfo(HttpSession session, @CheckParam(required = true) String path) {
        return super.getFolderInfo(path, getUserInfoFromSession(session).getUserId());
    }

    @RequestMapping("/newFoloder")
    @MInterceptor(checkParams = true)
    public ResponseVO newFoloder(HttpSession session,
                                 @CheckParam(required = true) String filePid,
                                 @CheckParam(required = true) String fileName) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(fileInfo);
    }

    @RequestMapping("/rename")
    @MInterceptor(checkParams = true)
    public ResponseVO rename(HttpSession session,
                             @CheckParam(required = true) String fileId,
                             @CheckParam(required = true) String fileName) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    /**
     * 移动文件时，显示的所有文件夹，要排除当前文件ID
     * 前端每点方框中的一个文件夹就会请求一次
     * @param session
     * @param filePid
     * @param currentFileIds
     * @return
     */
    @RequestMapping("/loadAllFolder")
    @MInterceptor(checkParams = true)
    public ResponseVO loadAllFolder(HttpSession session, @CheckParam(required = true) String filePid, String currentFileIds) {
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getUserId,getUserInfoFromSession(session).getUserId())
                .eq(FileInfo::getFilePid,filePid)
                .eq(FileInfo::getFolderType,FileFolderTypeEnums.FOLDER.getType())
                .eq(FileInfo::getDelFlag,FileDelFlagEnums.USING.getFlag())
                .orderByDesc(FileInfo::getCreateTime);
        if (!StringTools.isEmpty(currentFileIds)){
            queryWrapper.notIn(FileInfo::getFileId, Arrays.asList(currentFileIds.split(",")));
        }
        List<FileInfo> fileInfoList = fileInfoService.list(queryWrapper);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FileInfoVO.class));
    }

    /**
     * 移动文件
     */
    @RequestMapping("/changeFileFolder")
    @MInterceptor(checkParams = true)
    public ResponseVO changeFileFolder(HttpSession session,
                                       @CheckParam(required = true) String fileIds,
                                       @CheckParam(required = true) String filePid) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/createDownloadUrl/{fileId}")
    @MInterceptor(checkParams = true)
    public ResponseVO createDownloadUrl(HttpSession session, @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        return super.createDownloadUrl(fileId, getUserInfoFromSession(session).getUserId());
    }

    @RequestMapping("/download/{code}")
    @MInterceptor(checkLogin = false, checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response, @PathVariable("code") @CheckParam(required = true) String code) throws Exception {
        super.download(request, response, code);
    }

    @RequestMapping("/delFile")
    @MInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session, @CheckParam(required = true) String fileIds) {
        SessionUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    @Autowired
    private RedisComponent redisComponent;
    @RequestMapping("/rank")
    public ResponseVO rank(HttpSession session,FileInfoQO query){
        PaginationResultVO rankByPage = fileInfoService.findRankByPage(query);
        return getSuccessResponseVO(rankByPage);
    }
}
