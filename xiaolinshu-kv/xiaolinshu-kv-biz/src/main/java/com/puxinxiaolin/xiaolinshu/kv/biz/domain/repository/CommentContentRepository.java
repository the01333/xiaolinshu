package com.puxinxiaolin.xiaolinshu.kv.biz.domain.repository;

import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.CommentContentDO;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.CommentContentPrimaryKey;
import com.puxinxiaolin.xiaolinshu.kv.biz.domain.dataobject.NoteContentDO;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface CommentContentRepository extends CassandraRepository<CommentContentDO, CommentContentPrimaryKey> {

    /**
     * 删除评论正文
     *
     * @param noteId
     * @param yearMonth
     * @param contentId
     */
    void deleteByPrimaryKeyNoteIdAndPrimaryKeyYearMonthAndPrimaryKeyContentId(Long noteId,
                                                                              String yearMonth,
                                                                              UUID contentId);

    /**
     * 批量查询评论内容
     *
     * @param noteId     分区键 1
     * @param yearMonths 分区键 2
     * @param contentIds
     * @return
     */
    List<CommentContentDO> findByPKNoteIdAndPKYearMonthAndPKContentId(Long noteId,
                                                                      List<String> yearMonths,
                                                                      List<UUID> contentIds);

}