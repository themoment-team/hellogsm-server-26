package team.themoment.hellogsmv3.global.config;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.exception.GlobalAsyncExceptionHandler;

@EnableAsync
@Configuration
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final GlobalAsyncExceptionHandler globalAsyncExceptionHandler;

    @Override
    @Bean(name = "discordTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("Discord-Task");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return globalAsyncExceptionHandler;
    }
}
