package com.campus.usercredit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.dto.UserContextHolder;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.usercredit.dto.UserPublicVO;
import com.campus.usercredit.dto.UserVO;
import com.campus.usercredit.entity.UserAccount;
import com.campus.usercredit.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户主页服务（specs/05 U-06）：本人完整资料、他人公开信息，学号一律脱敏（AC-12）。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountMapper userAccountMapper;

    /** 本人主页（/users/me）：取网关透传的当前用户 */
    public UserVO me() {
        Long userId = UserContextHolder.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setSchoolCode(user.getSchoolCode());
        vo.setStudentNoMasked(AuthService.maskStudentNo(user.getStudentNo()));
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreatedAt(user.getCreatedTime());
        return vo;
    }

    /** 他人主页（/users/{id}）：仅公开信息 */
    public UserPublicVO publicProfile(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        UserPublicVO vo = new UserPublicVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setStudentNoMasked(AuthService.maskStudentNo(user.getStudentNo()));
        vo.setCreditScore(user.getCreditScore());
        return vo;
    }

    /** 内部工具：按 ID 查账号（供信用回流 / 内部用户快照等场景复用） */
    public UserAccount requireUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        return user;
    }

    /** 供 CreditService 分页查询本人明细的 wrapper 构造复用 */
    LambdaQueryWrapper<UserAccount> byId(Long userId) {
        return new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getId, userId);
    }
}
