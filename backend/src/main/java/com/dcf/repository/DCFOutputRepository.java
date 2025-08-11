package com.dcf.repository;

import com.dcf.entity.DCFOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DCFOutputRepository extends JpaRepository<DCFOutput, String> {
    
    /**
     * Find DCF outputs by user ID
     * @param userId the user ID
     * @return list of DCF outputs for the user
     */
    List<DCFOutput> findByUserIdOrderByCalculatedAtDesc(String userId);
    
    /**
     * Find DCF outputs by ticker symbol
     * @param ticker the ticker symbol
     * @return list of DCF outputs for the ticker
     */
    List<DCFOutput> findByTickerOrderByCalculatedAtDesc(String ticker);
    
    /**
     * Find DCF outputs by user and ticker
     * @param userId the user ID
     * @param ticker the ticker symbol
     * @return list of DCF outputs for the user and ticker
     */
    List<DCFOutput> findByUserIdAndTickerOrderByCalculatedAtDesc(String userId, String ticker);
    
    /**
     * Find the most recent DCF output for a user and ticker
     * @param userId the user ID
     * @param ticker the ticker symbol
     * @return optional containing the most recent DCF output
     */
    @Query("SELECT d FROM DCFOutput d WHERE d.userId = :userId AND d.ticker = :ticker ORDER BY d.calculatedAt DESC")
    Optional<DCFOutput> findMostRecentByUserAndTicker(@Param("userId") String userId, @Param("ticker") String ticker);
    
    /**
     * Find DCF output by DCF input ID
     * @param dcfInputId the DCF input ID
     * @return optional containing the DCF output
     */
    Optional<DCFOutput> findByDcfInputId(String dcfInputId);
    
    /**
     * Find undervalued stocks (fair value > current price)
     * @return list of undervalued DCF outputs
     */
    @Query("SELECT d FROM DCFOutput d WHERE d.fairValuePerShare > d.currentPrice AND d.currentPrice IS NOT NULL ORDER BY d.upsideDownsidePercentage DESC")
    List<DCFOutput> findUndervaluedStocks();
    
    /**
     * Find overvalued stocks (fair value < current price)
     * @return list of overvalued DCF outputs
     */
    @Query("SELECT d FROM DCFOutput d WHERE d.fairValuePerShare < d.currentPrice AND d.currentPrice IS NOT NULL ORDER BY d.upsideDownsidePercentage ASC")
    List<DCFOutput> findOvervaluedStocks();
    
    /**
     * Find DCF outputs with significant upside (above threshold)
     * @param upsideThreshold the upside percentage threshold
     * @return list of DCF outputs with significant upside
     */
    @Query("SELECT d FROM DCFOutput d WHERE d.upsideDownsidePercentage > :threshold ORDER BY d.upsideDownsidePercentage DESC")
    List<DCFOutput> findWithUpsideAbove(@Param("threshold") Double upsideThreshold);
    
    /**
     * Find DCF outputs calculated after a specific date
     * @param date the cutoff date
     * @return list of DCF outputs calculated after the date
     */
    List<DCFOutput> findByCalculatedAtAfter(LocalDateTime date);
    
    /**
     * Count DCF outputs by user
     * @param userId the user ID
     * @return count of DCF outputs for the user
     */
    long countByUserId(String userId);
    
    /**
     * Find DCF outputs by valuation status
     * @param valuation the valuation status (e.g., "Undervalued", "Overvalued")
     * @return list of DCF outputs with the specified valuation
     */
    List<DCFOutput> findByValuationOrderByCalculatedAtDesc(String valuation);
    
    /**
     * Get average fair value for a ticker
     * @param ticker the ticker symbol
     * @return average fair value per share
     */
    @Query("SELECT AVG(d.fairValuePerShare) FROM DCFOutput d WHERE d.ticker = :ticker")
    Double getAverageFairValueByTicker(@Param("ticker") String ticker);
    
    /**
     * Delete old DCF outputs before a specific date
     * @param date the cutoff date
     * @return number of deleted records
     */
    @Query("DELETE FROM DCFOutput d WHERE d.calculatedAt < :date")
    int deleteByCalculatedAtBefore(@Param("date") LocalDateTime date);
}