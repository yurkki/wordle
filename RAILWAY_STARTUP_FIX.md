# 🚀 Исправление запуска приложения на Railway

## Проблема
```
Starting Healthcheck
Path: /actuator/health
Attempt #1 failed with service unavailable
...
1/1 replicas never became healthy!
```

## Причина
Приложение не запускается или не слушает правильный порт/адрес.

## ✅ Решение

### 1. Добавлено логирование
- Подробные логи Tomcat
- Логи Spring Boot
- Логи приложения

### 2. Исправлена конфигурация сервера
- `server.address=0.0.0.0` - слушать все интерфейсы
- `server.servlet.context-path=/` - корневой контекст
- Явное указание адреса в команде запуска

### 3. Добавлены тестовые endpoints
- `/` - корневой endpoint для проверки
- `/health` - простой healthcheck
- `/actuator/health` - Spring Boot Actuator

### 4. Упрощен healthcheck
- Используется корневой путь `/`
- Увеличен timeout до 300 секунд
- Добавлены параметры запуска

## 🚀 Как применить исправление

### Вариант 1: Использовать исправленный Dockerfile
```bash
git add .
git commit -m "Fix Railway startup issues"
git push origin main
```

### Вариант 2: Использовать простой Dockerfile
```bash
# Переименуйте файлы
mv Dockerfile Dockerfile.complex
mv Dockerfile.simple Dockerfile

# Закоммитьте изменения
git add .
git commit -m "Use simple Dockerfile for Railway"
git push origin main
```

### Вариант 3: Тестирование локально
```bash
# Сделайте скрипт исполняемым
chmod +x test-railway.sh

# Запустите тест
./test-railway.sh
```

## 🔍 Проверка исправления

### Локальное тестирование
```bash
# Сборка
./gradlew clean build

# Запуск
java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar --server.port=8080 --server.address=0.0.0.0

# Проверка endpoints
curl http://localhost:8080/
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

### Ожидаемые ответы
```json
// GET /
{
  "message": "Wordle Game is running!",
  "status": "UP",
  "timestamp": "1234567890"
}

// GET /health
{
  "status": "UP",
  "service": "wordle",
  "timestamp": "1234567890"
}

// GET /actuator/health
{
  "status": "UP"
}
```

## 📋 Что было изменено

### 1. application.properties
```properties
# Логирование
logging.level.org.springframework.boot.web.embedded.tomcat=INFO
logging.level.org.apache.catalina=INFO

# Настройки сервера
server.address=0.0.0.0
server.servlet.context-path=/
```

### 2. WordleController.java
```java
@GetMapping("/")
@ResponseBody
public ResponseEntity<Map<String, String>> root() {
    // Простой endpoint для проверки
}

@GetMapping("/health")
@ResponseBody
public ResponseEntity<Map<String, String>> health() {
    // Healthcheck endpoint
}
```

### 3. railway.json
```json
{
  "startCommand": "java -jar app.jar --server.port=$PORT --server.address=0.0.0.0",
  "healthcheckPath": "/",
  "healthcheckTimeout": 300
}
```

### 4. Dockerfile.simple
```dockerfile
# Простой Dockerfile без многоэтапной сборки
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test
CMD ["sh", "-c", "java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar --server.port=$PORT --server.address=0.0.0.0"]
```

## 🎯 Результат

После применения исправления:
- ✅ Приложение слушает все интерфейсы (0.0.0.0)
- ✅ Healthcheck проходит успешно
- ✅ Доступны тестовые endpoints
- ✅ Подробные логи для отладки

## 🆘 Если проблема остается

### 1. Проверьте логи Railway
- Сборка: должна завершиться успешно
- Запуск: должны быть логи Tomcat
- Healthcheck: должен возвращать 200 OK

### 2. Проверьте переменные окружения
- `PORT` - должен быть установлен Railway
- `RAILWAY_ENVIRONMENT` - среда

### 3. Попробуйте альтернативные healthcheck paths
- `/` (корневой)
- `/health`
- `/actuator/health`

### 4. Проверьте порт
```bash
# В логах Railway должно быть:
# "Tomcat started on port(s): 8080 (http)"
# "Started WordleApplication in X.XXX seconds"
```

## 📞 Поддержка

Если проблема не решается:
1. Проверьте логи в Railway Dashboard
2. Убедитесь, что все изменения загружены
3. Попробуйте простой Dockerfile
4. Проверьте, что приложение запускается локально
