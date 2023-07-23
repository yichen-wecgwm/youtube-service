package com.wecgcm.youtube.exception;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 16:37
 */
public class YTDLPException extends RuntimeException{

    public YTDLPException(String msg) {
        super(msg);
    }

    public YTDLPException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public YTDLPException(Throwable cause) {
        super(cause);
    }
}
