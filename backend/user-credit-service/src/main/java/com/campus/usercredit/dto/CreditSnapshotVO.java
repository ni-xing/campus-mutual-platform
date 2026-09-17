package com.campus.usercredit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 信用快照（供 M2 检索排序 / M4 接单优先级内部查询）。 */
@Data
@AllArgsConstructor
public class CreditSnapshotVO {
    private Long userId;
    private Integer creditScore;
}
