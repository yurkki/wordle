package org.example.wordle.repository;

import org.example.wordle.model.GameStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository для работы со статистикой игр в базе данных
 */
@Repository
public interface GameStatsRepository extends JpaRepository<GameStatsEntity, Long> {
    
    /**
     * Найти все записи за определенную дату, отсортированные по попыткам и времени
     */
    List<GameStatsEntity> findByGameDateOrderByAttemptsAscGameTimeSecondsAsc(LocalDate gameDate);
    
    /**
     * Найти только успешные игры за определенную дату, отсортированные по попыткам и времени
     */
    List<GameStatsEntity> findByGameDateAndSuccessTrueOrderByAttemptsAscGameTimeSecondsAsc(LocalDate gameDate);
    
    /**
     * Подсчитать общее количество игроков за дату
     */
    long countByGameDate(LocalDate gameDate);
    
    /**
     * Подсчитать количество успешных игр за дату
     */
    long countByGameDateAndSuccessTrue(LocalDate gameDate);
    
    /**
     * Найти результат конкретного игрока за определенную дату
     */
    @Query("SELECT g FROM GameStatsEntity g WHERE g.gameDate = :date AND g.playerId = :playerId")
    GameStatsEntity findByGameDateAndPlayerId(@Param("date") LocalDate date, @Param("playerId") String playerId);
    
    /**
     * Найти статистику за период между датами
     */
    @Query("SELECT g FROM GameStatsEntity g WHERE g.gameDate >= :startDate AND g.gameDate <= :endDate ORDER BY g.gameDate DESC")
    List<GameStatsEntity> findByGameDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Удалить старые записи (для очистки)
     */
    void deleteByGameDateBefore(LocalDate cutoffDate);
    
    /**
     * Удалить записи за определенную дату
     */
    void deleteByGameDate(LocalDate gameDate);
    
    /**
     * Найти все игры конкретного игрока, отсортированные по дате
     */
    List<GameStatsEntity> findByPlayerIdOrderByCompletedAtAsc(String playerId);
}
