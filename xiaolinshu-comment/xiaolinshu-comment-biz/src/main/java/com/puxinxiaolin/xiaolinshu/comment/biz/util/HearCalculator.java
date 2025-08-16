package com.puxinxiaolin.xiaolinshu.comment.biz.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @Description: 热力值的计算 util
 * @original formula:
 * 热度 = (点赞数 * 权重1) + (回复数 * 权重2) + (收藏数 * 权重3) + (时间权重) + (用户影响力 * 权重4)
 * @actual formula:
 * 热度仅从被点赞数（70%）和回复数（30%）来参与计算
 * @Author: YCcLin
 * @Date: 2025/8/17 0:50
 */
public class HearCalculator {

    private static final double LIKE_WEIGHT = 0.7;
    private static final double REPLY_WEIGHT = 0.3;

    public static BigDecimal calculateHeat(Long likeCount, Long replyCount) {
        BigDecimal likeWeight = new BigDecimal(LIKE_WEIGHT);
        BigDecimal replyWeight = new BigDecimal(REPLY_WEIGHT);

        BigDecimal likeCountBD = new BigDecimal(likeCount);
        BigDecimal replyCountBD = new BigDecimal(replyCount);

        BigDecimal heat = likeCountBD.multiply(likeWeight)
                .add(replyCountBD.multiply(replyWeight));
        return heat.setScale(2, RoundingMode.HALF_UP);
    }

}
