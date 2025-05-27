package com.puxinxiaolin.xiaolinshu.distributed.id.generator.biz.core;

import com.puxinxiaolin.xiaolinshu.distributed.id.generator.biz.core.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
