package ar.edu.itba.paw.webapp.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
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
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;

    public WebConfig(
            @Value("${db.url}") final String databaseUrl,
            @Value("${db.username}") final String databaseUsername,
            @Value("${db.password}") final String databasePassword) {
        this.databaseUrl = requireNonBlank(databaseUrl, "db.url");
        this.databaseUsername = requireNonBlank(databaseUsername, "db.username");
        this.databasePassword = requireNonBlank(databasePassword, "db.password");
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
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(databaseUsername);
        dataSource.setPassword(databasePassword);
        return dataSource;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PlatformTransactionManager transactionManager(final DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
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
        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
        registry.addResourceHandler("/assets/**").addResourceLocations("/assets/");
    }

    @Override
    public Validator getValidator() {
        return validator();
    }

    private String requireNonBlank(final String value, final String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + propertyName);
        }
        return value;
    }
}
