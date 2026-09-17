package com.campus.usercredit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import com.campus.common.jwt.JwtSupport;
import com.campus.usercredit.common.CacheKeys;
import com.campus.usercredit.config.CampusProperties;
import com.campus.usercredit.dto.LoginRequest;
import com.campus.usercredit.dto.LoginVO;
import com.campus.usercredit.dto.RegisterRequest;
import com.campus.usercredit.dto.UserVO;
import com.campus.usercredit.entity.LoginLog;
import com.campus.usercredit.entity.UserAccount;
import com.campus.usercredit.mapper.LoginLogMapper;
import com.campus.usercredit.mapper.UserAccountMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 认证服务（specs/05 U-01~U-04）。
 *
 * <p>关键机制：
 * <ul>
 *   <li>BCrypt cost 10 慢哈希，明文不落库不落日志（《安全设计》§2.1.2）</li>
 *   <li>登录失败 5 次锁 15 分钟（Redis 计数），文案防枚举（C000005）</li>
 *   <li>JWT 2h + jti 互踢：新登录将旧 jti 写入 Redis 黑名单（TTL=旧 Token 剩余有效期）</li>
 *   <li>登出将当前 jti 写黑名单；网关命中黑名单即 401</li>
 *   <li>成功/失败/锁定均写 t_login_log（§7.2.4 审计）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 防时序枚举：账号不存在时也执行一次同成本 BCrypt 比对 */
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5B0C0V0KJrRnPzWfFpCxE9Tm0S1Wy";

    private final UserAccountMapper userAccountMapper;
    private final LoginLogMapper loginLogMapper;
    private final StringRedisTemplate redis;
    private final JwtSupport jwtSupport;
    private final CampusProperties properties;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    /**
     * 注册（AC-1~4、AC-8）：格式校验 → 唯一性兜底 → BCrypt 入库 → 自动登录。
     */
    public LoginVO register(RegisterRequest req, String ip, String userAgent) {
        // 密码策略：>=8 位且非纯数字（安全设计冻结口径）
        String password = req.getPassword();
        if (password.length() < 8 || password.matches("^[0-9]+$")) {
            throw new BizException(ErrorCode.PARAM_INVALID, "密码须至少 8 位且不能为纯数字");
        }
        // 校园邮箱域名白名单（AC-2）
        String email = req.getEmail().toLowerCase();
        if (!isCampusEmail(email)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请使用校园邮箱");
        }

        UserAccount user = new UserAccount();
        user.setSchoolCode("CAMPUS-MAIN");
        user.setStudentNo(req.getStudentNo());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setNickname(StringUtils.hasText(req.getNickname())
                ? req.getNickname() : "同学" + req.getStudentNo().substring(req.getStudentNo().length() - 4));
        user.setRole("STUDENT");
        user.setStatus("ACTIVE");
        user.setCreditScore(100);
        // F18 留痕：勾选时间 + 政策版本
        user.setPrivacyConsentVersion(StringUtils.hasText(req.getPrivacyVersion())
                ? req.getPrivacyVersion() : properties.getPrivacy().getCurrentVersion());
        user.setPrivacyConsentTime(LocalDateTime.now());
        user.setCreatedBy("register");

        try {
            userAccountMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 学号唯一索引兜底（AC-4）；Redis 幂等占位缺失时的最终防线
            throw new BizException(ErrorCode.STUDENT_NO_REGISTERED);
        }
        log.info("REGISTER_SUCCESS userId={} emailDomain={}", user.getId(),
                email.substring(email.indexOf('@') + 1));
        return new LoginVO(issueToken(user), toUserVO(user));
    }

    /**
     * 登录（AC-5、AC-7）：锁定检查 → BCrypt 比对 → 互踢 → 签发。
     */
    public LoginVO login(LoginRequest req, String ip, String userAgent) {
        String account = req.getAccount().trim();
        String failKey = CacheKeys.LOGIN_FAIL + account;

        // 锁定期直接拒绝（AC-5 后半：锁定期内正确密码也被拒）
        String failCount = redis.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= properties.getLogin().getMaxFail()) {
            saveLoginLog(null, account, ip, userAgent, "LOCKED");
            throw new BizException(ErrorCode.RATE_LIMITED, "登录失败次数过多，请 15 分钟后重试");
        }

        UserAccount user = findByAccount(account);

        // 防枚举：不存在也执行同成本比对，统一返回 C000005
        boolean passwordOk;
        if (user == null) {
            encoder.matches(req.getPassword(), DUMMY_HASH);
            passwordOk = false;
        } else {
            passwordOk = encoder.matches(req.getPassword(), user.getPasswordHash());
        }

        if (!passwordOk) {
            Long count = redis.opsForValue().increment(failKey);
            redis.expire(failKey, Duration.ofMinutes(properties.getLogin().getLockMinutes()));
            saveLoginLog(user == null ? null : user.getId(), account, ip, userAgent, "FAIL");
            log.info("LOGIN_FAIL account={} failCount={}", mask(account), count);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if ("BANNED".equals(user.getStatus())) {
            saveLoginLog(user.getId(), account, ip, userAgent, "FAIL");
            throw new BizException(ErrorCode.ACCOUNT_BANNED);
        }

        // 互踢：旧 jti 进黑名单（AC-7），TTL = 旧 Token 剩余有效期
        String activeKey = CacheKeys.JWT_ACTIVE + user.getId();
        String oldActive = redis.opsForValue().get(activeKey);
        if (oldActive != null && oldActive.contains("|")) {
            String[] parts = oldActive.split("\\|");
            long expireEpoch = Long.parseLong(parts[1]);
            Duration remaining = Duration.between(Instant.now(), Instant.ofEpochSecond(expireEpoch));
            if (!remaining.isNegative() && !remaining.isZero()) {
                redis.opsForValue().set(CacheKeys.JWT_BLACKLIST + parts[0], "1", remaining);
            }
        }

        // 签发新 Token 并记录当前会话
        JwtSupport.SignedToken signed = jwtSupport.sign(user.getId(), user.getRole(), user.getSchoolCode());
        redis.opsForValue().set(activeKey,
                signed.jti() + "|" + signed.expiresAt().getEpochSecond(),
                Duration.ofHours(properties.getJwt().getExpireHours()));

        redis.delete(failKey);
        saveLoginLog(user.getId(), account, ip, userAgent, "SUCCESS");
        log.info("LOGIN_SUCCESS userId={}", user.getId());
        return new LoginVO(signed.token(), toUserVO(user));
    }

    /**
     * 登出（AC-6）：当前 jti 写黑名单 + 清会话映射。
     */
    public void logout(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return;
        }
        try {
            Claims claims = jwtSupport.parse(authorization.substring(7));
            String jti = claims.get(JwtSupport.CLAIM_JTI, String.class);
            Object userId = claims.get(JwtSupport.CLAIM_USER_ID);
            Duration remaining = jwtSupport.remaining(claims);
            if (jti != null && !remaining.isZero()) {
                redis.opsForValue().set(CacheKeys.JWT_BLACKLIST + jti, "1", remaining);
            }
            if (userId != null) {
                redis.delete(CacheKeys.JWT_ACTIVE + userId);
            }
            log.info("LOGOUT userId={}", userId);
        } catch (Exception e) {
            // Token 已无效时登出视为完成，不抛错
            log.info("LOGOUT skipped: token already invalid");
        }
    }

    private UserAccount findByAccount(String account) {
        LambdaQueryWrapper<UserAccount> qw = new LambdaQueryWrapper<>();
        qw.eq(UserAccount::getStudentNo, account)
                .or()
                .eq(UserAccount::getEmail, account.toLowerCase());
        return userAccountMapper.selectOne(qw);
    }

    private boolean isCampusEmail(String email) {
        int at = email.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        return properties.getEmail().getAllowedDomains().contains(email.substring(at + 1));
    }

    private String issueToken(UserAccount user) {
        return jwtSupport.sign(user.getId(), user.getRole(), user.getSchoolCode()).token();
    }

    private void saveLoginLog(Long userId, String account, String ip, String userAgent, String result) {
        LoginLog entry = new LoginLog();
        entry.setSchoolCode("CAMPUS-MAIN");
        entry.setUserId(userId);
        entry.setStudentNo(mask(account));
        entry.setIp(ip);
        entry.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(200, userAgent.length())));
        entry.setResult(result);
        entry.setCreatedTime(LocalDateTime.now());
        loginLogMapper.insert(entry);
    }

    /** 日志脱敏：保留前 2 位 + 后 2 位 */
    private String mask(String account) {
        if (account == null || account.length() <= 4) {
            return "****";
        }
        return account.substring(0, 2) + "****" + account.substring(account.length() - 2);
    }

    private UserVO toUserVO(UserAccount user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setSchoolCode(user.getSchoolCode());
        vo.setStudentNoMasked(maskStudentNo(user.getStudentNo()));
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreatedAt(user.getCreatedTime());
        return vo;
    }

    /** 学号脱敏（AC-12）：中间 4 位打星，如 2024****56 */
    static String maskStudentNo(String studentNo) {
        if (studentNo == null || studentNo.length() <= 6) {
            return studentNo;
        }
        return studentNo.substring(0, 4) + "****" + studentNo.substring(studentNo.length() - 2);
    }
}
