package com.wecgcm.youtube.model.arg;

import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@Slf4j
@Component
public class MinIOUploadVideoArg extends MinioArg{

    public UploadObjectArgs build(String videoId) throws IOException {
        return UploadObjectArgs
                .builder()
                .bucket(VIDEO_BUCKET_NAME)
                .object(videoId + SLASH + videoId + YTDLPArg.VIDEO_EXT)
                .filename(filePath(videoId))
                .contentType(VIDEO_TYPE)
                .build();
    }

    public String filePath(String videoId){
        return YTDLPArg.OUT_PUT_DIR + videoId + YTDLPArg.VIDEO_EXT;
    }

}
