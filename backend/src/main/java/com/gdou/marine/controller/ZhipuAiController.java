package com.gdou.marine.controller;

import com.gdou.marine.annotation.Log;
import com.gdou.marine.service.OperationLogAsyncService;
import com.gdou.marine.service.impl.ZhipuAiServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class ZhipuAiController {

    private static final Logger log = LoggerFactory.getLogger(ZhipuAiController.class);

    private final ZhipuAiServiceImpl zhipuAiService;
    private final OperationLogAsyncService operationLogAsyncService;
    private final HttpServletRequest request;

    public ZhipuAiController(ZhipuAiServiceImpl zhipuAiService,
                             OperationLogAsyncService operationLogAsyncService,
                             HttpServletRequest request) {
        this.zhipuAiService = zhipuAiService;
        this.operationLogAsyncService = operationLogAsyncService;
        this.request = request;
    }

    // ==================== 图像识别 ====================

    @Log(module = "智能服务", description = "AI图像识别")
    @PostMapping("/identify")
    public Map<String, Object> identifySpecies(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            ZhipuAiServiceImpl.ImageIdentifyResult identification = zhipuAiService.identifyImage(file);
            result.put("success", true);
            result.put("data", identification);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            log.error("图像识别失败", e);
            result.put("success", false);
            result.put("message", "识别失败，请稍后重试");
        }
        return result;
    }

    // ==================== 物种智能推荐 ====================

    @Log(module = "智能服务", description = "物种信息推荐")
    @PostMapping("/species/suggest")
    public Map<String, Object> suggestSpecies(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String chineseName = (String) body.get("chineseName");
            if (!StringUtils.hasText(chineseName)) {
                result.put("success", false);
                result.put("message", "中文名不能为空");
                return result;
            }
            String provider = (String) body.getOrDefault("provider", "zhipu");
            String rawJson = zhipuAiService.suggestSpeciesInfo(chineseName, provider);
            result.put("success", true);
            result.put("data", rawJson);
        } catch (Exception e) {
            log.error("AI推荐物种失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 生态系统智能推荐 ====================

    @Log(module = "智能服务", description = "AI生态系统推荐")
    @PostMapping("/ecosystem/suggest")
    public Map<String, Object> suggestEcosystem(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String name = (String) body.get("name");
            if (!StringUtils.hasText(name)) {
                result.put("success", false);
                result.put("message", "名称不能为空");
                return result;
            }
            String provider = (String) body.getOrDefault("provider", "zhipu");
            String rawJson = zhipuAiService.suggestEcosystemInfo(name, provider);
            result.put("success", true);
            result.put("data", rawJson);
        } catch (Exception e) {
            log.error("AI推荐生态系统失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 物种信息补全 ====================

    @Log(module = "智能服务", description = "AI物种信息补全")
    @PostMapping("/species/complete")
    public Map<String, Object> completeSpeciesInfo(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            String speciesName = (String) body.get("speciesName");
            if (!StringUtils.hasText(speciesName)) {
                result.put("success", false);
                result.put("message", "物种名称不能为空");
                return result;
            }
            String provider = (String) body.getOrDefault("provider", "zhipu");
            ZhipuAiServiceImpl.SpeciesCompletionResult completion =
                    zhipuAiService.completeSpeciesInfo(speciesName, provider);
            result.put("success", true);
            result.put("data", completion);
        } catch (Exception e) {
            log.error("AI补全物种信息失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 私有工具方法 ====================

    private Long getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) return id;
        if (principal instanceof Integer id) return id.longValue();
        if (principal instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private String getClientIp() {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }
}
