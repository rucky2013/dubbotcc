package com.kp.dubbotcc.core.cache.mongo;

import com.kp.dubbotcc.api.TccServicePoint;
import com.kp.dubbotcc.api.Transaction;
import com.kp.dubbotcc.commons.emuns.TransactionStatus;
import com.kp.dubbotcc.commons.exception.TccException;
import com.kp.dubbotcc.core.cache.TransactionConverter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * mongo存入数据封装
 * @author chenbin@kuparts.com
 * @author chenbin
 * @version 1.0
 **/
public class MongoTransactionConverter extends TransactionConverter<MongoTransactionCache> {


    @Override
    public MongoTransactionCache convertToCache() throws TccException {
        MongoTransactionCache cache = new MongoTransactionCache();
        cache.setStatus(getTransaction().getStatus().name());
        cache.setTransId(getTransaction().getTransId());
        cache.setStartTime(getTransaction().getStartTime());
        byte[] bytes = getSerializer().serialize(getTransaction().getPotins());
        cache.setContents(bytes);
        return cache;
    }

    @Override
    public Transaction convertByCache() throws TccException {
        MongoTransactionCache cache = getTransactionCache();
        TransactionStatus status = TransactionStatus.valueOf(cache.getStatus());
        List<TccServicePoint> list = getSerializer()
                .deSerialize(cache.getContents(), CopyOnWriteArrayList.class);
        return new Transaction(
                cache.getTransId(),
                cache.getStartTime(),
                status, list);
    }
}
