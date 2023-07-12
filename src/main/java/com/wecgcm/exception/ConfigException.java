package com.wecgcm.exception;

/**
 * @author ：wecgwm
 * @date ：2023/07/11 16:37
 */
public class ConfigException extends RuntimeException{

    public ConfigException(String msg) {
        super(msg);
    }

    public ConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
