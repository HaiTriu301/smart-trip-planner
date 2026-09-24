package com.trieu.tripplanner.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Turns on @Async for MailService. Runs on Boot's applicationTaskExecutor, which uses virtual threads because
 * spring.threads.virtual.enabled=true (design.md 3.1). A void @Async method that throws would otherwise fail
 * silently; the handler below logs it with context so a dead SMTP is visible (CLAUDE.md rule 12).
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncTaskExecutor applicationTaskExecutor;

    public AsyncConfig(@Qualifier("applicationTaskExecutor") AsyncTaskExecutor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Async {}.{} failed: {}",
                method.getDeclaringClass().getSimpleName(), method.getName(), ex.getMessage(), ex);
    }

}
