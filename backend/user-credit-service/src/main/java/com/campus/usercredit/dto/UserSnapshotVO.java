package com.campus.usercredit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 用户公开快照（供 M2 订单昵称快照 / 商品页卖家信息内部查询，specs/05 U-07 同类内部口径）。 */
@Data
@AllArgsConstructor
public class UserSnapshotVO {
    private Long userId;
    private String nickname;
    private Integer creditScore;
}
