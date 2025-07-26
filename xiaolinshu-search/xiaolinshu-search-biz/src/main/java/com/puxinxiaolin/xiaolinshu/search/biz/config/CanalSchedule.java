package com.puxinxiaolin.xiaolinshu.search.biz.config;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Maps;
import com.puxinxiaolin.xiaolinshu.search.biz.domain.mapper.SelectMapper;
import com.puxinxiaolin.xiaolinshu.search.biz.enums.NoteStatusEnum;
import com.puxinxiaolin.xiaolinshu.search.biz.enums.NoteVisibleEnum;
import com.puxinxiaolin.xiaolinshu.search.biz.enums.UserStatusEnum;
import com.puxinxiaolin.xiaolinshu.search.biz.index.NoteIndex;
import com.puxinxiaolin.xiaolinshu.search.biz.index.UserIndex;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 实现 canal 构建笔记、用户实时增量 es 索引
 * @Author: YCcLin
 * @Date: 2025/7/26 11:22
 */
@Component
@Slf4j
public class CanalSchedule implements Runnable {
    @Resource
    private CanalProperties canalProperties;
    @Resource
    private CanalConnector canalConnector;
    @Resource
    private RestHighLevelClient restHighLevelClient;
    @Resource
    private SelectMapper selectMapper;

