-- 固定窗口限流：KEYS[1]=限流键，ARGV[1]=窗口秒数，ARGV[2]=上限
-- 首次计数时设置过期；超限返回 0，放行返回 1
local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, window)
end

if current > limit then
    return 0
end
return 1
