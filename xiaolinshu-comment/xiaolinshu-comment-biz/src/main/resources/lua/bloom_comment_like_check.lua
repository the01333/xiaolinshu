-- LUA 脚本: 评论点赞布隆过滤器

local key = KEYS[1]
local commentId = ARGV[1]  -- 笔记 ID

-- 判断布隆过滤器是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 校验是否已经点赞过（1-表示点赞过 0-表示未点赞过）
local isLiked = redis.call('BF.EXISTS', key, commentId)
if isLiked == 1 then
    return 1
end

-- 未被点赞, 添加点赞记录
redis.call('BF.ADD', key, commentId)
return 0