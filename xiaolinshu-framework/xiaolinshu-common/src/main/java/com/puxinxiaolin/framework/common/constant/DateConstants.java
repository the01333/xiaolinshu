package com.puxinxiaolin.framework.common.constant;

import java.time.format.DateTimeFormatter;

public interface DateConstants {

    DateTimeFormatter H_M = DateTimeFormatter.ofPattern("HH:mm");
    
    DateTimeFormatter M_D = DateTimeFormatter.ofPattern("MM-dd");
    
    DateTimeFormatter Y_M_D_H_M_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    DateTimeFormatter Y_M = DateTimeFormatter.ofPattern("yyyy-MM");

    DateTimeFormatter Y_M_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    DateTimeFormatter H_M_S = DateTimeFormatter.ofPattern("HH:mm:ss");
    
}
