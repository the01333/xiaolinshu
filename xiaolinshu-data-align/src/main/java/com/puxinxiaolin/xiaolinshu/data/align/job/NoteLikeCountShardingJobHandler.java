package com.puxinxiaolin.xiaolinshu.data.align.job;

import cn.hutool.core.collection.CollUtil;
import com.puxinxiaolin.xiaolinshu.data.align.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.data.align.constant.TableConstants;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.DeleteMapper;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.SelectMapper;
import com.puxinxiaolin.xiaolinshu.data.align.domain.mapper.UpdateMapper;
import com.puxinxiaolin.xiaolinshu.data.align.rpc.SearchRpcService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class NoteLikeCountShardingJobHandler {
    @Resource
    private SelectMapper selectMapper;
    @Resource
    private UpdateMapper updateMapper;
    @Resource
    private DeleteMapper deleteMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private SearchRpcService searchRpcService;

    @XxlJob("noteLikeCountShardingJobHandler")
    public void noteLikeCountShardingJobHandler() throws Exception {
        // 分片序号
        int shardIndex = XxlJobHelper.getShardIndex();
        // 分片总数
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("=================> 开始定时分片广播任务: 对当日发生变更的笔记点赞数进行对齐");
        XxlJobHelper.log("分片参数: 当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        log.info("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tableNameSuffix = TableConstants.buildTableNameSuffix(date, shardIndex);

        int batchSize = 1000;
        int processedTotal = 0;

        for (; ; ) {
            // 1. 分批次查询 t_data_align_note_like_count_temp_日期_分片序号, 如一批次查询 1000 条
            List<Long> noteIds = selectMapper.selectBatchFromDataAlignNoteLikeCountTempTable(tableNameSuffix, batchSize);
            if (CollUtil.isEmpty(noteIds)) {
                break;
            }

            // 2. 对 t_note_like 笔记点赞表执行 count(*) 操作，获取总数
            noteIds.forEach(noteId -> {
                int likeCount = selectMapper.selectCountFromFollowingTableByUserId(noteId);

                // 3. 更新 t_note_count 表，并更新对应 Redis 缓存
                int count = updateMapper.updateNoteLikeTotalByUserId(noteId, likeCount);
                if (count > 0) {
                    String redisKey = RedisKeyConstants.buildCountNoteKey(noteId);
                    Boolean hasKey = redisTemplate.hasKey(redisKey);
                    if (hasKey) {
                        redisTemplate.opsForHash()
                                .put(redisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, likeCount);
                    }
                }

                // 走 RPC, 重新构建 es 文档
                searchRpcService.rebuildNoteDocument(noteId);
            });

            // 4. 批量物理删除这一批次记录
            deleteMapper.batchDeleteDataAlignNoteLikeCountTempTable(tableNameSuffix, noteIds);
            
            processedTotal += noteIds.size();
        }

        XxlJobHelper.log("=================> 结束定时分片广播任务: 对当日发生变更的笔记点赞数进行对齐, 共对齐记录数: {}", processedTotal);
    }

}
