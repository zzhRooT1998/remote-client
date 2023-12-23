package com.zzh.remote.remoteclient.remote;

import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.assertj.core.internal.bytebuddy.description.annotation.AnnotationDescription;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @Author：zzh
 */
@SuppressWarnings("unchecked")
public enum RemoteAnnotationProcessor {
    PATH_VARIABLE(PathVariable.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            final PathVariable pathVariable = (PathVariable) annotationHolder.annotation;
            String value = pathVariable.value();

            request.pathVariable(!StringUtils.hasText(value) ? annotationHolder.parameterName : value, annotationHolder.arg);
        }
    },
    REQUEST_PARAM(RequestParam.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            final RequestParam requestParam = (RequestParam) annotationHolder.annotation;
            String value = requestParam.value();

            request.param(!StringUtils.hasText(value) ? annotationHolder.parameterName : value, annotationHolder.arg);
        }
    },
    REQUEST_BODY(RequestBody.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            request.body(annotationHolder.arg);
        }
    },
    REQUEST_HEADER(RequestHeader.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            final RequestHeader header = (RequestHeader) annotationHolder.annotation;

            String value = header.value();
            Object arg = annotationHolder.arg;
            final String argValue = Objects.isNull(arg) ? null : String.valueOf(arg);

            request.header(!StringUtils.hasText(value) ? annotationHolder.parameterName : value, argValue);
        }
    }
    ,
    REQUEST_MAPPING(RequestMapping.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            RequestMapping requestMapping = (RequestMapping) annotationHolder.annotation;
            RequestMethod[] methods = requestMapping.method();
            Assert.isTrue(methods.length == 1, "You merely can defined one request method");

            String[] value = requestMapping.value();
            String[] produces = requestMapping.produces();
            String[] consumes = requestMapping.consumes();

            handleMappingAnnotation(request, produces, consumes, value);
            request.method(methods[0]);
        }
    },
    POST_MAPPING(PostMapping.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            PostMapping postMapping = (PostMapping) annotationHolder.annotation;

            String[] value = postMapping.value();
            String[] produces = postMapping.produces();
            String[] consumes = postMapping.consumes();

            handleMappingAnnotation(request, produces, consumes, value);
            request.post();
        }
    },
    GET_MAPPING(GetMapping.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            GetMapping getMapping = (GetMapping) annotationHolder.annotation;

            String[] value = getMapping.value();
            String[] produces = getMapping.produces();
            String[] consumes = getMapping.consumes();

            handleMappingAnnotation(request, produces, consumes, value);
            request.put();
        }
    },
    PUT_MAPPING(PutMapping.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            PutMapping putMapping = (PutMapping) annotationHolder.annotation;

            String[] value = putMapping.value();
            String[] produces = putMapping.produces();
            String[] consumes = putMapping.consumes();

            handleMappingAnnotation(request, produces, consumes, value);
            request.put();
        }
    },
    DELETE_MAPPING(DeleteMapping.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            DeleteMapping deleteMapping = (DeleteMapping) annotationHolder.annotation;

            String[] value = deleteMapping.value();
            String[] produces = deleteMapping.produces();
            String[] consumes = deleteMapping.consumes();

            handleMappingAnnotation(request, produces, consumes, value);
            request.delete();
        }
    },
    REMOTE_CLIENT(RemoteClient.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            RemoteClient remoteClient = (RemoteClient) annotationHolder.annotation;
            String remoteClientValue = remoteClient.value();
            String responseDataField = remoteClient.responseDataField();

            request.service(remoteClientValue);
            request.responseDataField(responseDataField);
        }
    },
    RESPONSE_HANDLE(ResponseHandle.class) {
        @Override
        void handle(AnnotationHolder annotationHolder, HttpRequest<Object, Object> request) {
            ResponseHandle responseHandle = (ResponseHandle) annotationHolder.annotation;
            Class<? extends ResponseHandler> handle = responseHandle.handle();
            request.responseHandler((Class<ResponseHandler<Object>>) handle);
        }
    };

    public void handleMappingAnnotation(HttpRequest<Object, Object> request, String[] produces, String[] consumes, String[] value) {
        Assert.isTrue(value.length == 1, COMMON_ERR_MSG);

        request.header(HttpRequest.ACCEPT, produces).header(HttpRequest.CONTENT_TYPE, consumes).path(value[0]);
    }


    public static final String COMMON_ERR_MSG = "You merely can defined one request path";
    private static final Map<Class<?>, RemoteAnnotationProcessor> ANNOTATION_PROCESSOR_MAP = new HashMap<>();

    static {
        Arrays.stream(RemoteAnnotationProcessor.values())
                .forEach(remoteAnnotationProcessor -> ANNOTATION_PROCESSOR_MAP.put(remoteAnnotationProcessor.annotationType, remoteAnnotationProcessor));
    }
    final Class<?> annotationType;
    RemoteAnnotationProcessor(Class<?> annotationType) {
        this.annotationType = annotationType;
    }

    abstract void handle(@NonNull AnnotationHolder annotationHolder, @NonNull HttpRequest<Object, Object> request);

    public static Request<Object, Object> handle(ProceedingJoinPoint joinPoint) {
        AnnotationHolders annotationHolders = AnnotationHoldersFactory.getAnnotationHolders(joinPoint);
        annotationHolders = annotationHolders.fillArgs(joinPoint.getArgs());

        final HttpRequest<Object, Object> request = handleAnnotation(annotationHolders);

        //Get target method return class type
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getMethod().getReturnType();
        request.responseType((Class<Object>) returnType);

        return request;
    }

    private static HttpRequest<Object, Object> handleAnnotation(AnnotationHolders annotationHolders) {
        HttpRequest<Object, Object> request = new HttpRequest<>();

        Stream.of(annotationHolders.parameterAnnotationHolderList, annotationHolders.classAndMethodAnnotationHolderList)
                .flatMap(Collection::stream)
                .forEach(annotationHolder -> {
                    Annotation annotation = annotationHolder.annotation;
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    if (!ANNOTATION_PROCESSOR_MAP.containsKey(annotationType)) {
                        return;
                    }

                    RemoteAnnotationProcessor remoteAnnotationProcessor = ANNOTATION_PROCESSOR_MAP.get(annotationType);
                    remoteAnnotationProcessor.handle(annotationHolder, request);
                });

        return request;
    }

    @NoArgsConstructor
    static class AnnotationHolders {
        public static final RequestParam DEFAULT_REQUEST_PARAM = AnnotationDescription.AnnotationInvocationHandler.of(RequestParam.class.getClassLoader(), RequestParam.class, new HashMap<>());

        private final List<AnnotationHolder> parameterAnnotationHolderList = new ArrayList<>();
        private List<AnnotationHolder> classAndMethodAnnotationHolderList = new ArrayList<>();

        public AnnotationHolders(Parameter[] parameters) {
            for (Parameter parameter : parameters) {
                Annotation[] parameterAnnotations = parameter.getAnnotations();
                if (ObjectUtils.isEmpty(parameterAnnotations)) {
                    parameterAnnotations = new Annotation[]{DEFAULT_REQUEST_PARAM};
                }

                Annotation parameterAnnotation = parameterAnnotations[0];
                String name = parameter.getName();
                parameterAnnotationHolderList.add(new AnnotationHolder(parameterAnnotation, name));
            }
        }
        public void addForClassOrMethod(Annotation annotation) {
            classAndMethodAnnotationHolderList.add(new AnnotationHolder(annotation));
        }

        public AnnotationHolders fillArgs(Object[] args) {
            if (Objects.isNull(args)) {
                return this;
            }

            AnnotationHolders annotationHolders = new AnnotationHolders();
            List<AnnotationHolder> parameterAnnotationHolderList = this.parameterAnnotationHolderList;
            annotationHolders.classAndMethodAnnotationHolderList = this.classAndMethodAnnotationHolderList;

            IntStream.range(0, args.length).forEach(index -> {
                Object arg = args[index];
                AnnotationHolder annotationHolder = parameterAnnotationHolderList.get(index);

                annotationHolders.parameterAnnotationHolderList.add(annotationHolder.fillArg(arg));
            });

            return annotationHolders;
        }
    }



    static class AnnotationHolder {
        private Object arg;
        private String parameterName;
        private final Annotation annotation;

        public AnnotationHolder(Annotation annotation) {
            this.annotation = annotation;
        }

        public AnnotationHolder(Annotation annotation, String parameterName) {
            this.annotation = annotation;
            this.parameterName = parameterName;
        }

        public AnnotationHolder(Annotation annotation, Object arg, String parameterName) {
            this.arg = arg;
            this.annotation = annotation;
            this.parameterName = parameterName;
        }

        protected AnnotationHolder fillArg(Object arg) {
            return new AnnotationHolder(annotation, arg, parameterName);
        }
    }
}
