# 🔧 Исправление запуска Spring Boot приложения

## Проблема
Spring Boot приложение не может запуститься на Railway.

## ✅ Решение

### 1. Упрощена конфигурация
- Убраны сложные настройки из `application.properties`
- Оставлены только базовые настройки
- Убран конфликт с переменной PORT

### 2. Исправлен конфликт endpoints
- Убран дублирующий `@GetMapping("/")` 
- Создан отдельный `/status` endpoint
- Исправлен healthcheck path

### 3. Упрощен Dockerfile
- Убраны сложные команды
- Простая сборка и запуск
- Прямой запуск JAR файла

### 4. Обновлена конфигурация Railway
- Исправлен путь к JAR файлу
- Исправлен healthcheck path
- Упрощена команда запуска

## 🚀 Как применить исправление

### 1. Закоммитьте изменения
```bash
git add .
git commit -m "Fix Spring Boot startup issues"
git push origin main
```

### 2. Перезапустите деплой на Railway
- Зайдите в панель Railway
- Выберите ваш проект
- Нажмите "Redeploy"

## 🔍 Проверка локально

### Тестирование
```bash
# Сделайте скрипт исполняемым
chmod +x test-app.sh

# Запустите тест
./test-app.sh
```

### Ручное тестирование
```bash
# Сборка
./gradlew clean build

# Запуск
java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar

# Проверка endpoints
curl http://localhost:8080/
curl http://localhost:8080/status
curl http://localhost:8080/health
```

## 📋 Что было изменено

### 1. application.properties
```properties
# Упрощенная конфигурация
spring.application.name=wordle
server.port=8080
spring.thymeleaf.cache=false
```

### 2. WordleController.java
```java
// Исправлен конфликт endpoints
@GetMapping("/status")  // Вместо "/"
@ResponseBody
public ResponseEntity<Map<String, String>> status() {
    // Status endpoint
}
```

### 3. Dockerfile
```dockerfile
# Упрощенный Dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test
CMD ["java", "-jar", "build/libs/wordle-0.0.1-SNAPSHOT.jar"]
```

### 4. railway.json
```json
{
  "startCommand": "java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar",
  "healthcheckPath": "/status"
}
```

## 🎯 Ожидаемый результат

После применения исправления:
- ✅ Приложение собирается без ошибок
- ✅ Spring Boot запускается успешно
- ✅ Все endpoints работают
- ✅ Healthcheck проходит на Railway

## 🔍 Endpoints для проверки

- **`/`** - главная страница игры
- **`/status`** - простой статус приложения
- **`/health`** - healthcheck endpoint
- **`/actuator/health`** - Spring Boot Actuator

## 🆘 Если проблема остается

### 1. Проверьте логи Railway
- Сборка: должна завершиться успешно
- Запуск: должны быть логи Spring Boot
- Healthcheck: должен возвращать 200 OK

### 2. Проверьте локально
```bash
# Запустите тест
./test-app.sh

# Или вручную
./gradlew bootRun
```

### 3. Проверьте JAR файл
```bash
# Проверьте, что JAR создан
ls -la build/libs/

# Проверьте содержимое JAR
jar -tf build/libs/wordle-0.0.1-SNAPSHOT.jar | head -10
```

## 📞 Поддержка

Если проблема не решается:
1. Проверьте логи в Railway Dashboard
2. Убедитесь, что все изменения загружены
3. Проверьте, что приложение запускается локально
4. Попробуйте создать новый проект на Railway
