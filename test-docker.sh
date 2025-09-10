#!/bin/bash

echo "🐳 Тестируем Dockerfile для Railway..."

# Очищаем предыдущие образы
echo "🧹 Очищаем предыдущие образы..."
docker rmi wordle-railway 2>/dev/null || true

# Собираем образ
echo "🔨 Собираем Docker образ..."
docker build -t wordle-railway .

if [ $? -eq 0 ]; then
    echo "✅ Образ собран успешно!"
    
    # Тестируем запуск
    echo "🚀 Тестируем запуск контейнера..."
    docker run -d -p 8080:8080 -e PORT=8080 --name wordle-test wordle-railway
    
    # Ждем запуска
    sleep 5
    
    # Проверяем статус
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
