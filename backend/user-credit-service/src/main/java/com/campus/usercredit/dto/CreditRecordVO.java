package com.campus.usercredit.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 信用分变动明细条目。 */
@Data
public class CreditRecordVO {
    private Integer delta;
    private String sourceType;
    private Integer scoreAfter;
    private String bizEventId;
    private LocalDateTime createdAt;
}
