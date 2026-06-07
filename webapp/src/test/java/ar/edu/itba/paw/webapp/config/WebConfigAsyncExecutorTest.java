package ar.edu.itba.paw.webapp.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class WebConfigAsyncExecutorTest {

    @Test
    public void mvcAsyncTaskExecutor_usesCallerRunsRejectionPolicy() {
        final WebConfig webConfig = new WebConfig(null, "jdbc:hsqldb:mem:test", "sa", "sa");

        final ThreadPoolTaskExecutor executor = webConfig.mvcAsyncTaskExecutor();

        Assertions.assertTrue(
                executor.getThreadPoolExecutor().getRejectedExecutionHandler()
                        instanceof ThreadPoolExecutor.CallerRunsPolicy);
    }
}
