package com.zzh.remote.remoteclient.remote;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;
/**
 * @Author：zzh
 */
@Aspect
@Component
public class RemoteCallAOP {
    private final RemoteHandle remoteHandle;

    public RemoteCallAOP(RemoteHandle remoteHandle) {
        this.remoteHandle = remoteHandle;
    }

    @Pointcut("@within(RemoteClient)")
    public void packages() {
    }

    @Around("packages()")
    public Object execute(ProceedingJoinPoint joinPoint) throws RemoteClientException {
        try {
            final Request<Object, Object> request = RemoteAnnotationProcessor.handle(joinPoint);

            return remoteHandle.execute(request);
        } catch (Exception e) {
            throw new RemoteClientException("Failed to call remote service", e);
        }

    }
}
