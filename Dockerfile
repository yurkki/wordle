# Используем официальный OpenJDK образ
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем Gradle wrapper и build файлы
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Копируем исходный код
COPY src src

# Даем права на выполнение gradlew
RUN chmod +x ./gradlew

# Собираем приложение
RUN ./gradlew build -x test

# Копируем собранный JAR файл
COPY build/libs/wordle-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт (Railway автоматически определяет порт из переменной PORT)
EXPOSE $PORT

# Запускаем приложение с переменной окружения PORT
CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]
