package org.example.wordle.repository;

import org.example.wordle.model.PlayerStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStatsEntity, String> {
    
    Optional<PlayerStatsEntity> findByPlayerId(String playerId);
    
    @Query("SELECT COUNT(p) FROM PlayerStatsEntity p WHERE p.totalGames > 0")
    Long countActivePlayers();
    
    @Query("SELECT AVG(p.winRate) FROM PlayerStatsEntity p WHERE p.totalGames > 0")
    Double getAverageWinRate();
    
    @Query("SELECT MAX(p.maxStreak) FROM PlayerStatsEntity p")
    Integer getMaxStreakOverall();
    
    @Query("SELECT COUNT(p) FROM PlayerStatsEntity p WHERE p.totalGames > 0 AND p.winRate >= :minWinRate")
    Long countPlayersWithWinRateAbove(@Param("minWinRate") Double minWinRate);
}
