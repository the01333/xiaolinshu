-- 批量更新热度值
-- 入参说明：
-- KEYS[1]: ZSet 的键
-- ARGV: 每个评论的数据, 格式为 member1, score1, member2, score2 ...

local zSetKey = KEYS[1]
-- 有多少条评论
local batchSize = #ARGV / 2
-- ZSet 最多缓存 500 条评论
local sizeLimit = 500

-- 判断 ZSet 是否存在
if redis.call('EXISTS', zSetKey) == 0 then
    return -1
end

for i = 1, batchSize do
    local commentId = ARGV[(i - 1) * 2 + 1]
    local score = ARGV[(i - 1) * 2 + 2]

    local currentSize = redis.call('ZCARD', zSetKey)

    if currentSize < sizeLimit then
        redis.call('ZADD', zSetKey, score, commentId)
        currentSize = currentSize + 1
    else
        -- 获取最小热度值的评论（取出升序第一条并返回对应的 score）
        local minEntry = redis.call('ZRANGE', zSetKey, 0, 0, "WITHSCORES")
        local minScore = minEntry[2]

        if score > minEntry then
            -- 如果当前评论的热度大于最小热度, 替换掉最小的; 否则无视
            -- 1）删除最小热度值的评论
            redis.call('ZREM', zSetKey, minEntry[1])
            -- 2）添加新的评论
            redis.call('ZADD', zSetKey, score, commentId)
        end
    end
end

return 0