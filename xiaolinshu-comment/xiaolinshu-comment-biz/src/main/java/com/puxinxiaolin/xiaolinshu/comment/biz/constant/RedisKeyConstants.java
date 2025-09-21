package com.puxinxiaolin.xiaolinshu.comment.biz.constant;

public class RedisKeyConstants {

    /**
     * Key 前缀：布隆过滤器 - 用户点赞的评论
     */
    private static final String BLOOM_COMMENT_LIKES_KEY_PREFIX = "bloom:comment:likes:";
    
    /**
     * Key 前缀：二级评论分页 ZSET
     */
    private static final String CHILD_COMMENT_LIST_KEY_PREFIX = "comment:childList:";
    
    /**
     * 评论维度计数 Key 前缀
     */
    private static final String COUNT_COMMENT_KEY_PREFIX = "count:comment:";

    /**
     * Hash Field: 子评论总数
     */
    public static final String FIELD_CHILD_COMMENT_TOTAL = "childCommentTotal";

    /**
     * Hash Field: 点赞总数
     */
    public static final String FIELD_LIKE_TOTAL = "likeTotal";
    
    /**
     * Key 前缀: 评论详情 JSON
     */
    private static final String COMMENT_DETAIL_KEY_PREFIX  = "comment:detail:";
    
    /**
     * Key 前缀: 评论分页 ZSET
     */
    private static final String COMMENT_LIST_KEY_PREFIX = "comment:list:";
    
    /**
     * Key 前缀: 笔记评论总数
     */
    private static final String COUNT_COMMENT_TOTAL_KEY_PREFIX = "count:note:";

    /**
     * Hash Field 键: 评论总数
     */
    public static final String FIELD_COMMENT_TOTAL = "commentTotal";
    
    /**
     * Key 前缀: 一级评论的 first_reply_comment_id 字段值是否更新标识
     */
    private static final String HAVE_FIRST_REPLY_COMMENT_KEY_PREFIX = "comment:havaFirstReplyCommentId:";

    public static String buildBloomCommentLikesKey(Long userId) {
        return BLOOM_COMMENT_LIKES_KEY_PREFIX + userId;
    }
    
    public static String buildChildCommentListKey(Long commentId) {
        return CHILD_COMMENT_LIST_KEY_PREFIX + commentId;
    }
    
    public static String buildCountCommentKey(Long commentId) {
        return COUNT_COMMENT_KEY_PREFIX + commentId;
    }
    
    public static String buildCommentDetailKey(Object commentId) {
        return COMMENT_DETAIL_KEY_PREFIX + commentId;
    }
    
    public static String buildCommentListKey(Long noteId) {
        return COMMENT_LIST_KEY_PREFIX + noteId;
    }
    
    public static String buildNoteCommentTotalKey(Long noteId) {
        return COUNT_COMMENT_TOTAL_KEY_PREFIX + noteId;
    }
    
    public static String buildHaveFirstReplyCommentKey(Long commentId) {
        return HAVE_FIRST_REPLY_COMMENT_KEY_PREFIX + commentId;
    }
    
}
