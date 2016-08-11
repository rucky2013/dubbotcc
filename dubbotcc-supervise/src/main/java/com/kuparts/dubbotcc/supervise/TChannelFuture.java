package com.kuparts.dubbotcc.supervise;

/**
 * @author chenbin@kuparts.com
 * @author chenbin
 * @version 1.0
 **/
public interface TChannelFuture {
    boolean isConnected();

    TChannel getChannel();

    boolean awaitUninterruptibly(long timeoutMillis);

    boolean isDone();

    Throwable cause();
}
