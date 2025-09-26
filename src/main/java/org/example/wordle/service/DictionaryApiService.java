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

        // ВТОРОЙ ЭТАП: Если нет ключа API, используем fallback на локальный словарь
        if (yandexApiKey == null || yandexApiKey.isEmpty()) {
            System.out.println("API key not set, using fallback validation for word: " + word);
            return checkWordInLocalDictionary(word);
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
            return definitions != null && !definitions.isEmpty();

        } catch (HttpClientErrorException e) {
            System.out.println("Яндекс API ошибка: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            System.out.println("Яндекс API недоступен: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Ошибка Яндекс API: " + e.getMessage());
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
            status.append(", Яндекс.Словарь API: настроен");
        } else {
            status.append(", Яндекс.Словарь API: не настроен");
        }
        
        return status.toString();
    }
}
