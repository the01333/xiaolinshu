package com.puxinxiaolin.framework.common.util;

import com.puxinxiaolin.framework.common.constant.DateConstants;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    /**
     * 返回友好的相对时间字符串
     *
     * @param localDateTime
     * @return
     */
    public static String formatRelativeTime(LocalDateTime localDateTime) {
        LocalDateTime now = LocalDateTime.now();

        long daysDiff = ChronoUnit.DAYS.between(localDateTime, now);
        long hoursDiff = ChronoUnit.HOURS.between(localDateTime, now);
        long minutesDiff = ChronoUnit.MINUTES.between(localDateTime, now);
        if (daysDiff < 1) {
            if (hoursDiff < 1) {
                return minutesDiff + "分钟前";
            } else {
                return hoursDiff + "小时前";
            }
        } else if (daysDiff == 1) {
            return "昨天 " + localDateTime.format(DateConstants.H_M);
        } else if (daysDiff < 7) {
            return daysDiff + "天前";
        } else if (localDateTime.getYear() == now.getYear()) {
            return localDateTime.format(DateConstants.M_D);
        } else {
            return localDateTime.format(DateConstants.Y_M_D);
        }
    }

    /**
     * LocalDateTime -> String
     *
     * @param localDateTime
     * @return
     */
    public static String localDateTime2String(LocalDateTime localDateTime) {
        return localDateTime.format(DateConstants.Y_M_D_H_M_S);
    }

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
