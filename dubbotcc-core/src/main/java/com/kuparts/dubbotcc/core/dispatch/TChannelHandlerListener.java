package com.kuparts.dubbotcc.core.dispatch;

import java.util.EventListener;

/**
 * @author chenbin@kuparts.com
 * @author chenbin
 * @version 1.0
 **/
public interface TChannelHandlerListener extends EventListener {
    void operationComplete(TFuture future) throws Exception;
}