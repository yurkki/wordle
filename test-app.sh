#!/bin/bash

echo "🧪 Тестируем Spring Boot приложение..."

# Очищаем и собираем
echo "🔨 Сборка приложения..."
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "✅ Сборка успешна!"
    
    # Проверяем JAR файл
    if [ -f "build/libs/wordle-0.0.1-SNAPSHOT.jar" ]; then
        echo "✅ JAR файл создан!"
        
        # Запускаем приложение в фоне
        echo "🚀 Запуск приложения..."
        java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar &
        APP_PID=$!
        
        # Ждем запуска
        echo "⏳ Ожидание запуска (10 секунд)..."
        sleep 10
        
        # Проверяем, что приложение запустилось
        if ps -p $APP_PID > /dev/null; then
            echo "✅ Приложение запущено (PID: $APP_PID)"
            
            # Тестируем endpoints
            echo "🔍 Тестирование endpoints..."
            
            # Проверяем главную страницу
            if curl -s http://localhost:8080/ | grep -q "WORDLE"; then
                echo "✅ Главная страница работает!"
            else
                echo "❌ Главная страница не работает"
            fi
            
            # Проверяем status endpoint
            if curl -s http://localhost:8080/status | grep -q "UP"; then
                echo "✅ Status endpoint работает!"
            else
                echo "❌ Status endpoint не работает"
            fi
            
            # Проверяем health endpoint
            if curl -s http://localhost:8080/health | grep -q "UP"; then
                echo "✅ Health endpoint работает!"
            else
                echo "❌ Health endpoint не работает"
            fi
            
            # Останавливаем приложение
            echo "🛑 Остановка приложения..."
            kill $APP_PID
            wait $APP_PID 2>/dev/null
            echo "✅ Приложение остановлено"
            
        else
            echo "❌ Приложение не запустилось"
            echo "Проверьте логи выше"
        fi
        
    else
        echo "❌ JAR файл не найден!"
        exit 1
    fi
else
    echo "❌ Ошибка сборки!"
    exit 1
fi

echo "🎉 Тест завершен!"
