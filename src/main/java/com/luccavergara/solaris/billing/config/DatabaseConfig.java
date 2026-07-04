package com.luccavergara.solaris.billing.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (!StringUtils.hasText(url) || url.contains("${")) {
            throw new IllegalStateException(
                    "DATABASE_URL is not configured. Copy .env.example to .env or export DATABASE_URL "
                            + "(must start with jdbc:postgresql://)."
            );
        }

        if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
            url = "jdbc:" + url;
        }

        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(url)
                .username(properties.getUsername())
                .password(properties.getPassword())
                .build();
    }
}
