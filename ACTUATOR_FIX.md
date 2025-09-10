# 🔧 Исправление ошибки Spring Boot Actuator в Railway

## Проблема
```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'processorMetrics' defined in class path resource [org/springframework/boot/actuate/autoconfigure/metrics/SystemMetricsAutoConfiguration.class]: Failed to instantiate [io.micrometer.core.instrument.binder.system.ProcessorMetrics]: Factory method 'processorMetrics' threw exception with message: Cannot invoke "jdk.internal.platform.CgroupInfo.getMountPoint()" because "anyController" is null
```

## ✅ Решение

### 1. Убрана проблемная зависимость
- Удален `spring-boot-starter-actuator` из `build.gradle`
- Убраны все настройки Actuator из `application.properties`
- Удален `/health` endpoint из контроллера

### 2. Упрощена конфигурация
- Оставлены только базовые зависимости Spring Boot
- Простая конфигурация без метрик
- Использован простой Dockerfile

### 3. Создан простой Dockerfile
- `Dockerfile.simple` без проблемных зависимостей
- Обновлен `railway.json` для использования простого Dockerfile

## 🚀 Как применить исправление

### 1. Закоммитьте изменения
```bash
git add .
git commit -m "Fix Spring Boot Actuator error in Railway"
git push origin main
```

### 2. Перезапустите деплой на Railway
- Зайдите в панель Railway
- Выберите ваш проект
- Нажмите "Redeploy"

## 📋 Что было изменено

### 1. build.gradle
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    // Убран spring-boot-starter-actuator
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

### 2. application.properties
```properties
spring.application.name=wordle
server.port=8080
spring.thymeleaf.cache=false
spring.thymeleaf.enabled=true
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
```

### 3. WordleController.java
```java
// Убран /health endpoint
// Оставлен только /status endpoint
@GetMapping("/status")
@ResponseBody
public ResponseEntity<Map<String, String>> status() {
    // Простой статус без метрик
}
```

### 4. railway.json
```json
{
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile.simple"  // Используем простой Dockerfile
  },
  "deploy": {
    "startCommand": "java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar",
    "healthcheckPath": "/status",
    "healthcheckTimeout": 300
  }
}
```

### 5. Dockerfile.simple
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew build -x test
CMD ["java", "-jar", "build/libs/wordle-0.0.1-SNAPSHOT.jar"]
```

## 🔍 Проверка локально

### Тестирование
```bash
# Сборка
./gradlew clean build

# Запуск
java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar

# Проверка endpoints
curl http://localhost:8080/
curl http://localhost:8080/status
```

## 🎯 Ожидаемый результат

После применения исправления:
- ✅ Приложение собирается без ошибок
- ✅ Spring Boot запускается без ошибок Actuator
- ✅ Все endpoints работают
- ✅ Healthcheck проходит на Railway
- ✅ Нет ошибок с метриками процессора

## 🔍 Endpoints для проверки

- **`/`** - главная страница игры
- **`/status`** - простой статус приложения (для healthcheck)

## 🆘 Если проблема остается

### 1. Проверьте логи Railway
- Сборка: должна завершиться успешно
- Запуск: не должно быть ошибок Actuator
- Healthcheck: должен возвращать 200 OK

### 2. Проверьте локально
```bash
# Запустите тест
./gradlew bootRun

# Проверьте endpoints
curl http://localhost:8080/status
```

### 3. Альтернативное решение
Если проблема остается, можно попробовать:
- Использовать более старую версию Spring Boot
- Добавить JVM флаги для отключения метрик
- Использовать другой PaaS (Heroku, Render)

## 📞 Поддержка

Если проблема не решается:
1. Проверьте логи в Railway Dashboard
2. Убедитесь, что все изменения загружены
3. Проверьте, что приложение запускается локально
4. Попробуйте создать новый проект на Railway
