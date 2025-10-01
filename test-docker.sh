#!/bin/bash

echo "🐳 Тестируем Dockerfile для Railway..."

# Clean previous images
echo "🧹 Очищаем предыдущие образы..."
docker rmi wordle-railway 2>/dev/null || true

# Build image
echo "🔨 Собираем Docker образ..."
docker build -t wordle-railway .

if [ $? -eq 0 ]; then
    echo "✅ Образ собран успешно!"
    
    # Test startup
    echo "🚀 Тестируем запуск контейнера..."
    docker run -d -p 8080:8080 -e PORT=8080 --name wordle-test wordle-railway
    
    # Wait for startup
    sleep 5
    
    # Check status
    if docker ps | grep -q wordle-test; then
        echo "✅ Контейнер запущен успешно!"
        echo "🌐 Приложение доступно по адресу: http://localhost:8080"
        echo ""
        echo "Для остановки контейнера выполните:"
        echo "docker stop wordle-test && docker rm wordle-test"
    else
        echo "❌ Контейнер не запустился"
        echo "Логи контейнера:"
        docker logs wordle-test
    fi
else
    echo "❌ Ошибка при сборке образа"
    exit 1
fi
