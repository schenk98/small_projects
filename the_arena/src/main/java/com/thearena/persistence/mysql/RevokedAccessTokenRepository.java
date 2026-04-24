package com.thearena.persistence.mysql;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessTokenEntity, Long> {
    boolean existsByJti(String jti);
}
