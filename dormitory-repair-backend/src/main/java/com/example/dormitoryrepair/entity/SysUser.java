package com.example.dormitoryrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String studentNo;
    private String phone;
    private Integer gender;
    private String dormitoryBuilding;
    private String roomNo;
    private String avatar;
    /** 角色：ADMIN / STUDENT / REPAIRER */
    private String role;
    /** 状态：1=启用 0=禁用 */
    private Integer status;
    /** 技能类型（维修人员）：逗号分隔，如"水电维修,空调维修" */
    private String skillType;
    /** 负责区域（维修人员）：逗号分隔，如"3号楼,5号楼" */
    private String serviceArea;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
