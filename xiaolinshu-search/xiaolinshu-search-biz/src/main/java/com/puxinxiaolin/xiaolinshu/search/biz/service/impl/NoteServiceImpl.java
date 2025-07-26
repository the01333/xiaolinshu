package com.puxinxiaolin.xiaolinshu.search.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.constant.DateConstants;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.DateUtils;
import com.puxinxiaolin.framework.common.util.NumberUtils;
import com.puxinxiaolin.xiaolinshu.search.api.dto.RebuildNoteDocumentReqDTO;
import com.puxinxiaolin.xiaolinshu.search.biz.domain.mapper.SelectMapper;
import com.puxinxiaolin.xiaolinshu.search.biz.enums.NotePublishTimeRangeEnum;
import com.puxinxiaolin.xiaolinshu.search.biz.enums.NoteSortTypeEnum;
import com.puxinxiaolin.xiaolinshu.search.biz.index.NoteIndex;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchNoteReqVO;
import com.puxinxiaolin.xiaolinshu.search.biz.model.vo.SearchNoteRespVO;
import com.puxinxiaolin.xiaolinshu.search.biz.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private SelectMapper selectMapper;
    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 重建笔记文档
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> rebuildDocument(RebuildNoteDocumentReqDTO request) {
        Long noteId = request.getId();

        List<Map<String, Object>> result = selectMapper.selectESNoteIndexData(noteId, null);
        for (Map<String, Object> map : result) {
            IndexRequest indexRequest = new IndexRequest(NoteIndex.NAME)
                    .id(map.get(NoteIndex.FIELD_NOTE_ID).toString())
                    .source(map);

            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("==> 重建笔记文档失败: {}", e.getMessage(), e);
            }
        }
        
        return Response.success();
    }

    // @formatter:off
    /**
     * 搜索笔记
     * 【使用 function_score 自定义调整文档得分】
     * POST /note/_search
     * {
     *   "query": {
     *     "function_score": {
     *       "query": {
     *         "multi_match": {
     *           "query": "壁纸",
     *           "fields": [
     *             "title^2",
     *             "topic"
     *           ]
     *         }
     *       },
     *       "functions": [
     *         {
     *           "field_value_factor": {
     *             "field": "like_total",
     *             "factor": 0.5,
     *             "modifier": "sqrt",
     *             "missing": 0
     *           }
     *         },
     *         {
     *           "field_value_factor": {
     *             "field": "collect_total",
     *             "factor": 0.3,
     *             "modifier": "sqrt",
     *             "missing": 0
     *           }
     *         },
     *         {
     *           "field_value_factor": {
     *             "field": "comment_total",
     *             "factor": 0.2,
     *             "modifier": "sqrt",
     *             "missing": 0
     *           }
     *         }
     *       ],
     *       "score_mode": "sum",
     *       "boost_mode": "sum"
     *     }
     *   },
     *   "sort": [
     *     {
     *       "_score": {
     *         "order": "desc"
     *       }
     *     }
     *   ],
     *   "from": 0,
     *   "size": 10,
     *   "highlight": {
     *     "fields": {
     *       "title": {
     *         "pre_tags": [
     *           "<strong>"
     *         ],
     *       }
     *     }
     *   }
     * }
     * @param request
     * @return
     */
    // @formatter:on
    @Override
    public PageResponse<SearchNoteRespVO> searchNote(SearchNoteReqVO request) {
        String keyword = request.getKeyword();
        Integer pageNo = request.getPageNo();
        Integer type = request.getType();
        Integer sort = request.getSort();
        Integer publishTimeRange = request.getPublishTimeRange();

        // 1. 构建搜索请求
        SearchRequest searchRequest = new SearchRequest(NoteIndex.NAME);

        // 2. 搜索请求里的条件 builder
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 2.1 创建查询条件
        /*
            "query": {
                "bool": {
                    "must": [
                        {
                            "multi_match": {
                                "query": "壁纸",
                                "fields": [
                                  "title^2",
                                  "topic"
                                ]
                            }
                        }
                    ],
                    "filter": [
                        {
                          "term": {
                            "type": 0
                          }
                        }
                    ]
                }
            }
         */
        // 使用 BoolQuery 以及设置过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(
                QueryBuilders.multiMatchQuery(keyword)
                        .field(NoteIndex.FIELD_NOTE_TITLE, 2.0f)
                        .field(NoteIndex.FIELD_NOTE_TOPIC)
        );
        if (Objects.nonNull(type)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(NoteIndex.FIELD_NOTE_TYPE, type));
        }
        NotePublishTimeRangeEnum notePublishTimeRangeEnum = NotePublishTimeRangeEnum.valueOf(publishTimeRange);
        if (Objects.nonNull(notePublishTimeRangeEnum)) {
            String endTime = LocalDateTime.now().format(DateConstants.Y_M_D_H_M_S);
            String startTime = null;
            switch (notePublishTimeRangeEnum) {
                case DAY -> startTime = DateUtils.localDateTime2String(LocalDateTime.now().minusDays(1));
                case WEEK -> startTime = DateUtils.localDateTime2String(LocalDateTime.now().minusWeeks(1));
                case HALF_YEAR -> startTime = DateUtils.localDateTime2String(LocalDateTime.now().minusMonths(6));
            }

            if (StringUtils.isNotBlank(startTime)) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery(NoteIndex.FIELD_NOTE_CREATE_TIME)
                        .gte(startTime)
                        .lte(endTime)
                );
            }
        }

        // 2.2 按照选择的条件进行排序, 如果没选默认综合排序
        /*
            "sort": [
                {
                    "_score": {
                        "order": "desc"  // 根据前端传过来的排序选项替换字段即可
                    }
                }
            ]
        */
        NoteSortTypeEnum noteSortTypeEnum = NoteSortTypeEnum.valueOf(sort);
        if (Objects.nonNull(noteSortTypeEnum)) {
            switch (noteSortTypeEnum) {
                case LATEST -> sourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_CREATE_TIME)
                        .order(SortOrder.DESC));
                case MOST_LIKE -> sourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_LIKE_TOTAL)
                        .order(SortOrder.DESC));
                case MOST_COLLECT -> sourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_COLLECT_TOTAL)
                        .order(SortOrder.DESC));
                case MOST_COMMENT -> sourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_COMMENT_TOTAL)
                        .order(SortOrder.DESC));
            }

            sourceBuilder.query(boolQueryBuilder);
        } else {
            // 综合排序
            sourceBuilder.sort(new FieldSortBuilder("_score")
                    .order(SortOrder.DESC));

            // 2.2 创建 FilterFunctionBuilder 数组（结果根据算分进行排序）
            /*
                "functions": [
                     {
                       "field_value_factor": {
                         "field": "like_total",
                         "factor": 0.5,  // 乘数因子, 影响最终评分的权重
                         "modifier": "sqrt",  // 计算因子（sum、sqrt...）
                         "missing": 0  // 字段缺失的默认值
                       }
                     },
                     {
                       "field_value_factor": {
                         "field": "collect_total",
                         "factor": 0.3,
                         "modifier": "sqrt",
                         "missing": 0
                       }
                     },
                     {
                       "field_value_factor": {
                         "field": "comment_total",
                         "factor": 0.2,
                         "modifier": "sqrt",
                         "missing": 0
                       }
                     }
                 ],
            */
            FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = {
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_LIKE_TOTAL)
                                    .factor(0.5f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    ),
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_COLLECT_TOTAL)
                                    .factor(0.3f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    ),
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_COMMENT_TOTAL)
                                    .factor(0.2f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    )
            };

            // 2.3 构建 function_score 查询, boolQueryBuilder + filterFunctionBuilders
            /*
                 "score_mode": "sum",
                 "boost_mode": "sum"
            */
            FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders
                    .functionScoreQuery(boolQueryBuilder, filterFunctionBuilders)
                    .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
                    .boostMode(CombineFunction.SUM);

            // 2.4 设置查询
            sourceBuilder.query(functionScoreQueryBuilder);
        }

        // 2.5 分页
        int pageSize = 10;
        int from = (pageNo - 1) * pageSize;
        sourceBuilder.from(from);
        sourceBuilder.size(pageSize);

        // 2.6 高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(NoteIndex.FIELD_NOTE_TITLE)
                .preTags("<strong>")
                .postTags("</strong>");
        sourceBuilder.highlighter(highlightBuilder);

        // 3. 把所有条件封装到搜索请求中
        searchRequest.source(sourceBuilder);

        List<SearchNoteRespVO> result = Lists.newArrayList();
        long total = 0;
        try {
            log.info("==> SearchRequest: {}", searchRequest.source().toString());

            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            total = searchResponse.getHits().getTotalHits().value;
            log.info("==> 命中文档总数, hits: {}", total);

            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                log.info("==> 文档数据: {}", hit.getSourceAsString());

                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Long noteId = (Long) sourceAsMap.get(NoteIndex.FIELD_NOTE_ID);
                String cover = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_COVER);
                String title = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_TITLE);
                String avatar = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_AVATAR);
                String nickname = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_NICKNAME);
                // 获取更新时间
                String updateTimeStr = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_UPDATE_TIME);
                LocalDateTime updateTime = LocalDateTime.parse(updateTimeStr, DateConstants.Y_M_D_H_M_S);
                Integer likeTotal = (Integer) sourceAsMap.get(NoteIndex.FIELD_NOTE_LIKE_TOTAL);
                Integer commentTotal = (Integer) sourceAsMap.get(NoteIndex.FIELD_NOTE_COMMENT_TOTAL);
                Integer collectTotal = (Integer) sourceAsMap.get(NoteIndex.FIELD_NOTE_COLLECT_TOTAL);

                // 获取高亮字段
                String highlightedTitle = null;
                if (CollUtil.isNotEmpty(hit.getHighlightFields())
                        && hit.getHighlightFields().containsKey(NoteIndex.FIELD_NOTE_TITLE)) {
                    highlightedTitle = hit.getHighlightFields().get(NoteIndex.FIELD_NOTE_TITLE).fragments()[0].string();
                }

                SearchNoteRespVO searchNoteRspVO = SearchNoteRespVO.builder()
                        .noteId(noteId)
                        .cover(cover)
                        .title(title)
                        .highlightTitle(highlightedTitle)
                        .avatar(avatar)
                        .nickname(nickname)
                        .updateTime(DateUtils.formatRelativeTime(updateTime))
                        .likeTotal(NumberUtils.formatNumberString(likeTotal))
                        .commentTotal(NumberUtils.formatNumberString(commentTotal))
                        .collectTotal(NumberUtils.formatNumberString(collectTotal))
                        .build();
                result.add(searchNoteRspVO);
            }

        } catch (IOException e) {
            log.error("==> 查询 Elasticsearch 异常: {}", e.getMessage(), e);
        }

        return PageResponse.success(result, pageNo, total);
    }

}
