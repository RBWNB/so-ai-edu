package com.gdou.marine.aspect;

import com.gdou.marine.annotation.Log;
import com.gdou.marine.controller.AuthController;
import com.gdou.marine.dto.RegisterRequestDTO;
import com.gdou.marine.service.OperationLogAsyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;

@Aspect
@Component
public class LogAspect {

    private final OperationLogAsyncService operationLogAsyncService;

    /** 只对写操作记录日志，避免 GET 类查询请求泛滥 */
    private static final Set<String> LOGGABLE_METHODS = Set.of("POST", "PUT", "DELETE");

    public LogAspect(OperationLogAsyncService operationLogAsyncService) {
        this.operationLogAsyncService = operationLogAsyncService;
    }

    /**
     * 全局拦截 controller 包及其所有子包下的全部方法
     */
    @Pointcut("execution(* com.gdou.marine.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Long preAuthUserId = getCurrentUserId();
        String status = "SUCCESS";
        Integer resultCode = null;
        String errorMessage = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            if (result instanceof ResponseEntity<?> responseEntity) {
                if (responseEntity.getStatusCode().isError()) {
                    status = "FAILED";
                    resultCode = responseEntity.getStatusCode().value();
                    errorMessage = truncate(extractErrorMessage(responseEntity.getBody()));
                }
            }
            return result;
        } catch (Throwable e) {
            status = "FAILED";
            resultCode = 500;
            errorMessage = truncate(e.getMessage());
            throw e;
        } finally {
            try {
                handleLog(joinPoint, System.currentTimeMillis() - start, preAuthUserId,
                        status, resultCode, errorMessage);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleLog(ProceedingJoinPoint joinPoint, long executionTime, Long preAuthUserId,
                           String status, Integer resultCode, String errorMessage) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return;
        }

        String httpMethod = request.getMethod().toUpperCase();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Log logAnnotation = method.getAnnotation(Log.class);

        String module;
        String description;
        if (logAnnotation != null) {
            module = logAnnotation.module();
            description = logAnnotation.description();
        } else {
            // 无 @Log 注解时，只记录写操作，避免 GET 查询请求泛滥
            if (!LOGGABLE_METHODS.contains(httpMethod)) {
                return;
            }
            String className = joinPoint.getTarget().getClass().getSimpleName();
            module = className.replace("Controller", "");
            description = method.getName();
        }

        // 优先用方法执行前的 userId；若被清空（如 logout），用预捕获值兜底
        Long userId = getCurrentUserId();
        if (userId == null) {
            userId = preAuthUserId;
        }
        String requestUrl = request.getRequestURI();
        String ipAddress = getClientIp(request);

        // 匿名用户兜底：从方法入参 DTO 中提取 username，确保审计日志不出现 "anonymous"
        String resolvedUsername = (userId == null) ? extractUsernameFromArgs(joinPoint.getArgs()) : null;

        operationLogAsyncService.recordOperationLog(
                userId,
                module,
                description,
                httpMethod,
                requestUrl,
                ipAddress,
                executionTime,
                status,
                resultCode,
                errorMessage,
                resolvedUsername
        );
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Integer id) {
            return id.longValue();
        }
        if (principal instanceof String principalText) {
            try {
                return Long.parseLong(principalText);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 从未认证请求的 DTO 入参中提取 username。
     * 覆盖登录 (AuthController.LoginRequest) 和注册 (RegisterRequestDTO) 两个场景。
     * 若后续有其他匿名接口需要审计，在此补充 instanceof 分支即可。
     */
    private String extractUsernameFromArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof RegisterRequestDTO dto) {
                return dto.getUsername();
            }
            if (arg instanceof AuthController.LoginRequest loginReq) {
                return loginReq.getUsername();
            }
        }
        return null;
    }

    private String extractErrorMessage(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String s) {
            return s;
        }
        return body.toString();
    }

    private static String truncate(String s) {
        if (s != null && s.length() > 500) {
            return s.substring(0, 500);
        }
        return s;
    }
}
