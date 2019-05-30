package com.joy.lock.zkexpiration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.retry.RetryNTimes;

/**
 * @Description:
 * @Author: Joy
 * @Date: 2019-05-30 13:39
 */
public class ZkExpirationListener {

    static String zkPath = "/expiration";
    private static final String ZK_ADDRESS = "94.191.72.116:2181";

    private final static CuratorFramework client = CuratorFrameworkFactory.newClient(ZK_ADDRESS, new RetryNTimes(1, 500));

    public static void main(String[] args) throws Exception {
        client.start();
        client.getData().watched().inBackground((client,event) -> {
            if(event.getType() == CuratorEventType.DELETE){
                System.out.println("有数据进行了删除:"+ new String(event.getData()));
            }
        }).forPath(zkPath+"/key1");
    }
}
