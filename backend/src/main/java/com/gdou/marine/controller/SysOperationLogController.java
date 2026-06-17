package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.entity.SysOperationLog;
import com.gdou.marine.service.impl.SysOperationLogServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys-operation-log")
public class SysOperationLogController {

    private final SysOperationLogServiceImpl operationLogService;

    public SysOperationLogController(SysOperationLogServiceImpl operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pageOperationLogs(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "module", required = false) String module) {

        Page<SysOperationLog> page = new Page<>(current, size);
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.isEmpty()) {
            wrapper.like(SysOperationLog::getUsername, username);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.like(SysOperationLog::getModule, module);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SysOperationLog::getCreatedAt);

        Page<SysOperationLog> result = operationLogService.page(page, wrapper);
        return ResponseEntity.ok(result);
    }
}

