package com.zzh.remote.remoteclient.remote;

/**
 * @Author：zzh
 */

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public class AnnotationHoldersFactory {

    private AnnotationHoldersFactory() {
    }

    private static final Map<String, RemoteAnnotationProcessor.AnnotationHolders> ANNOTATION_HOLDERS_MAP = new ConcurrentHashMap<>();

    public static RemoteAnnotationProcessor.AnnotationHolders getAnnotationHolders(ProceedingJoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method targetMethod = signature.getMethod();
        final String targetMethodString = targetMethod.toString();

        if (ANNOTATION_HOLDERS_MAP.containsKey(targetMethodString)) {
            return ANNOTATION_HOLDERS_MAP.get(targetMethodString);

        }
        return createAnnotationHolders(joinPoint);
    }

    private static synchronized RemoteAnnotationProcessor.AnnotationHolders createAnnotationHolders(ProceedingJoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method targetMethod = signature.getMethod();
        final String targetMethodString = targetMethod.toString();


        //Double-check to prevent multiple instances from being created for a single key
        if (ANNOTATION_HOLDERS_MAP.containsKey(targetMethodString)) {
            return ANNOTATION_HOLDERS_MAP.get(targetMethodString);
        }

        final Parameter[] parameters = targetMethod.getParameters();
        final RemoteAnnotationProcessor.AnnotationHolders annotationHolders = new RemoteAnnotationProcessor.AnnotationHolders(parameters);

        final Class<?> targetInterface = signature.getDeclaringType();
        final RemoteClient remoteClient = targetInterface.getAnnotation(RemoteClient.class);
        annotationHolders.addForClassOrMethod(remoteClient);

        Annotation[] annotations = targetMethod.getAnnotations();
        Assert.isTrue(!ObjectUtils.isEmpty(annotations), "Ensure that at least defined one annotation on the method");

        Arrays.stream(annotations).forEach(annotationHolders::addForClassOrMethod);

        ANNOTATION_HOLDERS_MAP.put(targetMethodString, annotationHolders);
        return annotationHolders;
    }
}
