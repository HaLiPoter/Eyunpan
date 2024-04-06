package com.eyunpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eyunpan.component.RedisComponent;
import com.eyunpan.config.AppConfig;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.dto.UploadResultDto;
import com.eyunpan.entity.dto.UserSpaceDto;
import com.eyunpan.entity.enums.*;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.qo.SimplePage;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.exception.CustomException;
import com.eyunpan.mappers.FileMapper;
import com.eyunpan.mappers.UserInfoMapper;
import com.eyunpan.service.FileInfoService;
import com.eyunpan.service.UserInfoService;
import com.eyunpan.utils.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileInfoServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements FileInfoService {

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Autowired
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    @Autowired
    @Lazy
    private UserInfoService userInfoService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Override
    public Long getUserUseSpace(String userId) {
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        List<FileInfo> list = list(queryWrapper);
        return  list.stream().mapToLong(new ToLongFunction<FileInfo>() {
            @Override
            public long applyAsLong(FileInfo value) {
                return value.getFileSize()==null?0: value.getFileSize();
            }
        }).sum();
    }

    @Override
    public List<FileInfo> findListByParam(FileInfoQO fileInfoQO) {
        return fileMapper.IselectList(fileInfoQO);
    }

    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String realFileId, String userId) {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId,userId);
        queryWrapper.eq(FileInfo::getFileId,realFileId);
        return getOne(queryWrapper);
    }

    @Override
    public PaginationResultVO findListByPage(FileInfoQO query) {
        Integer count = findCountByParam(query);
        int pageSize=query.getPageSize()==null?PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage simplePage = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(simplePage);
        List<FileInfo> list = findListByParam(query);
        PaginationResultVO<FileInfo> resultVO = new PaginationResultVO<>(count, pageSize, simplePage.getPageNo(), simplePage.getPageTotal(), list);
        return resultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        //临时文件夹
        File tempFileFolder=null;
        boolean success=true;
        try {
            if (StringTools.isEmpty(fileId)){
                fileId=StringTools.getRandomString(Constants.LENGTH_10);
            }
            UploadResultDto resultDto = new UploadResultDto();
            resultDto.setFileId(fileId);
            UserSpaceDto spaceDto =  redisComponent.getUserSpaceUse(webUserDto.getUserId());
            Date date = new Date();

            //接受第一个分片
            if (chunkIndex==0){
                LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
                queryWrapper.eq(FileInfo::getFileMd5,fileMd5);
                queryWrapper.eq(FileInfo::getStatus, FileStatusEnums.USING.getStatus());
                FileInfo fileInfo = getOne(queryWrapper);

                if (fileInfo!=null){
                    if (fileInfo.getFileSize()+spaceDto.getUseSpace()>spaceDto.getTotalSpace()){
                        throw new CustomException(ResponseCodeEnum.CODE_904);
                    }
                    fileInfo.setFileId(fileId);
                    fileInfo.setFilePid(filePid);
                    fileInfo.setUserId(webUserDto.getUserId());
                    fileInfo.setFileMd5(null);
                    fileInfo.setCreateTime(date);
                    fileInfo.setLastUpdateTime(date);
                    fileInfo.setStatus(FileStatusEnums.USING.getStatus());
                    fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    fileInfo.setFileMd5(fileMd5);
                    fileName=autoRename(filePid,webUserDto.getUserId(),fileName);
                    fileInfo.setFileName(fileName);
                    save(fileInfo);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    //更新用户空间，包括redis和mysql
                    updateUserSpce(webUserDto,fileInfo.getFileSize());
                    return resultDto;
                }
            }

            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String curFolderName = webUserDto.getUserId() + fileId;
            String realFolderName=tempFolderName+curFolderName;

            tempFileFolder = new File(realFolderName);
            if (!tempFileFolder.exists()){
                tempFileFolder.mkdirs();
            }
            Long fileTempSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            long sum = file.getSize() + fileTempSize + spaceDto.getUseSpace();
            if (sum > spaceDto.getTotalSpace()){
                throw new CustomException(ResponseCodeEnum.CODE_904);
            }
            File newFile = new File(realFolderName + "/" + chunkIndex);
            file.transferTo(newFile);
            redisComponent.saveFileTempSize(webUserDto.getUserId(),fileId,file.getSize());

            if (chunkIndex<chunks-1){
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return resultDto;
            }

            String month = DateUtil.format(date, DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            String realFileName = curFolderName + fileSuffix;
            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
            //存数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(webUserDto.getUserId());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(date);
            fileInfo.setLastUpdateTime(date);
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            save(fileInfo);
            Long totalSize = redisComponent.getFileTempSize(webUserDto.getUserId(), fileId);
            updateUserSpce(webUserDto,totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(),webUserDto);
                }
            });
            return resultDto;

        }catch (CustomException e){
            success = false;
            logger.error("文件上传失败", e);
            throw e;
        }catch (Exception e){
            success = false;
            logger.error("文件上传失败", e);
            throw new CustomException("文件上传失败");
        }finally {
            //如果上传失败，清除临时目录
            if (tempFileFolder != null && !success) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    logger.error("删除临时目录失败");
                }
            }
        }
    }

    @Override
    public Integer findCountByParam(FileInfoQO fileInfoQO) {
        return fileMapper.IselectCount(fileInfoQO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {

        FileInfo fileInfo = getByFileIdAndUserId(fileId, userId);
        if (fileInfo==null){
            throw new CustomException("文件不存在");
        }
        if(fileInfo.getFileName().equals(fileName))return fileInfo;
        checkFileName(fileId,userId,fileName,fileInfo.getFolderType());
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())){
            fileName+=StringTools.getFileSuffix(fileInfo.getFileName());
        }
        Date date = new Date();
        LambdaUpdateWrapper<FileInfo> updateWrapper = WrapperFactory.fileInfoUpdateWrapper();
        updateWrapper.set(FileInfo::getLastUpdateTime,date).set(FileInfo::getFileName,fileName);
        updateWrapper.eq(FileInfo::getFileId,fileId).eq(FileInfo::getUserId,userId);
        update(updateWrapper);

        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(date);
        return fileInfo;
    }

    /**
     * 1.如果文件和目标目录下的文件文件名冲突，则将文件自动重命名
     * 2.修改移动的文件的pid为目标文件夹
     * @param fileIds
     * @param filePid
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, String userId) {

       String[] fileIdArrays= fileIds.split(",");

       //查找目标文件夹下的文件 tarFileList
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getFilePid,filePid).eq(FileInfo::getUserId,userId);
        List<FileInfo> targetFileList = list(queryWrapper);

        Map<String, FileInfo> targetFileNameMap = targetFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        //选中的文件/文件夹 selectList
        queryWrapper.clear();
        queryWrapper.eq(FileInfo::getUserId,userId);
        queryWrapper.in(FileInfo::getFileId, Arrays.asList(fileIdArrays));
        List<FileInfo> selectList = list(queryWrapper);

        for (FileInfo fileInfo:selectList){
            FileInfo targetFile = targetFileNameMap.get(fileInfo.getFileName());
            FileInfo updateInfo = new FileInfo();
            if (targetFile!=null){
                updateInfo.setFileName(StringTools.rename(fileInfo.getFileName()));
            }
            updateInfo.setFilePid(filePid);
            LambdaUpdateWrapper<FileInfo> updateWrapper = WrapperFactory.fileInfoUpdateWrapper();
            updateWrapper.eq(FileInfo::getFileId,fileInfo.getFileId()).eq(FileInfo::getUserId,fileInfo.getUserId());
            update(updateInfo,updateWrapper);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        Date date = new Date();
        String[] fileArray = fileIds.split(",");
        List<String> delFileIdList = Arrays.asList(fileArray);
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.in(FileInfo::getFileId,delFileIdList);
        List<FileInfo> delFileList = list(queryWrapper);

        ArrayList<FileInfo> subFileList = new ArrayList<>();
        for (FileInfo delFile:delFileList){
            findAllSubFile(subFileList,delFile,FileDelFlagEnums.USING.getFlag());
        }
        if (!subFileList.isEmpty()){
            subFileList.forEach(fileInfo -> {
                fileInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            });
            updateBatchById(subFileList);
        }

        for (FileInfo fileInfo : delFileList) {
            fileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
            fileInfo.setRecoveryTime(date);
        }
        updateBatchById(delFileList);
    }

    public void findAllSubFile(List<FileInfo> subFileList,FileInfo delFile,Integer flag){
        if (delFile.getFolderType()==FileFolderTypeEnums.FILE.getType()){
            return;
        }
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getFilePid,delFile.getFileId())
                .eq(FileInfo::getUserId,delFile.getUserId())
                .eq(FileInfo::getDelFlag,flag);
        List<FileInfo> list = list(queryWrapper);
        for (FileInfo fileInfo:list){
            subFileList.add(fileInfo);
            findAllSubFile(subFileList,fileInfo,flag);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray=fileIds.split(",");
        List<String> fileIdList = Arrays.asList(fileIdArray);
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getDelFlag,FileDelFlagEnums.RECYCLE.getFlag());
        queryWrapper.in(FileInfo::getFileId,fileIdList);
        List<FileInfo> recycleList = list(queryWrapper);

        //根目录下所有文件 rootFileList
        queryWrapper.clear();
        queryWrapper.eq(FileInfo::getFilePid,Constants.ZERO_STR)
                .eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getDelFlag,FileDelFlagEnums.USING.getFlag());
        List<FileInfo> rootFileList = list(queryWrapper);
        Map<String, FileInfo> rootFileMap = rootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));

        ArrayList<FileInfo> subFileList = new ArrayList<>();
        for (FileInfo recycleFile:recycleList){
            findAllSubFile(subFileList,recycleFile,FileDelFlagEnums.DEL.getFlag());
        }
        Date date = new Date();
        for (FileInfo fileInfo:subFileList){
            fileInfo.setLastUpdateTime(date);
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        }
        updateBatchById(subFileList);

        for (FileInfo fileInfo:recycleList){
            FileInfo rootFile = rootFileMap.get(fileInfo.getFileName());
            if (rootFile!=null){
                String newName=StringTools.rename(fileInfo.getFileName());
                fileInfo.setFileName(newName);
            }
        }

        for (FileInfo fileInfo:recycleList){
            fileInfo.setFilePid(Constants.ZERO_STR);
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            fileInfo.setLastUpdateTime(date);
        }

        updateBatchById(recycleList);
    }

    @Override
    public void delFileBatch(String userId, String fileIds, boolean admin) {
        List<String> fileIdList=Arrays.asList(fileIds.split(","));
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getUserId,userId)
                .in(FileInfo::getFileId,fileIdList);
        if (!admin){
            queryWrapper.eq(FileInfo::getDelFlag,FileDelFlagEnums.RECYCLE.getFlag());
        }
        List<FileInfo> delFileList = list(queryWrapper);
        ArrayList<FileInfo> subFileList = new ArrayList<>();

        for (FileInfo delFile:delFileList){
            findAllSubFile(subFileList,delFile,FileDelFlagEnums.DEL.getFlag());
        }

        List<String> subFileIdList = subFileList.stream().map(FileInfo::getFileId).collect(Collectors.toList());
        removeByIds(subFileIdList);
        removeByIds(fileIdList);

        //重新计算用户空间
        Long useSpace = this.fileMapper.selectUseSpace(userId);
        LambdaUpdateWrapper<UserInfo> updateWrapper = WrapperFactory.userInfoUpdateWrapper();
        updateWrapper.set(UserInfo::getUseSpace,useSpace);
        updateWrapper.eq(UserInfo::getUserId,userId);
        userInfoService.update(updateWrapper);

        //设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceDto);

    }
    @Override
    public void checkRootFilePid(String rootFilePid, String userId, String fileId) {
        if (StringTools.isEmpty(fileId)) {
            throw new CustomException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(fileId)) {
            return;
        }
        checkFilePid(rootFilePid, fileId, userId);
    }

    @Override
    public void saveShare(String fileId, String shareFileIds, String myFolderId, String shareUserId, String userId) {
        List<String> shareFileIdList=Arrays.asList(shareFileIds.split(","));
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getFilePid,myFolderId)
                .eq(FileInfo::getUserId,userId);
        List<FileInfo> curFileList = list(queryWrapper);
        Map<String, FileInfo> curFileMap = curFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        queryWrapper.clear();
        queryWrapper.in(FileInfo::getFileId,shareFileIdList);
        queryWrapper.eq(FileInfo::getUserId,shareUserId);
        List<FileInfo> shareFileList = list(queryWrapper);

        ArrayList<FileInfo> saveList = new ArrayList<>();
        Date date = new Date();
        for (FileInfo fileInfo:shareFileList){
            FileInfo info = curFileMap.get(fileInfo.getFileName());
            if (info!=null){
                fileInfo.setFileName(StringTools.rename(fileInfo.getFileName()));
            }
            saveAllSubFile(saveList,fileInfo,shareUserId,userId,date,myFolderId);
        }
        saveBatch(saveList);
    }

    private void saveAllSubFile(ArrayList<FileInfo> saveList, FileInfo fileInfo, String shareUserId, String userId, Date date, String myFolderId) {
        String sourceFileId = fileInfo.getFileId();
        fileInfo.setCreateTime(date);
        fileInfo.setLastUpdateTime(date);
        fileInfo.setFilePid(myFolderId);
        fileInfo.setUserId(userId);
        String newFileId = StringTools.getRandomString(Constants.LENGTH_10);
        fileInfo.setFileId(newFileId);
        saveList.add(fileInfo);
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
            LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
            queryWrapper.eq(FileInfo::getFilePid,sourceFileId)
                    .eq(FileInfo::getUserId,shareUserId);
            List<FileInfo> sourceList = list(queryWrapper);
            for (FileInfo info:sourceList){
                saveAllSubFile(saveList,info,shareUserId,userId,date,newFileId);
            }
        }
    }

    private void checkFilePid(String rootFilePid, String fileId, String userId) {
        FileInfo fileInfo = getFileInfoByFileIdAndUserId(fileId,userId);
        if (fileInfo == null) {
            throw new CustomException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STR.equals(fileInfo.getFilePid())) {
            throw new CustomException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(), userId);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        Date date = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(date);
        fileInfo.setLastUpdateTime(date);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        save(fileInfo);
        return fileInfo;
    }

    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getFolderType,folderType);
        queryWrapper.eq(FileInfo::getFileName,fileName);
        queryWrapper.eq(FileInfo::getFilePid,filePid);
        queryWrapper.eq(FileInfo::getUserId,userId);
        queryWrapper.eq(FileInfo::getDelFlag,FileDelFlagEnums.USING.getFlag());
        long count = count(queryWrapper);
        if (count > 0) {
            throw new CustomException("此目录下已存在同名文件，请修改名称");
        }
    }

    @Async
    public void transferFile(String fileId,SessionUserDto webUserDto){
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;

        FileInfo fileInfo = getByFileIdAndUserId(fileId, webUserDto.getUserId());
        try{
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            String tempFolderName=appConfig.getProjectFolder()+Constants.FILE_FOLDER_TEMP;
            String curFolderName=webUserDto.getUserId()+fileId;
            String realFolderName=tempFolderName+curFolderName;
            //该文件所在临时文件夹
            File fileFolder=new File(realFolderName);
            if (!fileFolder.exists()){
                fileFolder.mkdirs();
            }
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            String targetFolderName=appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName +"/"+ month);
            if (!targetFolder.exists()){
                targetFolder.mkdirs();
            }

            String realFileName=curFolderName+fileSuffix;
            targetFilePath=targetFolder.getPath()+"/"+realFileName;
            union(fileFolder.getPath(),targetFilePath,fileInfo.getFileName(),true);
            fileTypeEnum=FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (fileTypeEnum== FileTypeEnums.VIDEO){
                cutFile4Video(fileId, targetFilePath);
                cover=month+"/"+curFolderName+Constants.IMAGE_PNG_SUFFIX;
                String coverPath=targetFolderName+"/"+cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            }else if (fileTypeEnum==FileTypeEnums.IMAGE){
                //生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        }catch (Exception e){
            logger.error("文件转码失败，文件Id:{},userId:{}", fileId, webUserDto.getUserId(), e);
            transferSuccess = false;
        }finally {
            LambdaUpdateWrapper<FileInfo> updateWrapper = WrapperFactory.fileInfoUpdateWrapper();
            updateWrapper.set(FileInfo::getFileCover,cover);
            updateWrapper.set(FileInfo::getFileSize,new File(targetFilePath).length());
            updateWrapper.set(FileInfo::getStatus,transferSuccess?FileStatusEnums.USING.getStatus():FileStatusEnums.TRANSFER_FAIL.getStatus());
            updateWrapper.eq(FileInfo::getFileId,fileId);
            updateWrapper.eq(FileInfo::getUserId,webUserDto.getUserId());
            updateWrapper.eq(FileInfo::getStatus,FileStatusEnums.TRANSFER.getStatus());
            update(updateWrapper);
        }
    }

    public void cutFile4Video(String fileId,String targetFilePath){
        //创建同名切片目录
        File tsFolder = new File(targetFilePath.substring(0, targetFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        //先生成ts
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, targetFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8 和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }
    public static void union(String dirPath, String toFilePath, String fileName, boolean delSource) throws CustomException{
        File dir = new File(dirPath);
        if (!dir.exists()){
            throw new CustomException("目录错误");
        }
        File[] files = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile=null;
        try {
            writeFile=new RandomAccessFile(targetFile,"rw");
            byte[] bytes = new byte[10 * 1024];
            for (int i=0;i<files.length;i++){
                int len=-1;
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile=null;
                try {
                    readFile=new RandomAccessFile(chunkFile,"r");
                    while ((len=readFile.read(bytes))!=-1){
                        writeFile.write(bytes,0,len);
                    }
                }catch (Exception e){
                    logger.error("分片合并失败",e);
                    throw new CustomException("文件合并失败");
                }finally {
                    readFile.close();
                }
            }
        }catch (Exception e){
            logger.error("合并文件:{}失败", fileName, e);
            throw new CustomException("合并文件" + fileName + "出错了");
        }finally {

            try {
                if (writeFile!=null){
                    writeFile.close();
                }
            } catch (IOException e) {
                logger.error("关闭流失败");
            }
            if (delSource){
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public FileInfo getByFileIdAndUserId(String fileId,String userId){
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getFileId,fileId);
        queryWrapper.eq(FileInfo::getUserId,userId);
        return getOne(queryWrapper);
    }
    private void updateUserSpce(SessionUserDto webUserDto, Long fileSize) {
        LambdaUpdateWrapper<UserInfo> updateWrapper = WrapperFactory.userInfoUpdateWrapper();
        updateWrapper.eq(UserInfo::getUserId,webUserDto.getUserId());
        updateWrapper.setSql(fileSize!=null,"use_space=use_space+"+fileSize);
        userInfoService.update(updateWrapper);
        UserSpaceDto spaceDto = (UserSpaceDto) redisComponent.getUserSpaceUse(webUserDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace()+fileSize);
        redisComponent.saveUserSpaceUse(webUserDto.getUserId(), spaceDto);

    }

    private String autoRename(String filePid, String userId, String fileName) {
        LambdaQueryWrapper<FileInfo> queryWrapper = WrapperFactory.fileInfoQueryWrapper();
        queryWrapper.eq(FileInfo::getUserId,userId);
        queryWrapper.eq(FileInfo::getFilePid,filePid);
        queryWrapper.eq(FileInfo::getDelFlag,FileDelFlagEnums.USING.getFlag());
        queryWrapper.eq(FileInfo::getFileName,fileName);
        FileInfo one = getOne(queryWrapper);
        if (one!=null)return StringTools.rename(fileName);
        return fileName;
    }


}
