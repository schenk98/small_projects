package com.thearena.persistence.mysql;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    Optional<PlayerEntity> findByUsername(String username);
}
