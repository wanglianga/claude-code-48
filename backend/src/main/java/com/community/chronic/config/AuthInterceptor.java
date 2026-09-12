package com.community.chronic.config;

import com.community.chronic.model.AuthToken;
import com.community.chronic.repo.AuthTokenRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/** Bearer Token 认证拦截器，把登录用户放到 request attribute "authUser"。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthTokenRepo tokenRepo;

    public AuthInterceptor(AuthTokenRepo tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            AuthToken at = tokenRepo.findById(token).orElse(null);
            if (at != null && at.expiresAt.isAfter(LocalDateTime.now())) {
                req.setAttribute("authUser", at.user);
            }
        }
        return true; // 是否要求登录由各控制器自行判断
    }
}
