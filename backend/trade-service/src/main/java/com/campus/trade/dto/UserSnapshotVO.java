package com.campus.trade.dto;

/** 用户公开快照（内部接口返回结构，与 M1 InternalUserController 对齐；trade 侧独立定义避免跨应用依赖）。 */
public record UserSnapshotVO(Long userId, String nickname, Integer creditScore) {
}
