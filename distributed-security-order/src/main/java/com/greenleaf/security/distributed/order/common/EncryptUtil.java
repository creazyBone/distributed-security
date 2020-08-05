package com.greenleaf.security.distributed.order.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class EncryptUtil {
    private static final Logger logger = LoggerFactory.getLogger(EncryptUtil.class);

    public static String encodeUTF8StringBase64(String str) {
        String encode = null;
        try {
            encode = Base64.getEncoder().encodeToString(str.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            logger.warn("不支持的编码格式", e);
        }
        return encode;
    }

    public static String decodeUTF8StringBase64(String str) {
        String decode = null;
        byte[] bytes = Base64.getDecoder().decode(str);
        try {
           decode = new String(bytes,"utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("不支持的编码格式", e);
        }
        return decode;
    }

    public static void main(String[] args) {
        System.out.println( EncryptUtil.encodeUTF8StringBase64("hello"));
        System.out.println( EncryptUtil.decodeUTF8StringBase64("aGVsbG8="));

    }
}
