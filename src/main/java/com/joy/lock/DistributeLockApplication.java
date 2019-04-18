package com.joy.lock;

import com.joy.lock.redis.RedisPool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.UUID;

@SpringBootApplication
@RestController
public class DistributeLockApplication {

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @GetMapping("/")
    public String test(){
        String result = "fail";
        String uuid = UUID.randomUUID().toString();
        threadLocal.set(uuid);
        if (RedisPool.tryGetDistributedLock(jedis(),"test", threadLocal.get(),600000)) result = "ok";
        return result;
    }

    @GetMapping("/cancel")
    public String cancel(){
        String result = "fail";
        if(RedisPool.releaseDistributedLock(jedis(),"test",threadLocal.get())) result = "ok";
        return result;
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
