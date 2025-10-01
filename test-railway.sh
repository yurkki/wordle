#!/bin/bash

echo "🚂 Тестируем конфигурацию для Railway..."

# Check if application builds
echo "🔨 Сборка приложения..."
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "✅ Сборка успешна!"
    
    # Check if JAR file is created
    if [ -f "build/libs/wordle-0.0.1-SNAPSHOT.jar" ]; then
        echo "✅ JAR файл создан!"
        
        # Test startup
        echo "🚀 Тестируем запуск..."
        java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar --server.port=8080 --server.address=0.0.0.0 &
        APP_PID=$!
        
        # Wait for startup
        sleep 10
        
        # Check endpoints
        echo "🔍 Проверяем endpoints..."
        
        # Check root endpoint
        if curl -s http://localhost:8080/ | grep -q "Wordle Game is running"; then
            echo "✅ Корневой endpoint работает!"
        else
            echo "❌ Корневой endpoint не работает"
        fi
        
        # Check health endpoint
        if curl -s http://localhost:8080/health | grep -q "UP"; then
            echo "✅ Health endpoint работает!"
        else
            echo "❌ Health endpoint не работает"
        fi
        
        # Check actuator health
        if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
            echo "✅ Actuator health endpoint работает!"
        else
            echo "❌ Actuator health endpoint не работает"
        fi
        
        # Stop application
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
