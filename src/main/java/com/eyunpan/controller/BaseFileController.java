package com.eyunpan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eyunpan.component.RedisComponent;
import com.eyunpan.config.AppConfig;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.DownloadFileDto;
import com.eyunpan.entity.enums.FileCategoryEnums;
import com.eyunpan.entity.enums.FileFolderTypeEnums;
import com.eyunpan.entity.enums.ResponseCodeEnum;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.vo.FileCntDto;
import com.eyunpan.entity.vo.FolderVO;
import com.eyunpan.entity.vo.ResponseVO;
import com.eyunpan.exception.CustomException;
import com.eyunpan.service.FileInfoService;
import com.eyunpan.utils.CopyTools;
import com.eyunpan.utils.StringTools;
import com.eyunpan.utils.WrapperFactory;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;

public class BaseFileController extends IBaseController {

    @Resource
    protected FileInfoService fileInfoService;

    @Resource
    protected AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 获取当前目录，获取目录路径的显示
     *
     * @param path
     * @param userId
     * @return
     */
    public ResponseVO getFolderInfo(String path, String userId){

        String[] pathArray = path.split("/");
        FileInfoQO fileInfoQO = new FileInfoQO();
        fileInfoQO.setUserId(userId);
        fileInfoQO.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfoQO.setFileIdArray(pathArray);
        String orderBy="field(file_id,\"" + StringUtils.join(pathArray, "\",\"") + "\")";
        fileInfoQO.setOrderBy(orderBy);
        List<FileInfo> files = fileInfoService.findListByParam(fileInfoQO);
        return getSuccessResponseVO(CopyTools.copyList(files, FolderVO.class));
    }

    /**
     * 前端读取图片
     * @param response
     * @param imageFolder
     * @param imageName
     */
    public void getImage(HttpServletResponse response, String imageFolder, String imageName) {
        if (StringTools.isEmpty(imageFolder) || StringUtils.isBlank(imageName)) {
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, filePath);
    }

    /**
     *  读取文件
     * @param response
     * @param fileId
     * @param userId
     */
    protected void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        //如果读取的是.ts文件，ts文件时视频分片
        if (fileId.endsWith(".ts")) {
            //分割
            String[] tsAarray = fileId.split("_");
            String realFileId = tsAarray[0];
            //根据原文件的id查询出一个文件集合
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId, userId);
            if (fileInfo == null) {
                //分享的视频，ts路径记录的是原视频的id,这里通过id直接取出原视频
                FileInfoQO fileInfoQuery = new FileInfoQO();
                fileInfoQuery.setFileId(realFileId);
                List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
                fileInfo = fileInfoList.get(0);
                if (fileInfo == null) {
                    return;
                }

                //更具当前用户id和路径去查询当前用户是否有该文件，如果没有直接返回
                queryWrapper.clear();
                queryWrapper.eq(FileInfo::getFilePath,fileInfo.getFilePath());
                queryWrapper.eq(FileInfo::getUserId,userId);
                long count = fileInfoService.count(queryWrapper);
                if (count == 0) {
                    return;
                }
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;

            //ts文件路径
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        } else {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
            if (fileInfo == null) {
                return;
            }
            //视频文件读取.m3u8文件，如果读的是视频文件，重新设置文件路径定义到m3u8
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                //重新设置文件路径
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {//否则直接读
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        //读取字节流
        readFile(response, filePath);
    }

    protected ResponseVO createDownloadUrl(String fileId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new CustomException(ResponseCodeEnum.CODE_600);
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new CustomException(ResponseCodeEnum.CODE_600);
        }
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        DownloadFileDto downloadFileDto = new DownloadFileDto();
        downloadFileDto.setDownloadCode(code);
        downloadFileDto.setFilePath(fileInfo.getFilePath());
        downloadFileDto.setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code, downloadFileDto);

        //下载排行逻辑
        redisComponent.addFileDownloadCnt(fileInfo.getFileMd5());
        return getSuccessResponseVO(code);
    }

    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if (null == downloadFileDto) {
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }
}
