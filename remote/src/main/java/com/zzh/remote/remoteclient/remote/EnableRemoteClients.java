package com.zzh.remote.remoteclient.remote;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RemoteClientsRegistrar.class)
public @interface EnableRemoteClients {
    /**
     * Alias for the {@link #basePackages()} attribute.
     * @EnableRemoteClients ("org.my.pkg") instead of @EnableRemoteClients (basePackages = "org.my.pkg").
     * @return the array of 'basePackages"
     */
    @AliasFor("basePackages")
    String[] value() default {};

    /**
     * Base packages to scan for annotated {@link RemoteClient} components
     * @return the array of 'basePackages'
     */
    String[] basePackages() default {};
}
