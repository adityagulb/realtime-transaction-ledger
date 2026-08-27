package com.aditya.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(
        name = "app.sink",
        havingValue = "snowflake"
)
public class SnowflakeConfig {

    @Bean
    public DataSource snowflakeDataSource(
            @Value("${snowflake.url}") String url,
            @Value("${snowflake.username}") String username,
            @Value("${snowflake.password}") String password) {

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("net.snowflake.client.jdbc.SnowflakeDriver");

        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource snowflakeDataSource) {
        return new JdbcTemplate(snowflakeDataSource);
    }
}
