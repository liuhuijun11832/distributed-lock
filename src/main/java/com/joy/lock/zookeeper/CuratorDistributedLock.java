package com.joy.lock.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Description: curator实现zk分布式锁
 * @Author: Joy
 * @Date: 2019-04-20 18:38
 */
@Slf4j
public class CuratorDistributedLock {

    private static final String ZK_ADDRESS = "127.0.0.1:2181";

    private static final String ROOT_LOCK = "/LOCKS";

    static CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_ADDRESS, new RetryNTimes(10, 500));
    static InterProcessMutex lock = new InterProcessMutex(client, ROOT_LOCK);

    public static void tryGetDistributedLock() {
        try {
            if (lock.acquire(10 * 10000, TimeUnit.MILLISECONDS)) {
                log.info("当前线程:{}获取锁",Thread.currentThread().getName());
                Thread.sleep(5000L);
                CuratorDistributedLock.releaseDistributedLock();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void releaseDistributedLock() throws Exception {
        log.info("当前线程:{}释放锁",Thread.currentThread().getName());
        lock.release();
    }

    public static void main(String[] args) {
        final CountDownLatch countDownLatch = new CountDownLatch(3);
        client.start();
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                countDownLatch.countDown();
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CuratorDistributedLock.tryGetDistributedLock();
            }).start();
        }
    }
}
