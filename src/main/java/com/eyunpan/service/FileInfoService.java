package com.eyunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.dto.UploadResultDto;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.qo.FileInfoQO;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.mappers.FileMapper;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.search.SearchTerm;
import java.awt.*;
import java.util.List;

public interface FileInfoService extends IService<FileInfo> {
    Long getUserUseSpace(String userId);

    List<FileInfo> findListByParam(FileInfoQO fileInfoQO);

    FileInfo getFileInfoByFileIdAndUserId(String realFileId, String userId);

    PaginationResultVO findListByPage(FileInfoQO query);

    UploadResultDto uploadFile(SessionUserDto webUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks);

    Integer findCountByParam(FileInfoQO fileInfoQO);

    FileInfo rename(String fileId,String userId,String fileName);

    void changeFileFolder(String fileIds, String filePid, String userId);

    void removeFile2RecycleBatch(String userId, String fileIds);

    FileInfo newFolder(String filePid, String userId, String fileName);

    public void findAllSubFile(List<FileInfo> subFileList,FileInfo delFile,Integer flag);

    void recoverFileBatch(String userId, String fileIds);

    void delFileBatch(String userId, String fileIds, boolean admin);

    void checkRootFilePid(String fileId, String shareUserId, String filePid);

    void saveShare(String fileId, String shareFileIds, String myFolderId, String shareUserId, String userId);
}
