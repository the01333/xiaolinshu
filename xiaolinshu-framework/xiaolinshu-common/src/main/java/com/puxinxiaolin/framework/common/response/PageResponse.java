package com.puxinxiaolin.framework.common.response;

import lombok.Data;

import java.util.List;

/**
 * @Description: 分页返参实体
 * @Author: YCcLin
 * @Date: 2025/6/6 15:33
 */
@Data
public class PageResponse<T> extends Response<List<T>> {

    private long pageNo;
    private long pageSize;
    private long totalCount;
    private long totalPage;

    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount) {
        PageResponse<T> pageResponse = new PageResponse<>();

        pageResponse.setSuccess(true);
        pageResponse.setData(data);
        pageResponse.setPageNo(pageNo);
        pageResponse.setTotalCount(totalCount);

        long pageSize = 10L;
        pageResponse.setPageSize(pageSize);

        // 向上取整
        long totalPage = (totalCount + pageSize - 1) / pageSize;
        pageResponse.setTotalPage(totalPage);
        return pageResponse;
    }

    public static <T> PageResponse<T> success(List<T> data, long pageNo, long totalCount, long pageSize) {
        PageResponse<T> pageResponse = new PageResponse<>();

        pageResponse.setSuccess(true);
        pageResponse.setData(data);
        pageResponse.setPageNo(pageNo);
        pageResponse.setTotalCount(totalCount);
        pageResponse.setPageSize(pageSize);

        long totalPage = pageSize == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
        pageResponse.setTotalPage(totalPage);
        return pageResponse;
    }

    /**
     * 获取总页数
     *
     * @return
     */
    public static long getTotalPage(long totalCount, long pageSize) {
        return pageSize == 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
    }

    /**
     * 获取偏移量
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    public static long getOffset(long pageNo, long pageSize) {
        if (pageNo < 1) {
            pageNo = 1;
        }

        return (pageNo - 1) * pageSize;
    }

}
