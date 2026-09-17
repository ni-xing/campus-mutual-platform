package com.campus.usercredit.controller;

import com.campus.common.dto.Result;
import com.campus.usercredit.dto.UserPublicVO;
import com.campus.usercredit.dto.UserVO;
import com.campus.usercredit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户主页接口（specs/05 U-06）：/me 取网关透传身份，/{id} 公开信息。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.me());
    }

    @GetMapping("/{id}")
    public Result<UserPublicVO> profile(@PathVariable Long id) {
        return Result.ok(userService.publicProfile(id));
    }
}
