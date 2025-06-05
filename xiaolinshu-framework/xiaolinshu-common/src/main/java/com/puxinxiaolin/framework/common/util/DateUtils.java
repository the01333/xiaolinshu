package com.puxinxiaolin.framework.common.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateUtils {

    /**
     * LocalDateTime -> Timestamp
     *
     * @param localDateTime
     * @return
     */
    public static long localDateTime2Timestamp(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

}
