package ar.edu.itba.paw.webapp.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ComponentScan({
    "ar.edu.itba.paw.webapp.controller",
    "ar.edu.itba.paw.services",
    "ar.edu.itba.paw.persistence"
})
@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Environment environment;

    @Autowired
    public WebConfig(final Environment environment) {
        this.environment = environment;
    }

    @Bean
    public ViewResolver viewResolver() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    @Bean
    public DataSource dataSource() {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(getRequiredRuntimeProperty("PAW_DB_URL"));
        dataSource.setUsername(getRequiredRuntimeProperty("PAW_DB_USERNAME"));
        dataSource.setPassword(getRequiredRuntimeProperty("PAW_DB_PASSWORD"));
        return dataSource;
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(final DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
        registry.addResourceHandler("/assets/**").addResourceLocations("/assets/");
    }

    private String getRequiredRuntimeProperty(final String propertyName) {
        final String value = environment.getProperty(propertyName);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required runtime property: " + propertyName);
        }

        return value;
    }
}
