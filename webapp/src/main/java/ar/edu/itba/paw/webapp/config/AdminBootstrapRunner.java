package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.services.AdminBootstrapService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapService adminBootstrapService;
    private final AtomicBoolean executed = new AtomicBoolean(false);

    public AdminBootstrapRunner(final AdminBootstrapService adminBootstrapService) {
        this.adminBootstrapService = adminBootstrapService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        if (executed.compareAndSet(false, true)) {
            LOGGER.info("Admin bootstrap runner triggered");
            adminBootstrapService.bootstrapFromConfiguration();
            LOGGER.info("Admin bootstrap runner completed");
        }
    }
}
