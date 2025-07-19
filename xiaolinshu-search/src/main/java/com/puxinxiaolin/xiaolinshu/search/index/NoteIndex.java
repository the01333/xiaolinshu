package com.puxinxiaolin.xiaolinshu.search.index;

/**
 * @Description: 笔记索引
 * @Author: YCcLin
 * @Date: 2025/7/16 22:10
 */
public class NoteIndex {

    /**
     * 索引名称
     */
    public static final String NAME = "note";

    /**
     * 笔记ID
     */
    public static final String FIELD_NOTE_ID = "id";

    /**
     * 封面
     */
    public static final String FIELD_NOTE_COVER = "cover";

    /**
     * 头像
     */
    public static final String FIELD_NOTE_TITLE = "title";

    /**
     * 话题名称
     */
    public static final String FIELD_NOTE_TOPIC = "topic";

    /**
     * 发布者昵称
     */
    public static final String FIELD_NOTE_NICKNAME = "nickname";

    /**
     * 发布者头像
     */
    public static final String FIELD_NOTE_AVATAR = "avatar";

    /**
     * 笔记类型
     */
    public static final String FIELD_NOTE_TYPE = "type";

    /**
     * 发布时间
     */
    public static final String FIELD_NOTE_CREATE_TIME = "create_time";

    /**
     * 更新时间
     */
    public static final String FIELD_NOTE_UPDATE_TIME = "update_time";

    /**
     * 笔记被点赞数
     */
    public static final String FIELD_NOTE_LIKE_TOTAL = "like_total";

    /**
     * 笔记被收藏数
     */
    public static final String FIELD_NOTE_COLLECT_TOTAL = "collect_total";

    /**
     * 笔记被评论数
     */
    public static final String FIELD_NOTE_COMMENT_TOTAL = "comment_total";

}
