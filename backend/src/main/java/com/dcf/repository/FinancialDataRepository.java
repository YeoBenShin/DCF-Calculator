package com.dcf.repository;

import com.dcf.entity.FinancialData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialDataRepository extends JpaRepository<FinancialData, String> {
    
    /**
     * Find financial data by ticker symbol
     * @param ticker the ticker symbol
     * @return Optional containing the financial data if found
     */
    Optional<FinancialData> findByTicker(String ticker);
    
    /**
     * Check if financial data exists for a ticker
     * @param ticker the ticker symbol
     * @return true if data exists, false otherwise
     */
    boolean existsByTicker(String ticker);
    
    /**
     * Find financial data that is older than specified date
     * @param date the cutoff date
     * @return list of stale financial data
     */
    @Query("SELECT f FROM FinancialData f WHERE f.dateFetched < :date")
    List<FinancialData> findStaleData(@Param("date") LocalDate date);
    
    /**
     * Find financial data updated within the last N days
     * @param daysAgo the number of days ago
     * @return list of recently updated financial data
     */
    @Query("SELECT f FROM FinancialData f WHERE f.dateFetched >= :daysAgo")
    List<FinancialData> findRecentlyUpdated(@Param("daysAgo") LocalDate daysAgo);
    
    /**
     * Find all tickers that have financial data
     * @return list of ticker symbols
     */
    @Query("SELECT f.ticker FROM FinancialData f ORDER BY f.ticker")
    List<String> findAllTickers();
    
    /**
     * Count total number of financial data records
     * @return count of records
     */
    @Query("SELECT COUNT(f) FROM FinancialData f")
    long countAllRecords();
    
    /**
     * Find financial data with minimum required fields populated
     * @return list of financial data with complete data
     */
    @Query("SELECT f FROM FinancialData f WHERE SIZE(f.revenue) > 0 AND SIZE(f.freeCashFlow) > 0 AND SIZE(f.eps) > 0")
    List<FinancialData> findCompleteData();
    
    /**
     * Delete financial data older than specified date
     * @param date the cutoff date
     * @return number of deleted records
     */
    @Query("DELETE FROM FinancialData f WHERE f.dateFetched < :date")
    int deleteStaleData(@Param("date") LocalDate date);
}