package com.gdou.marine.service;

import com.gdou.marine.entity.SysOperationLog;
import com.gdou.marine.entity.SysUser;
import com.gdou.marine.service.impl.SysOperationLogServiceImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogAsyncService {

    private final SysOperationLogServiceImpl sysOperationLogService;
    private final SysUserService sysUserService;

    public OperationLogAsyncService(SysOperationLogServiceImpl sysOperationLogService,
                                    SysUserService sysUserService) {
        this.sysOperationLogService = sysOperationLogService;
        this.sysUserService = sysUserService;
    }

    @Async("operationLogExecutor")
    public void recordOperationLog(Long userId,
                                   String module,
                                   String description,
                                   String requestMethod,
                                   String requestUrl,
                                   String ipAddress,
                                   Long executionTime,
                                   String status,
                                   Integer resultCode,
                                   String errorMessage) {
        recordOperationLog(userId, module, description, requestMethod, requestUrl, ipAddress,
                executionTime, status, resultCode, errorMessage, null);
    }

    @Async("operationLogExecutor")
    public void recordOperationLog(Long userId,
                                   String module,
                                   String description,
                                   String requestMethod,
                                   String requestUrl,
                                   String ipAddress,
                                   Long executionTime,
                                   String status,
                                   Integer resultCode,
                                   String errorMessage,
                                   String resolvedUsername) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(
                org.springframework.util.StringUtils.hasText(resolvedUsername)
                        ? resolvedUsername
                        : resolveUsername(userId));
        operationLog.setModule(module);
        operationLog.setDescription(description);
        operationLog.setRequestMethod(requestMethod);
        operationLog.setRequestUrl(requestUrl);
        operationLog.setIpAddress(ipAddress);
        operationLog.setExecutionTime(executionTime);
        operationLog.setStatus(status);
        operationLog.setErrorMessage(errorMessage);
        operationLog.setCreatedAt(LocalDateTime.now());
        sysOperationLogService.save(operationLog);
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            return "anonymous";
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return "unknown";
        }
        return user.getUsername();
    }
}
