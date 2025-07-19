package com.puxinxiaolin.framework.common.constant;

import java.time.format.DateTimeFormatter;

public interface DateConstants {
    
    String Y_M_D_H_M_S_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    String Y_M_FORMATER = "yyyy-MM";
    
    String Y_M_D_FORMAT = "yyyy-MM-dd";
    
    String H_M_S_FORMAT = "HH:mm:ss";

    DateTimeFormatter DATE_FORMAT_Y_M_D_H_M_S = DateTimeFormatter.ofPattern(Y_M_D_H_M_S_FORMAT);
    
}
