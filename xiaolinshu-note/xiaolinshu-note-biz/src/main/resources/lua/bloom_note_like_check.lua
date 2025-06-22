-- LUA 脚本：点赞布隆过滤器

local key = KEYS[1]
local value = ARGV[1]

-- 使用 EXISTS 命令检查布隆过滤器是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 校验该篇笔记是否被点赞过(1 表示已经点赞, 0 表示未点赞)
local isLiked = redis.call('BF.EXISTS', key, value)
if isLiked == 1 then
    return 1
end

-- 未被点赞, 添加点赞数据
redis.call('BF.ADD', key, value)
return 0