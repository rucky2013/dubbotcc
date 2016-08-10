package com.kuparts.dubbotcc.core.rollback;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.rpc.RpcResult;
import com.kuparts.dubbotcc.commons.api.TccResponse;
import com.kuparts.dubbotcc.commons.api.TccServicePoint;
import com.kuparts.dubbotcc.commons.api.Transaction;
import com.kuparts.dubbotcc.commons.bean.BeanServiceUtils;
import com.kuparts.dubbotcc.commons.config.ApplicationConfigCache;
import com.kuparts.dubbotcc.commons.emuns.ServicePointStatus;
import com.kuparts.dubbotcc.commons.exception.TccException;
import com.kuparts.dubbotcc.commons.utils.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 执行回滚动作,并根据用户的设置进行,回滚结果回调...
 *
 * @author chenbin@kuparts.com
 * @author chenbin
 * @version 1.0
 **/
public class RollbackAction implements Action {
    private static final Logger LOG = LoggerFactory.getLogger(RollbackAction.class);

    @Override
    public boolean execute(Transaction transaction) {

        final TccServicePoint[] failurePoint = {new TccServicePoint()};
        transaction.getPotins().stream().forEach(point -> {
            //回滚执行器
            if (point.getStatus() == ServicePointStatus.SUCCESS) {
                CompletableFuture.supplyAsync(new Worker(point)).thenAccept(result -> {
                    //rollback执行完毕,
                    result.setFailureId(failurePoint[0].getPointId());//设置错误的point
                    //开始回调客户自身业务
                    try {
                        BeanServiceUtils.getInstance().getBean(CallbackService.class)
                                .execute(transaction.getCallback(), result);
                        LOG.info("rollback result:" + result);
                    } catch (Exception ex) {
                        LOG.error("callback method error." + ex.getMessage());
                    }
                });
            } else if (point.getStatus() == ServicePointStatus.FAILURE) {
                failurePoint[0] = point;
            }
        });
        return Boolean.TRUE;
    }

    /**
     * 具体的事务回调实现.
     */
    class Worker implements Supplier<TccResponse> {

        private TccServicePoint point;

        private Worker(TccServicePoint point) {
            this.point = point;
        }

        @Override
        public TccResponse get() {
            LOG.info("start executing  rollback transaction" + point.getTransId() + "rollback:" + point.getRollbackInvocation().getMethod());
            Assert.notNull(point);
            TccResponse response = new TccResponse();
            try {
                ReferenceConfig reference = ApplicationConfigCache.getInstance().get(point.getRollbackInvocation().getTargetClazz().getName());
                reference.setInterface(point.getRollbackInvocation().getTargetClazz());
                reference.setVersion(point.getVersion());
                reference.setRetries(0);
                reference.setGroup(point.getGroup());
                reference.setTimeout(4000);
                RpcResult result = invoker(reference);//开始调用
                response.setPointId(point.getPointId());
                response.setError(result.getException());
                response.setSuccessful(!result.hasException());
                response.setArgs(point.getRollbackInvocation().getArgumentValues());
                response.setInterfaceName(point.getRollbackInvocation().getTargetClazz().getName());
                response.setRollbackMethod(point.getRollbackInvocation().getMethod());
                response.setObj(result.getValue());
                response.setPort(point.getPort());
                response.setRemoteAddress(point.getRemoteAddress());
                response.setTransId(point.getTransId());
                LOG.info("rollback transaction successful ,transaction：" + point.getTransId() + ",rollback:" + point.getRollbackInvocation().getMethod());
            } catch (TccException e) {
                LOG.error("rollback transaction failure ," +
                        " get application config failure " + point.getRollbackInvocation().getMethod() + "," + e.getMessage());
                response.setError(e);
                response.setSuccessful(false);
            }
            return response;
        }
        /**
         * 调用事务补偿方法
         *
         * @param config dubbo引用配置
         * @return 远程调用结果
         */
        private RpcResult invoker(ReferenceConfig config) {
            Object targetInstance = config.get();
            RpcResult result = new RpcResult();
            try {
                Method method = targetInstance.getClass().getDeclaredMethod(point.getRollbackInvocation().getMethod(), point.getRollbackInvocation().getArgumentTypes());
                Object obj = method.invoke(targetInstance, point.getRollbackInvocation().getArgumentValues());
                result.setValue(obj);
            } catch (NoSuchMethodException e) {
                result.setException(e);
                LOG.error("rollback transaction failure ," +
                        "no such rollback method " + point.getRollbackInvocation().getMethod() + "," + e.getMessage());
            } catch (InvocationTargetException | IllegalAccessException e) {
                result.setException(e);
                LOG.error("rollback transaction failure ," +
                        "invoke rollback method failure:" + point.getRollbackInvocation().getMethod() + "," + e.getMessage());
            } catch (RuntimeException e) {
                result.setException(e);
                LOG.error("rollback transaction failure ," + point.getRollbackInvocation().getMethod() + "," + e.getMessage());
            }
            return result;
        }
    }
}
