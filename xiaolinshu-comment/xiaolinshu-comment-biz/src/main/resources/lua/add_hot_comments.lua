-- 批量添加评论到 ZSET 中

local zSetKey = KEYS[1]
-- 获取传入的数据和分数列表
local membersAndScores = ARGV
-- ZSet 最多缓存 500 条评论
local sizeLimit = 500

-- 判断 ZSet 是否存在
if redis.call('EXISTS', zSetKey) == 0 then
    return -1
end 

-- 获取当前 ZSet 大小
local currentSize = redis.call('ZCARD', zSetKey)

for i = 1, #membersAndScores, 2 do
    local commentId = membersAndScores[i]
    local score = membersAndScores[i + 1]

    if currentSize < sizeLimit then
        redis.call('ZADD', zSetKey, score, commentId)
        currentSize = currentSize + 1
    else
        -- 则达到最大限制, 停止添加
        break
    end
end

return 0