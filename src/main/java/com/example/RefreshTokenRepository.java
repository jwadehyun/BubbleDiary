package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :hash and t.expiresAt > :now")
    Optional<RefreshToken> findValid(String hash, Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.tokenHash = :hash")
    int deleteByHash(String hash);

    @Modifying
    @Query("delete from RefreshToken t where t.user = :user")
    void deleteAllOwned(User user);
}
