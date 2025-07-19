package com.puxinxiaolin.xiaolinshu.search.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.util.NumberUtils;
import com.puxinxiaolin.xiaolinshu.search.index.UserIndex;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserReqVO;
import com.puxinxiaolin.xiaolinshu.search.model.vo.SearchUserRespVO;
import com.puxinxiaolin.xiaolinshu.search.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private RestHighLevelClient restHighLevelClient;

    /**
     * 搜索用户
     *
     * @param request
     * @return
     */
    @Override
    public PageResponse<SearchUserRespVO> searchUser(SearchUserReqVO request) {
        String keyword = request.getKeyword();
        Integer pageNo = request.getPageNo();

        SearchRequest searchRequest = new SearchRequest(UserIndex.NAME);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 构建条件
        sourceBuilder.query(QueryBuilders.multiMatchQuery(
                keyword, UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_XIAOLINSHU_ID)
        );
        FieldSortBuilder sortBuilder = new FieldSortBuilder(UserIndex.FIELD_USER_FANS_TOTAL)
                .order(SortOrder.DESC);
        sourceBuilder.sort(sortBuilder);

        // 分页
        int pageSize = 10;
        int from = (pageNo - 1) * pageSize;
        sourceBuilder.from(from);
        sourceBuilder.size(pageSize);

        // 设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(UserIndex.FIELD_USER_NICKNAME)
                .preTags("<strong>")
                .postTags("</strong>");
        sourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(sourceBuilder);

        List<SearchUserRespVO> result = Lists.newArrayList();
        long total = 0;
        try {
            log.info("==> SearchRequest: {}", searchRequest);

            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            total = searchResponse.getHits().getTotalHits().value;
            log.info("==> 命中文档总数, hits: {}", total);

            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                log.info("==> 文档数据: {}", hit.getSourceAsString());

                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Long userId = (Long) (sourceAsMap.get(UserIndex.FIELD_USER_ID));
                String nickname = (String) sourceAsMap.get(UserIndex.FIELD_USER_NICKNAME);
                String avatar = (String) sourceAsMap.get(UserIndex.FIELD_USER_AVATAR);
                String xiaolinshuId = (String) sourceAsMap.get(UserIndex.FIELD_USER_XIAOLINSHU_ID);
                Integer noteTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_NOTE_TOTAL);
                Integer fansTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_FANS_TOTAL);
                // 高亮字段
                String highlightNickname = null;
                if (CollUtil.isNotEmpty(hit.getHighlightFields())
                        && hit.getHighlightFields().containsKey(UserIndex.FIELD_USER_NICKNAME)) {
                    highlightNickname = hit.getHighlightFields().get(UserIndex.FIELD_USER_NICKNAME)
                            .fragments()[0].string();
                }

                SearchUserRespVO respVO = SearchUserRespVO.builder()
                        .userId(userId)
                        .nickName(nickname)
                        .avatar(avatar)
                        .xiaolinshuId(xiaolinshuId)
                        .noteTotal(noteTotal)
                        .fansTotal(NumberUtils.formatNumberString(fansTotal))
                        .highlightNickname(highlightNickname)
                        .build();
                result.add(respVO);
            }
        } catch (Exception e) {
            log.error("==> 查询 Elasticsearch 异常: {}", e.getMessage(), e);
        }

        return PageResponse.success(result, pageNo, total);
    }

}
