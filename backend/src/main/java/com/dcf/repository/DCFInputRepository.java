package com.dcf.repository;

import com.dcf.entity.DCFInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DCFInputRepository extends JpaRepository<DCFInput, String> {
    
    /**
     * Find DCF inputs by user ID
     * @param userId the user ID
     * @return list of DCF inputs for the user
     */
    List<DCFInput> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find DCF inputs by ticker symbol
     * @param ticker the ticker symbol
     * @return list of DCF inputs for the ticker
     */
    List<DCFInput> findByTickerOrderByCreatedAtDesc(String ticker);
    
    /**
     * Find DCF inputs by user and ticker
     * @param userId the user ID
     * @param ticker the ticker symbol
     * @return list of DCF inputs for the user and ticker
     */
    List<DCFInput> findByUserIdAndTickerOrderByCreatedAtDesc(String userId, String ticker);
    
    /**
     * Find the most recent DCF input for a user and ticker
     * @param userId the user ID
     * @param ticker the ticker symbol
     * @return optional containing the most recent DCF input
     */
    @Query("SELECT d FROM DCFInput d WHERE d.userId = :userId AND d.ticker = :ticker ORDER BY d.createdAt DESC")
    Optional<DCFInput> findMostRecentByUserAndTicker(@Param("userId") String userId, @Param("ticker") String ticker);
    
    /**
     * Find DCF inputs created after a specific date
     * @param date the cutoff date
     * @return list of DCF inputs created after the date
     */
    List<DCFInput> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Count DCF inputs by user
     * @param userId the user ID
     * @return count of DCF inputs for the user
     */
    long countByUserId(String userId);
    
    /**
     * Find DCF inputs with growth rate above threshold
     * @param threshold the growth rate threshold
     * @return list of DCF inputs with high growth rates
     */
    @Query("SELECT d FROM DCFInput d WHERE d.growthRate > :threshold ORDER BY d.growthRate DESC")
    List<DCFInput> findByGrowthRateGreaterThan(@Param("threshold") Double threshold);
    
    /**
     * Find unique tickers that have been analyzed
     * @return list of unique ticker symbols
     */
    @Query("SELECT DISTINCT d.ticker FROM DCFInput d ORDER BY d.ticker")
    List<String> findDistinctTickers();
    
    /**
     * Delete old DCF inputs before a specific date
     * @param date the cutoff date
     * @return number of deleted records
     */
    @Query("DELETE FROM DCFInput d WHERE d.createdAt < :date")
    int deleteByCreatedAtBefore(@Param("date") LocalDateTime date);
}