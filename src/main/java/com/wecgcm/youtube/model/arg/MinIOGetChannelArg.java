package com.wecgcm.youtube.model.arg;

import io.minio.GetObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ：wecgwm
 * @date ：2023/07/18 23:28
 */
@Slf4j
@Component
public class MinIOGetChannelArg extends MinioArg{

    public GetObjectArgs build(int channelId) {
        return GetObjectArgs
                .builder()
                .bucket(CHANNEL_BUCKET_NAME)
                .object(channelId + JSON)
                .build();
    }

}
