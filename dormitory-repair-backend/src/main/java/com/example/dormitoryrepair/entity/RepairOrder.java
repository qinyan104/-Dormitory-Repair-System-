package com.example.dormitoryrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("repair_order")
public class RepairOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long categoryId;
    private Long handlerId;
    private Long workerId;
    private String title;
    private String content;
    private String imageUrl;
    private String urgency;
    private Integer repairStatus;
    private LocalDateTime submitTime;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime workerAcceptTime;
    private LocalDateTime handleTime;
    private LocalDateTime workerCompleteTime;
    private LocalDateTime finishTime;
    private LocalDateTime studentConfirmTime;
    private LocalDateTime cancelTime;
    private String remark;
    /** AI 优先级评分 (1-10) */
    private Integer priorityScore;
    /** AI 影响范围 (1-10) */
    private Integer impactScope;
    /** AI 验收风险评分 (0-100) */
    private Integer riskScore;
    /** 是否需要管理员复查 */
    private Integer needsReview;
    /** 验收方式 */
    private String reviewType;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
