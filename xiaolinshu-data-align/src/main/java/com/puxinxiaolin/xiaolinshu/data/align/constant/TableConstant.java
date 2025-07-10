package com.puxinxiaolin.xiaolinshu.data.align.constant;

public class TableConstant {

    public static final String TABLE_NAME_SEPARATOR = "_";

    public static String buildTableNameSuffix(String data, int hashKey) {
        return data + TABLE_NAME_SEPARATOR + hashKey;
    }

}
