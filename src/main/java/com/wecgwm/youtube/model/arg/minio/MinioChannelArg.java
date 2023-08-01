package com.wecgwm.youtube.model.arg.minio;

/**
 * @author ：wecgwm
 * @date ：2023/07/31 23:45
 */
public final class MinioChannelArg extends MinioArg {

    public static String object() {
        return CHANNEL + JSON_EXT;
    }

    public static String bucket() {
        return CHANNEL_BUCKET_NAME;
    }

}
