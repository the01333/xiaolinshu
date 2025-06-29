-- 校验笔记收藏 ZSET 列表是否已经初始化并进行更新

local key = KEYS[1]
local noteId = ARGV[1]
local timestamp = ARGV[2]

local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

local size = redis.call('ZCARD', key)
-- 若已经收藏了 300 篇笔记, 则移除最早收藏的那篇
if size >= 300 then
    redis.call('ZPOPMIN', key)
end

-- 添加新的笔记收藏关系
redis.call('ZADD', key, timestamp, noteId)
return 0