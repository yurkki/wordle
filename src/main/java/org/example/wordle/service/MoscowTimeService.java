package org.example.wordle.service;

import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class MoscowTimeService {
    
    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public LocalDate getCurrentMoscowDate() {
        return LocalDate.now(MOSCOW_ZONE);
    }

    public LocalDateTime getCurrentMoscowDateTime() {
        return LocalDateTime.now(MOSCOW_ZONE);
    }

    public String getCurrentMoscowDateString() {
        return getCurrentMoscowDate().format(DATE_FORMATTER);
    }

    public boolean isToday(LocalDate date) {
        return date.equals(getCurrentMoscowDate());
    }

    public boolean isToday(LocalDateTime dateTime) {
        return dateTime.toLocalDate().equals(getCurrentMoscowDate());
    }

    public LocalDateTime getStartOfDay(LocalDate date) {
        return date.atStartOfDay(MOSCOW_ZONE).toLocalDateTime();
    }

    public LocalDateTime getEndOfDay(LocalDate date) {
        return date.atTime(23, 59, 59).atZone(MOSCOW_ZONE).toLocalDateTime();
    }

    public boolean isWithinDay(LocalDateTime dateTime, LocalDate date) {
        LocalDateTime startOfDay = getStartOfDay(date);
        LocalDateTime endOfDay = getEndOfDay(date);
        
        return !dateTime.isBefore(startOfDay) && !dateTime.isAfter(endOfDay);
    }

    public String getMoscowTimeInfo() {
        LocalDateTime now = getCurrentMoscowDateTime();
        LocalDate today = getCurrentMoscowDate();
        
        return String.format(
            "🕐 Московское время: %s\n📅 Дата: %s\n⏰ Время: %s",
            now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
            today.format(DATE_FORMATTER),
            now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
    }

    public boolean shouldUpdateDailyWord(LocalDate lastUpdateDate) {
        LocalDate currentMoscowDate = getCurrentMoscowDate();
        return !currentMoscowDate.equals(lastUpdateDate);
    }
}
