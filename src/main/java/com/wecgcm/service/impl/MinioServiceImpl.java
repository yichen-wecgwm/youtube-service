package com.wecgcm.service.impl;

import cn.hutool.core.text.StrPool;
import com.wecgcm.exception.UploadException;
import com.wecgcm.service.MinioService;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.wecgcm.util.YoutubeFileUtil.OUT_PUT_DIR;
import static com.wecgcm.util.YoutubeFileUtil.VIDEO_EXT;

/**
 * @author ：wecgwm
 * @date ：2023/07/16 19:32
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class MinioServiceImpl implements MinioService {
    private final MinioClient minioClient;

    @Override
    public void upload(String videoId) {
        try {
            ObjectWriteResponse objectWriteResponse = minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(OUT_PUT_DIR)
                            .object(videoId + StrPool.SLASH + videoId + VIDEO_EXT)
                            .filename(OUT_PUT_DIR + StrPool.SLASH + videoId + VIDEO_EXT)
                            .build());
            if (objectWriteResponse == null) {
                throw new UploadException("minio upload fail, resp is null");
            }
        } catch (ErrorResponseException | InsufficientDataException | XmlParserException | ServerException |
                 NoSuchAlgorithmException | IOException | InvalidResponseException | InvalidKeyException |
                 InternalException e) {
            throw new RuntimeException(e);
        }
    }

}
