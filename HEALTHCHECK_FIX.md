# 🏥 Исправление Healthcheck на Railway

## Проблема
```
Starting Healthcheck
====================
Path: /
Retry window: 1m40s
Attempt #1 failed with service unavailable
...
1/1 replicas never became healthy!
Healthcheck failed!
```

## Причина
Railway не может подключиться к приложению для проверки здоровья.

## ✅ Решение

### 1. Добавлен Healthcheck Endpoint
- **`/health`** - простой endpoint для проверки
- **`/actuator/health`** - Spring Boot Actuator endpoint

### 2. Добавлен Spring Boot Actuator
- Автоматический healthcheck
- Более надежная проверка состояния
- Детальная информация о здоровье приложения

### 3. Обновлена конфигурация Railway
- `healthcheckPath: "/actuator/health"`
- `healthcheckTimeout: 300` (5 минут)
- Увеличен timeout для медленного запуска

## 🚀 Как применить исправление

### 1. Закоммитьте изменения
```bash
git add .
git commit -m "Fix healthcheck for Railway"
git push origin main
```

### 2. Перезапустите деплой на Railway
- Зайдите в панель Railway
- Выберите ваш проект
- Нажмите "Redeploy"

### 3. Проверьте логи
- Следите за логами сборки
- Проверьте логи запуска приложения
- Убедитесь, что healthcheck проходит

## 🔍 Проверка локально

### Тест healthcheck endpoints
```bash
# Запустите приложение локально
./gradlew bootRun

# Проверьте endpoints
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

### Ожидаемый ответ
```json
{
  "status": "UP",
  "service": "wordle"
}
```

## 📋 Что было добавлено

### 1. Healthcheck Controller
```java
@GetMapping("/health")
@ResponseBody
public ResponseEntity<Map<String, String>> health() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("service", "wordle");
    return ResponseEntity.ok(response);
}
```

### 2. Spring Boot Actuator
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

### 3. Actuator Configuration
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
management.health.defaults.enabled=true
```

### 4. Railway Configuration
```json
{
  "healthcheckPath": "/actuator/health",
  "healthcheckTimeout": 300
}
```

## 🎯 Результат

После применения исправления:
- ✅ Railway может проверить здоровье приложения
- ✅ Healthcheck проходит успешно
- ✅ Приложение запускается и работает
- ✅ Доступно по адресу Railway

## 🆘 Если проблема остается

### 1. Проверьте логи Railway
- Сборка: должна завершиться успешно
- Запуск: приложение должно стартовать
- Healthcheck: должен возвращать 200 OK

### 2. Проверьте порт
- Убедитесь, что приложение слушает правильный порт
- Проверьте переменную PORT

### 3. Проверьте endpoints
```bash
# После деплоя проверьте
curl https://your-app.railway.app/health
curl https://your-app.railway.app/actuator/health
```

### 4. Альтернативные healthcheck paths
Если `/actuator/health` не работает, попробуйте:
- `/health`
- `/`
- `/actuator`

## 📞 Поддержка

Если проблема не решается:
1. Проверьте логи в Railway Dashboard
2. Убедитесь, что все изменения загружены
3. Попробуйте создать новый проект на Railway
4. Проверьте, что приложение запускается локально
