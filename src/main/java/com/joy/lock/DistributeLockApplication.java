package com.joy.lock;

import com.joy.lock.redis.RedisPool;
import com.joy.lock.zookeeper.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RestController
@Slf4j
public class DistributeLockApplication {

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @GetMapping("/jedis")
    public String test(){
        String result = "fail";
        String uuid = UUID.randomUUID().toString();
        log.info(uuid);
        if (RedisPool.tryGetDistributedLock(jedis(),"test", uuid,600000)) {
            result = "ok";
            RedisPool.releaseDistributedLock(jedis(), "test", uuid);
        }
        return result;
    }

    @GetMapping("/redisson")
    public String redis(){
        String result = "fail";
        RLock lock = redisson().getLock("test");
        lock.lock(60000L, TimeUnit.SECONDS);
        log.info("获取到锁");
        result = "ok";
        lock.unlock();
        return result;
    }

    @GetMapping("/zk")
    public String zookeeper() throws IOException, InterruptedException {
        String result = "fail";
        if(distributedLock().releaseDistributedLock()){
            result = "ok";
        }
        return result;
    }


    @Bean
    public DistributedLock distributedLock() throws IOException, InterruptedException {
        return new DistributedLock();
    }

    @Bean
    public Redisson redisson(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        Redisson redisson = (Redisson) Redisson.create(config);
        return redisson;
    }

    @Bean
    public Jedis jedis(){
        Jedis jedis = new Jedis("127.0.0.1",6379);
        return jedis;
    }

    public static void main(String[] args) {
        SpringApplication.run(DistributeLockApplication.class, args);
    }

}