    /**
     * 定时拉取指定数量的批量消息, 每 100ms 执行一次
     */
    @Scheduled(fixedDelay = 100)
    @Override
    public void run() {
        // 初始化批次 ID, -1 表示未开始或未获取到数据
        long batchId = -1;
        try {
            // 从 canalConnector 获取批量消息, 返回的数据量由 batchSize 控制, 若不足, 则拉取已有的
            Message message = canalConnector.getWithoutAck(canalProperties.getBatchSize());

            // 获取当前拉取消息的批次 ID
            batchId = message.getId();
            // 获取当前批次中的数据条数
            int size = message.getEntries().size();

            if (batchId == -1 || size == 0) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
            } else {
                // 如果当前批次有数据, 处理这批次数据
                processEntry(message.getEntries());
            }

            // 对当前批次的消息进行 ack 确认, 表示该批次的数据已经被成功消费
            canalConnector.ack(batchId);
        } catch (Exception e) {
            log.error("消费 Canal 批次数据异常: {}", e.getMessage(), e);

            // 如果出现异常, 需要进行数据回滚, 以便重新消费这批次的数据
            canalConnector.rollback(batchId);
        }
    }

    /**
     * 打印这一批次中的数据条目
     *
     * @param entries
     */
    private void processEntry(List<CanalEntry.Entry> entries) throws Exception {
        for (CanalEntry.Entry entry : entries) {
            // 只处理 ROWDATA 行数据类型的 Entry，忽略事务等其他类型
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                // 获取事件类型，如：INSERT、UPDATE、DELETE 等等
                CanalEntry.EventType eventType = entry.getHeader().getEventType();
                String database = entry.getHeader().getSchemaName();
                String table = entry.getHeader().getTableName();

                // 解析出 RowChange 对象，包含 RowData 和事件相关信息
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    // 获取行中所有列的最新值（AfterColumns）
                    List<CanalEntry.Column> newestColumnValues = rowData.getAfterColumnsList();
                    Map<String, Object> columnMap = parseColumns2Map(newestColumnValues);

                    log.info("EventType: {}, Database: {}, Table: {}, Columns: {}", eventType, database, table, columnMap);

                    // 处理事件
                    processEvent(columnMap, table, eventType);
                }
            }
        }
    }

    /**
     * 处理事件
     *
     * @param columnMap
     * @param table
     * @param eventType
     */
    private void processEvent(Map<String, Object> columnMap, String table, CanalEntry.EventType eventType) throws Exception {
        switch (table) {
            case "t_note" -> handleNoteEvent(columnMap, eventType);
            case "t_user" -> handleUserEvent(columnMap, eventType);
            default -> log.warn("Table: {} not support", table);
        }
    }

    /**
     * 处理用户表事件
     *
     * @param columnMap
     * @param eventType
     */
    private void handleUserEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType) throws Exception {
        Long userId = Long.valueOf(columnMap.get("id").toString());

        switch (eventType) {
            case INSERT -> syncUserIndex(userId);
            case UPDATE -> {
                Integer status = Integer.valueOf(columnMap.get("status").toString());
                Integer isDeleted = Integer.valueOf(columnMap.get("is_deleted").toString());
                if (Objects.equals(status, UserStatusEnum.ENABLE.getCode())
                        || Objects.equals(isDeleted, 0)) {
                    syncNoteIndexAndUserIndex(userId);
                } else if (Objects.equals(status, UserStatusEnum.DISABLED.getCode())
                        || Objects.equals(isDeleted, 1)) {
                    deleteUserDocument(userId.toString());
                }
            }
            default -> log.warn("Unhandled event type for t_user: {}", eventType);
        }
    }

    /**
     * 删除指定 ID 的用户文档
     *
     * @param documentId
     */
    private void deleteUserDocument(String documentId) throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest(UserIndex.NAME, documentId);
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    /**
     * 同步笔记索引和用户索引（可能是多条）
     *
     * @param userId
     */
    private void syncNoteIndexAndUserIndex(Long userId) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();

        // 用户索引
        List<Map<String, Object>> userResult = selectMapper.selectESUserIndexData(userId);
        for (Map<String, Object> map : userResult) {
            bulkRequest.add(new IndexRequest(UserIndex.NAME)
                    .id(map.get(UserIndex.FIELD_USER_ID).toString())
                    .source(map));
        }

        // 笔记索引
        List<Map<String, Object>> noteResult = selectMapper.selectESNoteIndexData(null, userId);
        for (Map<String, Object> map : noteResult) {
            bulkRequest.add(new IndexRequest(NoteIndex.NAME)
                    .id(map.get(NoteIndex.FIELD_NOTE_ID).toString())
                    .source(map));
        }

        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    /**
     * 同步用户索引
     *
     * @param userId
     */
    private void syncUserIndex(Long userId) throws IOException {
        List<Map<String, Object>> result = selectMapper.selectESUserIndexData(userId);

        for (Map<String, Object> map : result) {
            IndexRequest request = new IndexRequest(UserIndex.NAME);
            request.id(map.get(UserIndex.FIELD_USER_ID).toString());
            request.source(map);

            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        }
    }

    /**
     * 处理笔记表事件
     *
     * @param columnMap
     * @param eventType
     */
    private void handleNoteEvent(Map<String, Object> columnMap, CanalEntry.EventType eventType) throws Exception {
        Long noteId = Long.valueOf(columnMap.get("id").toString());

        switch (eventType) {
            case INSERT -> syncNoteIndex(noteId);
            case UPDATE -> {
                Integer status = Integer.parseInt(columnMap.get("status").toString());
                Integer visible = Integer.parseInt(columnMap.get("visible").toString());

                if (Objects.equals(NoteStatusEnum.NORMAL.getCode(), status)
                        && Objects.equals(NoteVisibleEnum.PUBLIC.getCode(), visible)) {
                    syncNoteIndex(noteId);
                } else if (Objects.equals(NoteStatusEnum.DELETED.getCode(), status)
                        || Objects.equals(NoteStatusEnum.DOWNED.getCode(), status)
                        || Objects.equals(NoteVisibleEnum.PRIVATE.getCode(), visible)) {
                    deleteNoteDocument(noteId.toString());
                }
            }
            default -> log.warn("Unhandled event type for t_note: {}", eventType);
        }
    }

    /**
     * 删除笔记索引
     *
     * @param documentId
     */
    private void deleteNoteDocument(String documentId) throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest(NoteIndex.NAME, documentId);
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    /**
     * 同步笔记索引
     *
     * @param noteId
     */
    private void syncNoteIndex(Long noteId) throws Exception {
        List<Map<String, Object>> result = selectMapper.selectESNoteIndexData(noteId, null);

        for (Map<String, Object> res : result) {
            IndexRequest request = new IndexRequest(NoteIndex.NAME);
            request.id(res.get(NoteIndex.FIELD_NOTE_ID).toString());
            request.source(res);

            // 写入数据到笔记索引中
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        }
    }

    /**
     * 将列数据转为 Map
     *
     * @param columns
     * @return
     */
    private Map<String, Object> parseColumns2Map(List<CanalEntry.Column> columns) {
        HashMap<String, Object> map = Maps.newHashMap();
        columns.forEach(col -> {
            if (Objects.isNull(col)) {
                return;
            }
            map.put(col.getName(), col.getValue());
        });

        return map;
    }

}
