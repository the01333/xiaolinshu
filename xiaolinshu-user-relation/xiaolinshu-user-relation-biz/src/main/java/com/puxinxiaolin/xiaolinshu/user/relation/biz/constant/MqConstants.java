package com.puxinxiaolin.xiaolinshu.user.relation.biz.constant;

public interface MqConstants {

    /**
     * Topic: 关注、取关共用一个主题
     */
    String TOPIC_FOLLOW_OR_UNFOLLOW = "FollowUnfollowTopic";

    /**
     * Tag: 关注
     */
    String TAG_FOLLOW = "Follow";

    /**
     * Tag: 取关
     */
    String TAG_UNFOLLOW = "Unfollow";
    
}
