package com.puxinxiaolin.xiaolinshu.data.align.job;

import cn.hutool.core.collection.CollUtil;
import com.puxinxiaolin.xiaolinshu.data.align.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.TableConstants;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.DeleteMapper;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.SelectMapper;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.UpdateMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @Description: 定时分片广播任务: 对当日发生变更的用户关注数进行对齐
 * @Author: YCcLin
 * @Date: 2025/7/12 10:36
 */
@Component
@Slf4j
public class FollowingCountShardingJobHandler {
    @Resource
    private SelectMapper selectMapper;
    @Resource
    private UpdateMapper updateMapper;
    @Resource
    private DeleteMapper deleteMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @XxlJob("followingCountShardingJobHandler")
    public void followingCountShardingJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("分片参数: 当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        log.info("分片参数: 当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);

        // 一批次 1000 条
        int batchSize = 1000;
        // 共对齐了多少条记录
        int processedTotal = 0;

        // 1. 分批次查询 t_data_align_following_count_temp_日期_分片序号，如一批次查询 1000 条，直到全部查询完成
        for (; ; ) {
            List<Long> userIds = selectMapper.selectBatchFromDataAlignFollowingCountTempTable(tableNameSuffix, batchSize);
            if (CollUtil.isEmpty(userIds)) {
                break;
            }

            userIds.forEach(userId -> {
                // 2. 对 t_following 关注表执行 count(*) 操作，获取总数
                int followingCount = selectMapper.selectCountFromFollowingTableByUserId(userId);

                // 3. 更新 t_user_count 表，并更新对应 Redis 缓存
                int count = updateMapper.updateUserFollowingTotalByUserId(userId, followingCount);
                if (count > 0) {
                    String redisKey = RedisKeyConstants.buildCountUserKey(userId);
                    Boolean existed = redisTemplate.hasKey(redisKey);
                    if (existed) {
                        redisTemplate.opsForHash()
                                .put(redisKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, followingCount);
                    }
                }
            });

            // 4. 批量物理删除这一批次记录
            deleteMapper.batchDeleteDataAlignFollowingCountTempTable(tableNameSuffix, userIds);
            
            processedTotal += userIds.size();
        }

        XxlJobHelper.log("=================> 结束定时分片广播任务: 对当日发生变更的用户关注数进行对齐, 共对齐记录数: {}", processedTotal);
    }

}
