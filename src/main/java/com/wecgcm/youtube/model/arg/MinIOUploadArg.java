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
public class MinIOUploadArg {
    private static final String BUCKET_NAME = "videos";
    private static final String SLASH = "/";
    private static final String VIDEO_TYPE = "video/webm";

    public UploadObjectArgs build(String videoId) throws IOException {
        return UploadObjectArgs
                .builder()
                .bucket(BUCKET_NAME)
                .object(videoId + SLASH + videoId + YTDownloadArg.VIDEO_EXT)
                .filename(getFilePath(videoId))
                .contentType(VIDEO_TYPE)
                .build();
    }

    public String getFilePath(String videoId){
        return YTDownloadArg.OUT_PUT_DIR + videoId + YTDownloadArg.VIDEO_EXT;
    }

}
