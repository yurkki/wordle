package org.example.wordle.controller;

import org.example.wordle.repository.GameStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для тестирования подключения к базе данных
 */
@RestController
@RequestMapping("/api/db")
public class DatabaseTestController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private GameStatsRepository gameStatsRepository;
    
    /**
     * Проверяет подключение к базе данных
     */
    @GetMapping("/test-connection")
    public Map<String, Object> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            response.put("success", true);
            response.put("message", "Подключение к БД успешно");
            response.put("database", connection.getMetaData().getDatabaseProductName());
            response.put("version", connection.getMetaData().getDatabaseProductVersion());
            response.put("url", connection.getMetaData().getURL());
            response.put("username", connection.getMetaData().getUserName());
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Ошибка подключения к БД: " + e.getMessage());
            response.put("error", e.getSQLState());
        }
        
        return response;
    }
    
    /**
     * Проверяет работу JPA репозитория
     */
    @GetMapping("/test-repository")
    public Map<String, Object> testRepository() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long count = gameStatsRepository.count();
            response.put("success", true);
            response.put("message", "JPA репозиторий работает");
            response.put("totalRecords", count);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка JPA репозитория: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return response;
    }
    
    /**
     * Получает информацию о базе данных
     */
    @GetMapping("/info")
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            response.put("database", connection.getMetaData().getDatabaseProductName());
            response.put("version", connection.getMetaData().getDatabaseProductVersion());
            response.put("driver", connection.getMetaData().getDriverName());
            response.put("driverVersion", connection.getMetaData().getDriverVersion());
            response.put("url", connection.getMetaData().getURL());
            response.put("username", connection.getMetaData().getUserName());
            response.put("maxConnections", connection.getMetaData().getMaxConnections());
            response.put("readOnly", connection.isReadOnly());
            response.put("autoCommit", connection.getAutoCommit());
            
            // Проверяем таблицы
            try {
                long tableCount = gameStatsRepository.count();
                response.put("gameStatsCount", tableCount);
                response.put("tablesExist", true);
            } catch (Exception e) {
                response.put("tablesExist", false);
                response.put("tableError", e.getMessage());
            }
            
        } catch (SQLException e) {
            response.put("error", "Ошибка получения информации: " + e.getMessage());
        }
        
        return response;
    }
}
