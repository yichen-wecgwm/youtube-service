package com.wecgwm.youtube.model.arg.minio;

/**
 * @author ：wecgwm
 * @date ：2023/07/23 18:02
 */
public sealed abstract class MinioArg permits MinioArchiveArg, MinioChannelArg, MinioLockArg, MinioThumbnailArg, MinioVideoInfoArg, MinioVideoArg {
    public static final String VIDEO_TYPE = "video/webm";
    public static final String IMG_TYPE = "image/png";
    static final String SLASH = "/";
    static final String VIDEO_BUCKET_NAME = "videos";
    static final String CHANNEL_BUCKET_NAME = "channel";
    static final String JSON_EXT = ".json";
    static final String VIDEO_EXT = ".webm";
    static final String THUMBNAIL_EXT = ".png";
    static final String VIDEO_INFO = "video-info";
    static final String ARCHIVE = "archive";
    static final String LOCK = "lock";
    static final String CHANNEL = "channel";
}
