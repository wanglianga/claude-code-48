package com.community.chronic.service;

import com.community.chronic.model.AuthToken;
import com.community.chronic.model.Enums;
import com.community.chronic.model.UserAccount;
import com.community.chronic.repo.AuthTokenRepo;
import com.community.chronic.repo.UserAccountRepo;
import com.community.chronic.web.ApiException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepo userRepo;
    private final AuthTokenRepo tokenRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepo userRepo, AuthTokenRepo tokenRepo) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
    }

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    @Transactional
    public AuthToken login(String username, String password) {
        UserAccount user = userRepo.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("用户名或密码错误"));
        if (!encoder.matches(password, user.passwordHash)) {
            throw ApiException.unauthorized("用户名或密码错误");
        }
        AuthToken token = new AuthToken(
                UUID.randomUUID().toString().replace("-", ""),
                user,
                LocalDateTime.now().plusDays(7));
        return tokenRepo.save(token);
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            tokenRepo.deleteById(token);
        }
    }

    public UserAccount require(UserAccount user, Enums.Role... roles) {
        if (user == null) {
            throw ApiException.unauthorized("请先登录");
        }
        if (roles.length == 0) {
            return user; // 仅要求登录，不限角色
        }
        for (Enums.Role r : roles) {
            if (user.role == r || user.role == Enums.Role.ADMIN) {
                return user;
            }
        }
        throw ApiException.forbidden("当前角色无权执行此操作");
    }
}
