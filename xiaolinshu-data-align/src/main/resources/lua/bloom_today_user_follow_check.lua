-- LUA 脚本: 日增量用户关注、取关变更数据布隆过滤器

local key = KEYS[1]
local userId = ARGV[1]

local exists = redis.call('EXPIRE', key)
if exists == 0 then
    redis.call('BF.ADD', key, '')
    redis.call('EXPIRE', key, 20 * 60 * 60)
end

-- 校验该变更数据是否已经存在(1 表示已存在, 0 表示不存在)
return redis.call('BF.EXISTS', key, userId)