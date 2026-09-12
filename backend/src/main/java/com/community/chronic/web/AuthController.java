package com.community.chronic.web;

import com.community.chronic.model.AuthToken;
import com.community.chronic.model.UserAccount;
import com.community.chronic.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        AuthToken token = authService.login(req.username(), req.password());
        return Map.of("token", token.token, "user", token.user);
    }

    @GetMapping("/me")
    public UserAccount me(@RequestAttribute(value = "authUser", required = false) UserAccount user) {
        return authService.require(user);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return Map.of("ok", true);
    }
}
