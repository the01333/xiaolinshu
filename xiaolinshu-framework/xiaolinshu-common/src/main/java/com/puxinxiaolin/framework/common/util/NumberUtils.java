package com.puxinxiaolin.framework.common.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class NumberUtils {

    public static String formatNumberString(long number) {
        if (number < 10000) {
            return String.valueOf(number);
        } else if (number < 100000000) {
            double result = number / 10000.0;
            DecimalFormat df = new DecimalFormat("#.#");
            df.setRoundingMode(RoundingMode.DOWN);
            String finalShowed = df.format(result);
            return finalShowed + "万";
        } else {
            return "9999万";
        }
    }

    public static void main(String[] args) {
        System.out.println(formatNumberString(1000));         // 1000
        System.out.println(formatNumberString(11130));        // 1.1万
        System.out.println(formatNumberString(26719300));     // 2671.9万
        System.out.println(formatNumberString(10000000));    // 1000万
        System.out.println(formatNumberString(999999));       // 99.9万
        System.out.println(formatNumberString(150000000));    // 超过一亿，展示9999万
        System.out.println(formatNumberString(99999));        // 9.9万
    }

}
