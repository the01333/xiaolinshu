package com.puxinxiaolin.xiaolinshu.note.biz.constant;

public class RedisKeyConstants {

    public static final String NOTE_DETAIL_KEY = "note:detail:";
    
    public static String buildNoteDetailKey(Long noteId) {
        return NOTE_DETAIL_KEY + noteId;
    }

}

