package com.joy.lock.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @Description: zk客户端
 * @Author: Joy
 * @Date: 2019-04-18 18:30
 */
public class ZookeeperClient {

    public static ZooKeeper getInstance() throws IOException, InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);//countDpwnlatch表示需要等待的线程数，直到该数值变为0才会真正执行任务
        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    countDownLatch.countDown();
                }
            }
        });
        countDownLatch.await();
        return zooKeeper;
    }

}
