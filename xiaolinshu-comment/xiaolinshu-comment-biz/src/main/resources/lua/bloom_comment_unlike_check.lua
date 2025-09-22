local key = KEYS[1]
local commentId = ARGV[1]

local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

return redis.call('BF.EXISTS', key, commentId)