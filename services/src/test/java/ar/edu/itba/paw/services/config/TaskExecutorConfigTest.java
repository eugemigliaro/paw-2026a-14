package ar.edu.itba.paw.services.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class TaskExecutorConfigTest {

    @Test
    public void mailTaskExecutor_usesCallerRunsRejectionPolicy() {
        final Executor executor = new MailConfiguration().mailTaskExecutor();

        final ThreadPoolExecutor pool = ((ThreadPoolTaskExecutor) executor).getThreadPoolExecutor();

        Assertions.assertTrue(
                pool.getRejectedExecutionHandler() instanceof ThreadPoolExecutor.CallerRunsPolicy);
    }

    @Test
    public void matchRecurrenceTaskExecutor_usesCallerRunsRejectionPolicy() {
        final Executor executor = new RecurringMatchesConfiguration().matchRecurrenceTaskExecutor();

        final ThreadPoolExecutor pool = ((ThreadPoolTaskExecutor) executor).getThreadPoolExecutor();

        Assertions.assertTrue(
                pool.getRejectedExecutionHandler() instanceof ThreadPoolExecutor.CallerRunsPolicy);
    }
}
