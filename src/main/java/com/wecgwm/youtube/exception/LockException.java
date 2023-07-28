package com.wecgwm.youtube.exception;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 16:37
 */
public class LockException extends RuntimeException{
    public LockException(String msg) {
        super(msg);
    }

    public LockException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public LockException(Throwable cause) {
        super(cause);
    }
}
