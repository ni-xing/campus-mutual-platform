package com.campus.usercredit.controller;

import com.campus.common.dto.Result;
import com.campus.usercredit.dto.UserSnapshotVO;
import com.campus.usercredit.entity.UserAccount;
import com.campus.usercredit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部用户接口：供 M2/M4 服务间调用（订单昵称快照等）。
 * 网关侧 /internal/** 404，不暴露公网；仅 docker 内网可达。
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public Result<UserSnapshotVO> snapshot(@PathVariable Long userId) {
        var user = userService.requireUser(userId);
        return Result.ok(new UserSnapshotVO(user.getId(), user.getNickname(), user.getCreditScore()));
    }
}
