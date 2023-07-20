package com.wecgcm.youtube.exception;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 16:37
 */
public class UploadException extends RuntimeException{

    public UploadException(String msg) {
        super(msg);
    }

    public UploadException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UploadException(Throwable cause) {
        super(cause);
    }
}
