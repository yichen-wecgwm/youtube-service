package com.wecgcm.exception;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 16:37
 */
public class DownloadException extends RuntimeException{

    public DownloadException(String msg) {
        super(msg);
    }

    public DownloadException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
