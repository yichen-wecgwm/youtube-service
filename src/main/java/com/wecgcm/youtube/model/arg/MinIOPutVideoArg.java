package com.wecgcm.youtube.model.arg;

import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@Slf4j
@Component
public class MinIOPutVideoArg extends MinioArg{
    public PutObjectArgs build(String videoId, InputStream inputStream) throws IOException {
        return PutObjectArgs
                .builder()
                .bucket(VIDEO_BUCKET_NAME)
                .object(videoId + SLASH + TITLE)
                .stream(inputStream, -1, 10485760)
                .build();
    }

}
