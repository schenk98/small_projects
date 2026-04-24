package com.thearena.persistence.mysql;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
