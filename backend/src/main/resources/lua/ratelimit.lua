-- 固定窗口限流：自增、设置过期、超限判断合并在一个脚本内原子执行
-- KEYS[1] = 计数器 key
-- ARGV[1] = 窗口秒数
-- ARGV[2] = 窗口内允许的最大次数
-- 返回 1 = 放行，0 = 拒绝

local count = redis.call('INCR', KEYS[1])

-- 第一个进入窗口的请求负责设置过期时间，窗口翻篇靠 Redis 自动删除
if count == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end

if count > tonumber(ARGV[2]) then
    return 0
end

return 1
