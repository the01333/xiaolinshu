package com.puxinxiaolin.xiaolinshu.auth.controller;

import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.puxinxiaolin.xiaolinshu.auth.alarm.AlarmConfig;
import com.puxinxiaolin.xiaolinshu.auth.alarm.AlarmInterface;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestController {

    @Resource
    private AlarmInterface alarmInterface;
    
//    @Value("${rate-limit.api.limit}")
    @NacosValue(value = "${rate-limit.api.limit}", autoRefreshed = true)
    private Integer limit;

    @GetMapping("/test")
    public String test() {
        return "当前限流阈值为: " + limit;
    }

    @GetMapping("/alarm")
    public String sendAlarm() {
        alarmInterface.send("系统出错啦，小林速度上线解决问题！");
        return "alarm success";
    }

}
