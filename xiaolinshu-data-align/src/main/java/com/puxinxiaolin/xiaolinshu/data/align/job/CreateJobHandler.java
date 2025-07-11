package com.puxinxiaolin.xiaolinshu.data.align.job;

import com.puxinxiaolin.xiaolinshu.data.align.constant.TableConstants;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.CreateTableMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RefreshScope
public class CreateJobHandler {

    @Value("${table.shards}")
    private int tableShards;

    @Resource
    private CreateTableMapper createTableMapper;

    @XxlJob("createTableJobHandler")
    public void createTableJobHandler() throws Exception {

        String date = LocalDate.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        XxlJobHelper.log("## 开始初始化明日增量数据表...");

        if (tableShards > 0) {
            for (int hashKey = 0; hashKey < tableShards; hashKey++) {
                String tableNameSuffix = TableConstants.buildTableNameSuffix(date, hashKey);

                createTableMapper.createDataAlignFollowingCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignFansCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNoteCollectCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignUserCollectCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignUserLikeCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNoteLikeCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNotePublishCountTempTable(tableNameSuffix);
            }
        }

        XxlJobHelper.log("## 结束创建日增量数据表, 日期: {}...", date);
    }

}
