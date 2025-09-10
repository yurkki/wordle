# 🔧 Исправление ошибки с переменной PORT

## Проблема
```
Invalid value '$PORT' for configuration property 'server.port'
Failed to convert to type java.lang.Integer
```

## Причина
Spring Boot не может преобразовать переменную `$PORT` в число из-за конфликта настроек.

## ✅ Решение

### 1. Убрана конфликтующая настройка порта
- Удален `server.port=${PORT:8080}` из `application.properties`
- Railway автоматически установит порт через переменную окружения

### 2. Создан специальный профиль для Railway
- `application-railway.properties` - конфигурация только для Railway
- Без конфликтующих настроек порта
- Оптимизированные настройки для production

### 3. Упрощена команда запуска
- Убран параметр `--server.port=$PORT`
- Оставлен только `--server.address=0.0.0.0`
- Используется Railway профиль

## 🚀 Как применить исправление

### Вариант 1: Использовать исправленный Dockerfile
```bash
git add .
git commit -m "Fix PORT variable conflict"
git push origin main
```

### Вариант 2: Использовать рабочий Dockerfile
```bash
# Переименуйте файлы
mv Dockerfile Dockerfile.old
mv Dockerfile.working Dockerfile

# Закоммитьте изменения
git add .
git commit -m "Use working Dockerfile for Railway"
git push origin main
```

## 🔍 Проверка исправления

### Локальное тестирование
```bash
# Сборка
./gradlew clean build

# Запуск с Railway профилем
java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar --spring.profiles.active=railway --server.address=0.0.0.0

# Проверка
curl http://localhost:8080/
```

### Ожидаемый результат
- ✅ Приложение запускается без ошибок
- ✅ Слушает порт 8080 (или PORT от Railway)
- ✅ Доступно по адресу 0.0.0.0
- ✅ Healthcheck проходит успешно

## 📋 Что было изменено

### 1. application.properties
```properties
# УДАЛЕНО: server.port=${PORT:8080}
# Оставлены только базовые настройки
```

### 2. application-railway.properties (новый файл)
```properties
# Специальная конфигурация для Railway
# Без настройки server.port
# Railway установит порт автоматически
```

### 3. railway.json
```json
{
  "startCommand": "java -jar app.jar --spring.profiles.active=railway --server.address=0.0.0.0"
}
```

### 4. Dockerfile
```dockerfile
# Упрощенная команда запуска
CMD ["sh", "-c", "java -jar app.jar --server.address=0.0.0.0"]
```

### 5. Dockerfile.working (альтернативный)
```dockerfile
# Простой рабочий Dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test
CMD ["sh", "-c", "java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar --spring.profiles.active=railway --server.address=0.0.0.0"]
```

## 🎯 Результат

После применения исправления:
- ✅ Нет конфликтов с переменной PORT
- ✅ Railway автоматически устанавливает порт
- ✅ Приложение запускается успешно
- ✅ Healthcheck проходит

## 🆘 Если проблема остается

### 1. Проверьте логи Railway
- Должны быть логи запуска Spring Boot
- Не должно быть ошибок с PORT
- Должны быть логи Tomcat

### 2. Попробуйте альтернативный Dockerfile
```bash
mv Dockerfile Dockerfile.old
mv Dockerfile.working Dockerfile
git add . && git commit -m "Use alternative Dockerfile" && git push
```

### 3. Проверьте переменные окружения
- Railway должен установить PORT автоматически
- Проверьте в панели Railway

## 📞 Поддержка

Если проблема не решается:
1. Проверьте логи в Railway Dashboard
2. Убедитесь, что все изменения загружены
3. Попробуйте альтернативный Dockerfile
4. Проверьте, что приложение запускается локально
