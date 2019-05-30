package com.joy.lock.zkexpiration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @Author: Joy
 * @Date: 2019-05-29 9:25
 */
public class ZkExpirationTest {

    private final static Logger log = LoggerFactory.getLogger(ZkExpirationTest.class);

    static String zkPath = "/expiration";
    private static final String ZK_ADDRESS = "94.191.72.116:2181";
    CountDownLatch countDownLatch = new CountDownLatch(1);
    private final static CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_ADDRESS, new RetryNTimes(1, 500));

    public static void main(String[] args) throws Exception {
        client.start();
        log.info("State:{}",client.getState());
        client.create().inBackground().forPath(zkPath);
        client.create().withMode(CreateMode.EPHEMERAL).inBackground().forPath(zkPath+"/key1");
        TimeUnit.SECONDS.sleep(60);
    }

}
