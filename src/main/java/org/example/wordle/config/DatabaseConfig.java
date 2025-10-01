package org.example.wordle.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Конфигурация базы данных для Railway.
 * Преобразует DATABASE_URL из формата postgresql:// в jdbc:postgresql://
 */
@Configuration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "railway")
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() throws URISyntaxException {
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }

        // Railway предоставляет URL в формате postgresql://user:password@host:port/database
        // Spring Boot ожидает jdbc:postgresql://user:password@host:port/database
        String jdbcUrl = databaseUrl.replaceFirst("^postgresql://", "jdbc:postgresql://");
        
        // Парсим URL для извлечения компонентов
        URI dbUri = new URI(databaseUrl);
        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        
        // Создаем DataSource с правильным URL и отдельными параметрами
        org.springframework.boot.jdbc.DataSourceBuilder<?> builder = 
            org.springframework.boot.jdbc.DataSourceBuilder.create();
        
        builder.url(jdbcUrl);
        builder.username(username);
        builder.password(password);
        builder.driverClassName("org.postgresql.Driver");
        
        return builder.build();
    }
}
