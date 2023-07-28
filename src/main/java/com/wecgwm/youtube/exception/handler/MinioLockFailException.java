package com.wecgwm.youtube.exception.handler;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 16:37
 */
public class MinioLockFailException extends RuntimeException{

    public MinioLockFailException(String msg) {
        super(msg);
    }

    public MinioLockFailException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public MinioLockFailException(Throwable cause) {
        super(cause);
    }
}
