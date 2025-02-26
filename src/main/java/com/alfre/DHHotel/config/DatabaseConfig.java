package com.alfre.DHHotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration class for setting up database connections.
 * This class defines beans for {@link DataSource}, {@link JdbcTemplate},
 * and {@link NamedParameterJdbcTemplate} to facilitate database interactions.
 *
 * <p>It uses Spring Boot's {@code @ConfigurationProperties} to load properties
 * prefixed with {@code datasource.my-connection} from the application configuration.</p>
 *
 * @author Alfredo Sobrados González
 */
@Configuration
public class DatabaseConfig {

    /**
     * Creates and configures a {@link DataSource} bean.
     * The properties for this data source are loaded from the configuration file
     * using the prefix {@code datasource.my-connection}.
     *
     * @return a configured {@code DataSource} instance
     */
    @Bean
    @ConfigurationProperties(prefix = "datasource.my-connection")
    public DataSource crudDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Creates a {@link JdbcTemplate} bean for executing SQL queries.
     *
     * @param crudDataSource the configured data source
     * @return an instance of {@code JdbcTemplate}
     */
    @Bean
    public JdbcTemplate crudJdbcTemplate(DataSource crudDataSource) {
        return new JdbcTemplate(crudDataSource);
    }

    /**
     * Creates a {@link NamedParameterJdbcTemplate} bean to allow
     * named parameters in SQL queries.
     *
     * @param crudJdbcTemplate the {@code JdbcTemplate} instance
     * @return an instance of {@code NamedParameterJdbcTemplate}
     */
    @Bean
    public NamedParameterJdbcTemplate crudNamedParameterJdbcTemplate(JdbcTemplate crudJdbcTemplate) {
        return new NamedParameterJdbcTemplate(crudJdbcTemplate);
    }
}
