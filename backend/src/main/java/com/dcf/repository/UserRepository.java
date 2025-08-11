package com.dcf.repository;

import com.dcf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Find user by email
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if user exists by email
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users who have a specific ticker in their watchlist
     * @param ticker the ticker symbol
     * @return list of users with the ticker in their watchlist
     */
    @Query("SELECT u FROM User u JOIN u.watchlist w WHERE w = :ticker")
    java.util.List<User> findUsersWithTickerInWatchlist(@Param("ticker") String ticker);
    
    /**
     * Count users who have a specific ticker in their watchlist
     * @param ticker the ticker symbol
     * @return count of users with the ticker in their watchlist
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.watchlist w WHERE w = :ticker")
    long countUsersWithTickerInWatchlist(@Param("ticker") String ticker);
}