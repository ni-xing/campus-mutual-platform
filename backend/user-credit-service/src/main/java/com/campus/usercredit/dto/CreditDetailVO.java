package com.campus.usercredit.dto;

import lombok.Data;

import java.util.List;

/** 本人信用分与明细（specs/05 U-07）。 */
@Data
public class CreditDetailVO {
    private Long userId;
    private Integer creditScore;
    private List<CreditRecordVO> records;
}
