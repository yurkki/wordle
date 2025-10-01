#!/bin/bash

echo "🚀 Начинаем деплой Wordle приложения..."

# Build application
echo "📦 Сборка приложения..."
./gradlew clean build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Сборка завершена успешно!"
else
    echo "❌ Ошибка при сборке приложения"
    exit 1
fi

# Create JAR file
echo "📦 Создание JAR файла..."
cp build/libs/wordle-0.0.1-SNAPSHOT.jar target/

echo "🎉 Приложение готово к деплою!"
echo "📁 JAR файл: target/wordle-0.0.1-SNAPSHOT.jar"
echo ""
echo "Для деплоя на Heroku:"
echo "1. git add ."
echo "2. git commit -m 'Deploy to Heroku'"
echo "3. git push heroku main"
echo ""
echo "Для локального запуска:"
echo "java -jar target/wordle-0.0.1-SNAPSHOT.jar"
