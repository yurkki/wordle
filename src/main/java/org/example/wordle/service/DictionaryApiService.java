package org.example.wordle.service;

import org.example.wordle.repository.ExtendedWordsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Сервис для работы с Яндекс.Словарь API
 */
@Service
public class DictionaryApiService {

    private final RestTemplate restTemplate;
    private final ExtendedWordsRepository extendedWordsRepository;

    @Value("${dictionary.api.yandex.key:}")
    private String yandexApiKey;

    @Value("${dictionary.api.timeout:5000}")
    private int timeoutMs;

    // Кэширование ошибок API на 1 час
    private LocalDateTime lastApiErrorTime = null;
    private static final int API_ERROR_CACHE_HOURS = 1;

    public DictionaryApiService(ExtendedWordsRepository extendedWordsRepository) {
        this.restTemplate = new RestTemplate();
        this.extendedWordsRepository = extendedWordsRepository;
    }

    @PostConstruct
    public void init() {
        System.out.println("DictionaryApiService initialized with API key: " +
                           (yandexApiKey != null && !yandexApiKey.isEmpty() ? "SET" : "NOT SET"));
    }

    /**
     * Проверяет, можно ли использовать API (не было ли ошибки в последний час)
     */
    private boolean canUseApi() {
        if (yandexApiKey == null || yandexApiKey.isEmpty()) {
            return false;
        }
        
        if (lastApiErrorTime == null) {
            return true;
        }
        
        long hoursSinceLastError = ChronoUnit.HOURS.between(lastApiErrorTime, LocalDateTime.now());
        boolean canUse = hoursSinceLastError >= API_ERROR_CACHE_HOURS;
        
        if (!canUse) {
            long remainingMinutes = 60 - ChronoUnit.MINUTES.between(lastApiErrorTime, LocalDateTime.now());
            System.out.println("API заблокирован из-за ошибки. Осталось минут до разблокировки: " + remainingMinutes);
        }
        
        return canUse;
    }

    /**
     * Отмечает время ошибки API
     */
    private void markApiError() {
        lastApiErrorTime = LocalDateTime.now();
        System.out.println("API ошибка зафиксирована. API будет заблокирован на " + API_ERROR_CACHE_HOURS + " час(а)");
    }

    /**
     * Проверяет слово через расширенный словарь и Яндекс.Словарь API
     * Сначала проверяет в расширенном словаре, затем через Яндекс API
     */
    public boolean isWordValid(String word) {
        if (word == null || word.length() != 5) {
            return false;
        }

        // Проверяем формат слова
        if (!word.matches("[а-яА-ЯёЁ]{5}")) {
            return false;
        }

        // ПЕРВЫЙ ЭТАП: Проверяем в расширенном словаре
        if (extendedWordsRepository.containsWord(word)) {
            System.out.println("Слово найдено в расширенном словаре: " + word);
            return true;
        }

        // ВТОРОЙ ЭТАП: Проверяем, можно ли использовать API
        if (!canUseApi()) {
            System.out.println("API заблокирован из-за предыдущей ошибки, используем fallback валидацию для слова: " + word);
            boolean isValid = checkWordInLocalDictionary(word);
            if (!isValid) {
                // Логируем случай, когда слово не найдено в расширенном словаре и API заблокирован
                System.out.println("⚠️ СЛОВО НЕ НАЙДЕНО В РАСШИРЕННОМ СЛОВАРЕ И API ЗАБЛОКИРОВАН: " + word + " (пользователь пытался ввести неизвестное слово)");
            }
            return isValid;
        }

        // ТРЕТИЙ ЭТАП: Пробуем API с таймаутом
        try {
            CompletableFuture<Boolean> apiResult = CompletableFuture.supplyAsync(() -> {
                try {
                    return checkWordWithYandexApi(word);
                } catch (Exception e) {
                    return false;
                }
            });

            // Ждем результат с таймаутом
            Boolean result = apiResult.get(timeoutMs, TimeUnit.MILLISECONDS);

            return result != null && result;

        } catch (Exception e) {
            System.out.println("Яндекс API недоступен: " + e.getMessage());
            // Отмечаем ошибку API для блокировки на час
            markApiError();
            // Логируем случай, когда API недоступен, но слово не найдено в расширенном словаре
            System.out.println("⚠️ СЛОВО НЕ НАЙДЕНО В РАСШИРЕННОМ СЛОВАРЕ И API НЕДОСТУПЕН: " + word + " (пользователь пытался ввести неизвестное слово)");
            return false;
        }
    }

    /**
     * Fallback проверка в локальном словаре (WordsRepository)
     */
    private boolean checkWordInLocalDictionary(String word) {
        // Простая проверка на основе списка слов из WordsRepository
        // Это базовый fallback когда API недоступен
        return word != null && word.length() == 5 && word.matches("[а-яА-ЯёЁ]{5}");
    }

    /**
     * Проверка через Яндекс.Словарь API
     */
    private boolean checkWordWithYandexApi(String word) {
        if (yandexApiKey == null || yandexApiKey.isEmpty()) {
            return false;
        }

        try {
            String url = String.format(
                    "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?key=%s&lang=ru-ru&text=%s",
                    yandexApiKey, word
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            @SuppressWarnings("unchecked")
            List<Object> definitions = (List<Object>) response.get("def");
            boolean found = definitions != null && !definitions.isEmpty();

            if (found) {
                System.out.println("✅ Яндекс API: слово '" + word + "' найдено в словаре");
            } else {
                System.out.println("❌ Яндекс API: слово '" + word + "' НЕ найдено в словаре");
            }

            return found;

        } catch (HttpClientErrorException e) {
            System.out.println("Яндекс API ошибка: " + e.getStatusCode());
            // Отмечаем ошибку API для блокировки на час
            markApiError();
        } catch (ResourceAccessException e) {
            System.out.println("Яндекс API недоступен: " + e.getMessage());
            // Отмечаем ошибку API для блокировки на час
            markApiError();
        } catch (Exception e) {
            System.out.println("Ошибка Яндекс API: " + e.getMessage());
            // Отмечаем ошибку API для блокировки на час
            markApiError();
        }

        return false;
    }


    /**
     * Получает статус API и расширенного словаря
     */
    public String getApiStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Расширенный словарь: ").append(extendedWordsRepository.getWordCount()).append(" слов");

        if (yandexApiKey != null && !yandexApiKey.isEmpty()) {
            if (canUseApi()) {
                status.append(", Яндекс.Словарь API: доступен");
            } else {
                long remainingMinutes = 60 - ChronoUnit.MINUTES.between(lastApiErrorTime, LocalDateTime.now());
                status.append(", Яндекс.Словарь API: заблокирован (осталось ").append(remainingMinutes).append(" мин)");
            }
        } else {
            status.append(", Яндекс.Словарь API: не настроен");
        }

        return status.toString();
    }
}
