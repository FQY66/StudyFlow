package com.sf.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class springDataRedisTest {
    @Autowired
    RedisTemplate redisTemplate;

    @Test
    public void testRedisTemplate(){
        System.out.println(redisTemplate);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("mytimekey","mytimevalue",10, TimeUnit.SECONDS);
        valueOperations.set("mykey1","myvalue1");
        String mykey1 = valueOperations.get("mykey1").toString();
        System.out.println(mykey1+"                  "+valueOperations.get("mytimekey").toString());
    }

    @Test
    public void testHash(){
        // 测试hash hset hget hkeys hvals
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.put("myhash","key1","value3");
        hashOperations.put("myhash","key2","value4");
        System.out.println(hashOperations.get("myhash","key1"));
        System.out.println(hashOperations.get("myhash","key2"));
        System.out.println(hashOperations.keys("myhash"));
        System.out.println(hashOperations.values("myhash"));
        Set keys = hashOperations.keys("myhash");
        System.out.println(keys);
        List values = hashOperations.values("myhash");
        System.out.println(values);
    }

    @Test
    public void testList(){
        // 测试list lpush lpop rpop llen
        ListOperations listOperations = redisTemplate.opsForList();
        listOperations.leftPush("mylist","value1");
        listOperations.leftPush("mylist","value2");
        Object index = listOperations.index("mylist", 0);
        System.out.println("mylist index 0: "+index);
        System.out.println(listOperations.range("mylist",0,1));
//        System.out.println(listOperations.leftPop("mylist"));
//        System.out.println(listOperations.rightPop("mylist"));
        Long size = listOperations.size("mylist");
        System.out.println(size);
    }
    @Test
    public void testSet(){
        //sadd smembers scard sinter sunion sdiff
        SetOperations setOperations = redisTemplate.opsForSet();
        setOperations.add("myset","value1");
        setOperations.add("myset","value2");
        System.out.println(setOperations.members("myset"));
        System.out.println(setOperations.size("myset"));
        System.out.println(setOperations.members("myset3"));
    }
}

