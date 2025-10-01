package org.example.wordle.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() throws URISyntaxException {
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set");
        }

        logger.info("Original DATABASE_URL: {}", databaseUrl);

        // Railway предоставляет URL в формате postgresql://user:password@host:port/database
        // Spring Boot ожидает jdbc:postgresql://user:password@host:port/database
        String jdbcUrl = databaseUrl.replaceFirst("^postgresql://", "jdbc:postgresql://");
        
        logger.info("Converted JDBC URL: {}", jdbcUrl);
        
        // Парсим URL для извлечения компонентов
        URI dbUri = new URI(databaseUrl);
        String userInfo = dbUri.getUserInfo();
        String username = "";
        String password = "";
        
        if (userInfo != null && userInfo.contains(":")) {
            String[] credentials = userInfo.split(":", 2);
            username = credentials[0];
            password = credentials[1];
            logger.info("Parsed username: {}, password: [HIDDEN]", username);
        }
        
        // Создаем DataSource БЕЗ username/password в URL, только в отдельных параметрах
        // Это предотвращает проблемы с парсингом URL
        String cleanJdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();
        logger.info("Clean JDBC URL (without credentials): {}", cleanJdbcUrl);
        
        org.springframework.boot.jdbc.DataSourceBuilder<?> builder = 
            org.springframework.boot.jdbc.DataSourceBuilder.create();
        
        builder.url(cleanJdbcUrl);
        if (!username.isEmpty()) {
            builder.username(username);
        }
        if (!password.isEmpty()) {
            builder.password(password);
        }
        builder.driverClassName("org.postgresql.Driver");
        
        logger.info("DataSource configured with clean URL: {}", cleanJdbcUrl);
        
        return builder.build();
    }
}
