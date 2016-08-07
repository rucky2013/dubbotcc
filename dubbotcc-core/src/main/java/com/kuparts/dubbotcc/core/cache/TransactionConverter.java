package com.kuparts.dubbotcc.core.cache;


import com.kuparts.dubbotcc.api.Transaction;
import com.kuparts.dubbotcc.commons.exception.TccException;
import com.kuparts.dubbotcc.core.serializer.ObjectSerializer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 将事务对象转换为存入缓存对象
 *
 * @author chenbin@kuparts.com
 * @author chenbin
 * @version 1.0
 **/
public abstract class TransactionConverter<C extends TransactionCache> {
    private Transaction transaction;

    @Autowired
    private ObjectSerializer serializer;

    private C transactionCache;

    protected Transaction getTransaction() {
        return transaction;
    }

    protected ObjectSerializer getSerializer() {
        return serializer;
    }

    protected C getTransactionCache() {
        return transactionCache;
    }

    /**
     * 将transaction对象转换为可存入缓存对象
     *
     * @return
     */
    public abstract C convertToCache() throws TccException;

    public abstract Transaction convertByCache() throws TccException;

    /**
     * 初始化转换信息
     * 从事务对象转换为缓存对象
     *
     * @param transaction 事务对象
     * @return 转换器
     */
    public TransactionConverter initToCache(Transaction transaction) {
        this.transaction = transaction;
        return this;
    }

    /**
     * 初始化转换对象..
     * 从缓存对像转换为事务对象
     *
     * @param cache 缓存对象
     * @return 转换器
     */
    public TransactionConverter initByCache(C cache) {
        transactionCache = cache;
        return this;
    }
}
