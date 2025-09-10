# 🚀 Деплой Wordle приложения

## Быстрый старт

### 1. Heroku (Рекомендуется для начинающих)

1. **Установите Heroku CLI**
   ```bash
   # Windows (через Chocolatey)
   choco install heroku-cli
   
   # Или скачайте с https://devcenter.heroku.com/articles/heroku-cli
   ```

2. **Войдите в аккаунт**
   ```bash
   heroku login
   ```

3. **Создайте приложение**
   ```bash
   heroku create your-wordle-app-name
   ```

4. **Деплой**
   ```bash
   git add .
   git commit -m "Deploy to Heroku"
   git push heroku main
   ```

5. **Откройте приложение**
   ```bash
   heroku open
   ```

### 2. Railway

1. Зайдите на [railway.app](https://railway.app)
2. Войдите через GitHub
3. Нажмите "New Project" → "Deploy from GitHub repo"
4. Выберите ваш репозиторий
5. Railway автоматически задеплоит приложение

### 3. Render

1. Зайдите на [render.com](https://render.com)
2. Войдите через GitHub
3. Нажмите "New" → "Web Service"
4. Выберите ваш репозиторий
5. Настройки:
   - **Build Command**: `./gradlew build`
   - **Start Command**: `java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar`
6. Нажмите "Create Web Service"

### 4. VPS (DigitalOcean/Linode)

1. **Создайте VPS** (Ubuntu 20.04+)
2. **Подключитесь по SSH**
   ```bash
   ssh root@your-server-ip
   ```

3. **Установите Java 17**
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jdk
   ```

4. **Склонируйте репозиторий**
   ```bash
   git clone https://github.com/yourusername/wordle.git
   cd wordle
   ```

5. **Соберите и запустите**
   ```bash
   ./gradlew build
   java -jar build/libs/wordle-0.0.1-SNAPSHOT.jar
   ```

6. **Настройте Nginx** (опционально)
   ```bash
   sudo apt install nginx
   # Настройте проксирование на порт 8080
   ```

## 🐳 Docker деплой

### Локальный запуск с Docker
```bash
# Сборка образа
docker build -t wordle-app .

# Запуск контейнера
docker run -p 8080:8080 wordle-app
```

### Docker Compose
```bash
docker-compose up -d
```

## 📝 Важные замечания

1. **Порт**: Приложение использует порт 8080 по умолчанию
2. **Переменные окружения**: 
   - `PORT` - порт для Heroku/Railway
   - `SPRING_PROFILES_ACTIVE=prod` - для production режима
3. **Память**: Минимум 512MB RAM для работы
4. **Java**: Требуется Java 17+

## 🔧 Troubleshooting

### Ошибка "Port already in use"
```bash
# Найдите процесс на порту 8080
lsof -i :8080
# Убейте процесс
kill -9 PID
```

### Ошибка сборки
```bash
# Очистите кэш Gradle
./gradlew clean
# Пересоберите
./gradlew build
```

### Проблемы с Heroku
```bash
# Посмотрите логи
heroku logs --tail
# Перезапустите приложение
heroku restart
```

## 🌐 Домены и SSL

- **Heroku**: Автоматический SSL, домен `your-app.herokuapp.com`
- **Railway**: Автоматический SSL, домен `your-app.railway.app`
- **Render**: Автоматический SSL, домен `your-app.onrender.com`
- **VPS**: Настройте домен и SSL сертификат (Let's Encrypt)

## 💰 Стоимость

| Провайдер | Бесплатный тариф | Платный тариф |
|-----------|------------------|---------------|
| Heroku    | 550 часов/месяц | $7/месяц |
| Railway   | $5 кредитов/месяц | $5/месяц |
| Render    | 750 часов/месяц | $7/месяц |
| VPS       | - | $3-5/месяц |

## 🎯 Рекомендации

- **Для тестирования**: Heroku (бесплатно)
- **Для продакшена**: Railway или VPS
- **Для масштабирования**: AWS/GCP/Azure
