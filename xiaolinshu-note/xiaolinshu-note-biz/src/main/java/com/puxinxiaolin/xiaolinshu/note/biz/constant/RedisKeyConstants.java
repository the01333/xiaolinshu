package com.puxinxiaolin.xiaolinshu.note.biz.constant;

public class RedisKeyConstants {

    // 用户笔记点赞列表 ZSet 前缀
    public static final String USER_NOTE_LIKE_ZSET_KEY = "user:note:likes:";
    
    // 布隆过滤器: 用户笔记点赞
    public static final String BLOOM_USER_NOTE_LIKE_LIST_KEY = "bloom:note:likes:";

    public static final String NOTE_DETAIL_KEY = "note:detail:";

    public static String buildNoteDetailKey(Long noteId) {
        return NOTE_DETAIL_KEY + noteId;
    }

    public static String buildBloomUserNoteLikeListKey(Long userId) {
        return BLOOM_USER_NOTE_LIKE_LIST_KEY + userId;
    }

    public static String buildUserNoteLikeZSetKey(Long userId) {
        return USER_NOTE_LIKE_ZSET_KEY + userId;
    }

}

