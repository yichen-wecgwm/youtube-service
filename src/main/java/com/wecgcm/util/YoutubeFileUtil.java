package com.wecgcm.util;

import cn.hutool.core.text.StrPool;
import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.File;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 19:28
 */
public class YoutubeFileUtil {
    public static final String OUT_PUT_DIR = "videos";
    public static final String VIDEO_EXT = ".mp4";

    public static String videoFormatName(File file){
        return videoFormatName(getVideoID(file));
    }
    public static String videoFormatName(String videoId){
        return OUT_PUT_DIR + StrPool.SLASH + videoId + StrPool.DASHED + Format.VIDEO;
    }

    public static String audioFormatName(File file){
        return audioFormatName(getVideoID(file));
    }

    public static String audioFormatName(String videoId){
        return OUT_PUT_DIR + StrPool.SLASH + videoId + StrPool.DASHED + Format.AUDIO;
    }

    public static String getVideoID(File file) {
        return file.getName().split(StrPool.DASHED)[0];
    }

}
