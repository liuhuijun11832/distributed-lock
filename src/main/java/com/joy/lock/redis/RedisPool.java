package com.joy.lock.redis;

import redis.clients.jedis.Jedis;

import java.util.Collections;

/**
 * @Description: redis分布式锁实现
 * @Author: Joy
 * @Date: 2019-04-17 14:45
 */
public class RedisPool {

    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXISTS = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;

    /**
     * @description 获取分布式锁
     * @param jedis
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @return
     */
    public static boolean tryGetDistributedLock(Jedis jedis,String lockKey,String requestId,int expireTime){
        /**
         * lockKey作为key,requestId作为value用于区分加锁的请求，可以使用不重复的字符串例如UUID或者GUID
         * NX表示该key不存在时才会进行set操作
         * PX表示设置过期时间，具体值由最后一个int值决定
         * jedis.setnx()没有提供直接设置超时的操作，如果锁没有释放会导致死锁
         * 这里尽量使用一行操作来set，如果多个操作无法保证原子性
         */
        String result = jedis.set(lockKey, requestId, SET_IF_NOT_EXISTS, SET_WITH_EXPIRE_TIME, expireTime);
        return LOCK_SUCCESS.equals(result);
    }

    /**
     *分布式锁需要满足几个条件：互斥、不会死锁、容错、唯一解锁
     */

    /**
     * @description 释放锁
     * @param jedis
     * @param lockKey
     * @param requestId
     * @return
     */
    public static boolean releaseDistributedLock(Jedis jedis,String lockKey,String requestId){
        //将所有的释放和获取操作交由一行Lua脚本操作完成，保证原子操作
        //eval命令执行Lua代码的时候，Lua代码将被当成一个命令去执行，并且直到eval命令执行完成，Redis才会执行其他命令
        //如果使用先get lockKey的值，然后比对requestId的方式判断是否同一请求，可能导致删除的是其他requestID
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        return RELEASE_SUCCESS.equals(result);
    }

}
