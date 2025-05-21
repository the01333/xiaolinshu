package com.puxinxiaolin.xiaolinshu.auth.alarm.impl;

import com.puxinxiaolin.xiaolinshu.auth.alarm.AlarmInterface;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmsAlarmHelper implements AlarmInterface {
    @Override
    public boolean send(String message) {
        log.info("==> 【短信告警】: {}", message);

        return true;
    }
}
