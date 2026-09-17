package com.campus.usercredit.controller;

import com.campus.common.dto.Result;
import com.campus.usercredit.dto.LoginRequest;
import com.campus.usercredit.dto.LoginVO;
import com.campus.usercredit.dto.RegisterRequest;
import com.campus.usercredit.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（specs/05 U-01~U-04）。注册/登录为网关 JWT 白名单路径，公开可达。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        return Result.ok(authService.register(req, clientIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return Result.ok(authService.login(req, clientIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.ok();
    }

    private String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : http.getRemoteAddr();
    }
}
