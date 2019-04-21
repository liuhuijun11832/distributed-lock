package com.joy.lock.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 真正的锁实现
 * @Author: Joy
 * @Date: 2019-04-20 10:45
 */
@Slf4j
public class DistributedLock {

    /**
     * zookeeper分布式锁原理：
     * 节点有序性：节点可以设置为有序的，例如node-1,node-2等
     * 临时节点：超时以后自动删除避免死锁
     * 事件监听：节点变化时客户端可以收到
     */

    private static final String ROOT_LOCK = "/LOCKS";
    private ZooKeeper zooKeeper;
    private int sessionTimeout;
    private String lockId;
    private final static byte[] data = {1, 2};
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public DistributedLock() throws IOException, InterruptedException {
        this.zooKeeper = ZookeeperClient.getInstance();
        this.sessionTimeout = ZookeeperClient.getSessionTimeout();
    }

    public boolean tryGetDistributedLock() {
        try {
            //这里的四个参数分别是：路径，保存内容，权限，临时有序节点
            lockId = zooKeeper.create(ROOT_LOCK + "/", data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("当前线程:{} 创建节点,id={}", Thread.currentThread().getName(), lockId);
            List<String> childrenList = zooKeeper.getChildren(ROOT_LOCK, true);
            childrenList.sort(String::compareTo);
            for (int i = 0; i < childrenList.size(); i++) {
                childrenList.set(i, ROOT_LOCK + "/" + childrenList.get(i));
            }
            String first = childrenList.get(0);
            if (lockId.equals(first)) {
                log.info("当前线程:{} 获取锁成功,节点为:{}", Thread.currentThread().getName(), lockId);
                return true;
            }
            List<String> lessThanLockIDList = childrenList.subList(0, childrenList.indexOf(lockId));
            if (!lessThanLockIDList.isEmpty()) {
                String preLockID = lessThanLockIDList.get(lessThanLockIDList.size() - 1);
                //监听上一节点变化,如果删除在监听器里会将countDownLatch减1，这样就能执行挂起的客户端
                zooKeeper.exists(preLockID, new LockWatcher(countDownLatch));
                //使用countDownLatch闭锁来挂起当前线程直到lockWatcher监听到上一节点的变化countDown了或者超时sessionTimeout以后
                countDownLatch.await(sessionTimeout, TimeUnit.MILLISECONDS);
                log.info("当前线程:{} 获取锁成功,节点为:{}", Thread.currentThread().getName(), lockId);
            }
            return true;
        } catch (Exception e) {
            log.error("获取锁异常",e);
        }
        return false;
    }

    public boolean releaseDistributedLock(){
        log.info("当前线程:{} 将要释放锁:{}", Thread.currentThread().getName(), lockId);
        try {
            zooKeeper.delete(lockId, -1);
            log.info("当前线程:{} 释放锁:{} 成功", Thread.currentThread().getName(), lockId);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        final CountDownLatch countDownLatch = new CountDownLatch(10);
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                DistributedLock distributedLock = null;
                try {
                    distributedLock = new DistributedLock();
                    countDownLatch.countDown();
                    countDownLatch.await();
                    distributedLock.tryGetDistributedLock();
                    Thread.sleep(random.nextInt(500));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (distributedLock != null) {
                        distributedLock.releaseDistributedLock();
                    }
                }
            }).start();
        }
    }
}
