package com.puxinxiaolin.framework.common.util;

import java.util.regex.Pattern;

public class ParamUtils {

    private ParamUtils() {
    }

    // ============================== 校验昵称 ==============================

    private static final int NICK_NAME_MIN_LENGTH = 2;
    private static final int NICK_NAME_MAX_LENGTH = 24;

    private static final String NICK_NAME_REGEX = "[!@#$%^&*(),.?\\\":{}|<>]";


    public static boolean checkNickName(String nickName) {
        if (nickName.length() < NICK_NAME_MIN_LENGTH || nickName.length() > NICK_NAME_MAX_LENGTH) {
            return false;
        }

        Pattern pattern = Pattern.compile(NICK_NAME_REGEX);
        return !pattern.matcher(nickName).find();
    }


    // ============================== 校验小林书号 ==============================

    private static final int ID_MIN_LENGTH = 6;
    private static final int ID_MAX_LENGTH = 15;

    private static final String ID_REGEX = "^[a-zA-Z0-9_]+$";

    /**
     * 小哈书 ID 校验
     *
     * @param xiaolinshuId
     * @return
     */
    public static boolean checkXiaolinshuId(String xiaolinshuId) {
        if (xiaolinshuId.length() < ID_MIN_LENGTH || xiaolinshuId.length() > ID_MAX_LENGTH) {
            return false;
        }

        Pattern pattern = Pattern.compile(ID_REGEX);
        return pattern.matcher(xiaolinshuId).matches();
    }

    /**
     * 字符串长度校验
     *
     * @param str
     * @param length
     * @return
     */
    public static boolean checkLength(String str, int length) {
        return !str.isEmpty() && str.length() <= length;
    }
    
}
