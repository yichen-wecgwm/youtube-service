package com.wecgcm.youtube.model.arg;

/**
 * @author ：wecgwm
 * @date ：2023/07/23 18:02
 */
public class MinioArg {
    public static final String VIDEO_TYPE = "video/webm";
    private static final String SLASH = "/";
    private static final String VIDEO_BUCKET_NAME = "videos";
    private static final String CHANNEL_BUCKET_NAME = "channel";
    private static final String JSON_EXT = ".json";
    private static final String VIDEO_EXT = ".webm";
    private static final String TITLE = "title";
    private static final String ARCHIVE = "archive";
    private static final String LOCK = "lock";

    public static class Video {
        public static String object(String videoId) {
            return videoId + SLASH + videoId + VIDEO_EXT;
        }

        public static String bucket() {
            return VIDEO_BUCKET_NAME;
        }
    }

    public static class Channel {
        public static String object(String channelId) {
            return channelId + MinioArg.JSON_EXT;
        }

        public static String bucket() {
            return CHANNEL_BUCKET_NAME;
        }
    }

    public static class Title {
        public static String object(String videoId) {
            return videoId + MinioArg.SLASH + TITLE;
        }

        public static String bucket() {
            return VIDEO_BUCKET_NAME;
        }
    }

    public static class Archive {
        public static String object(String videoId) {
            return ARCHIVE + MinioArg.SLASH + videoId;
        }

        public static String bucket() {
            return VIDEO_BUCKET_NAME;
        }
    }

    public static class Lock {
        public static String object(String videoId) {
            return videoId + MinioArg.SLASH + LOCK;
        }

        public static String bucket() {
            return VIDEO_BUCKET_NAME;
        }
    }

}
