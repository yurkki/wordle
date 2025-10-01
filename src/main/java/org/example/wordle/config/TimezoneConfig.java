package org.example.wordle.config;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Конфигурация часового пояса для приложения
 */
@Configuration
public class TimezoneConfig {

    @PostConstruct
    public void init() {
        // Устанавливаем системный часовой пояс на московское время
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        System.setProperty("user.timezone", "Europe/Moscow");
        
        System.out.println("🌍 Установлен часовой пояс: " + ZoneId.systemDefault());
    }
}
