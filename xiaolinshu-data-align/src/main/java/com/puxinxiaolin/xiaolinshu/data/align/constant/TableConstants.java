package com.puxinxiaolin.xiaolinshu.data.align.constant;

public class TableConstants {

    public static final String TABLE_NAME_SEPARATOR = "_";

    public static String buildTableNameSuffix(String data, long hashKey) {
        return data + TABLE_NAME_SEPARATOR + hashKey;
    }

}
