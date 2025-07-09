package com.puxinxiaolin.xiaolinshu.note.biz.constant;

public interface MQConstants {

    // ------------------ collect or unCollect ---------------------

    // Topic: 计数 - 笔记收藏数
    String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic";
    
    // Topic 主题: 笔记收藏、取消收藏
    String TOPIC_COLLECT_OR_UN_COLLECT = "CollectUnCollectTopic";
    
    // Tag 标签: 笔记收藏
    String TAG_COLLECT = "Collect";
    
    // Tag 标签: 笔记取消收藏
    String TAG_UN_COLLECT = "UnCollect";
    
    // -------------------- like or unLike -----------------------
    
    // Topic: 计数 - 笔记点赞数
    String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";
    
    // Topic 主题: 笔记点赞、取消点赞
    String TOPIC_LIKE_OR_UNLIKE = "LikeUnlikeTopic";
    
    // Tag 标签: 笔记点赞
    String TAG_LIKE = "Like";
    
    // Tag 标签: 笔记取消点赞
    String TAG_UNLIKE = "Unlike";
    
    // ------------------ note ------------------
    
    // Topic: 笔记操作（发布、删除）
    String TOPIC_NOTE_OPERATE = "NoteOperateTopic";
    
    // Tag 标签: 笔记发布
    String TAG_NOTE_PUBLISH = "publishNote";
    
    // Tag 标签: 笔记删除
    String TAG_NOTE_DELETE = "deleteNote";
    
    // Topic 主题: 删除笔记本地缓存
    String TOPIC_DELETE_NOTE_LOCAL_CACHE = "DeleteNoteLocalCacheTopic";

    // Topic 主题: 延迟双删 Redis 笔记缓存
    String TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE = "DelayDeleteNoteRedisCacheTopic";
    
}
