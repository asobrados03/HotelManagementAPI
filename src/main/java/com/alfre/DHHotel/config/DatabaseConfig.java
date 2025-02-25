package com.alfre.DHHotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @ConfigurationProperties(prefix = "datasource.my-connection")
    public DataSource crudDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate crudJdbcTemplate(DataSource crudDataSource) {
        var jdbcTemplate = new JdbcTemplate(crudDataSource);
        return jdbcTemplate;
    }

    @Bean
    public NamedParameterJdbcTemplate crudNamedParameterJdbcTemplate(JdbcTemplate crudJdbcTemplate) {
        return new NamedParameterJdbcTemplate(crudJdbcTemplate);
    }
}
