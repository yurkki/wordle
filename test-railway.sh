#!/bin/bash

echo "🚂 Тестируем конфигурацию для Railway..."

# Проверяем, что приложение собирается
echo "🔨 Сборка приложения..."
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "✅ Сборка успешна!"
    
    # Проверяем, что JAR файл создан
    if [ -f "build/libs/wordle-0.0.1-SNAPSHOT.jar" ]; then
        echo "✅ JAR файл создан!"
        
        # Тестируем запуск
        echo "🚀 Тестируем запуск..."
        java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar --server.port=8080 --server.address=0.0.0.0 &
        APP_PID=$!
        
        # Ждем запуска
        sleep 10
        
        # Проверяем endpoints
        echo "🔍 Проверяем endpoints..."
        
        # Проверяем корневой endpoint
        if curl -s http://localhost:8080/ | grep -q "Wordle Game is running"; then
            echo "✅ Корневой endpoint работает!"
        else
            echo "❌ Корневой endpoint не работает"
        fi
        
        # Проверяем health endpoint
        if curl -s http://localhost:8080/health | grep -q "UP"; then
            echo "✅ Health endpoint работает!"
        else
            echo "❌ Health endpoint не работает"
        fi
        
        # Проверяем actuator health
        if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
            echo "✅ Actuator health endpoint работает!"
        else
            echo "❌ Actuator health endpoint не работает"
        fi
        
        # Останавливаем приложение
        kill $APP_PID
        echo "🛑 Приложение остановлено"
        
    else
        echo "❌ JAR файл не найден!"
        exit 1
    fi
else
    echo "❌ Ошибка сборки!"
    exit 1
fi

echo "🎉 Тест завершен!"
