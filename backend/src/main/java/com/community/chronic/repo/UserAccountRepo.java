package com.community.chronic.repo;

import com.community.chronic.model.Enums;
import com.community.chronic.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepo extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    List<UserAccount> findByRole(Enums.Role role);
}
