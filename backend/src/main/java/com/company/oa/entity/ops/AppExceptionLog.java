package com.company.oa.entity.ops;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_exception_log")
public class AppExceptionLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String requestId;

    private String requestUri;

    private String requestMethod;

    private Long userId;

    private String exceptionClass;

    private String exceptionMessage;

    private String stackTrace;

    private String severity;

    private LocalDateTime occurredAt;
}