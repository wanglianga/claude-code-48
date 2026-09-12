package com.community.chronic.repo;

import com.community.chronic.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepo extends JpaRepository<AuthToken, String> {
    void deleteByUserId(Long userId);
}
