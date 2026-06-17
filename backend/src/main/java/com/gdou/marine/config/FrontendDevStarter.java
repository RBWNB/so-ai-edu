package com.gdou.marine.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 开发环境下自动启动前端 Vite dev server。
 * 当 IntelliJ 运行 BackendApplication 时，自动在 frontend 目录执行 npm run dev。
 * 点击停止按钮时，自动关闭前端进程（包括 npm 和 node 子进程树）。
 * 仅在 dev profile 下生效，生产环境不会触发。
 */
@Configuration
@Profile("dev")
public class FrontendDevStarter {

    private static final Logger log = LoggerFactory.getLogger(FrontendDevStarter.class);
    private Process frontendProcess;
    private volatile boolean shuttingDown = false;

    public FrontendDevStarter() {
        startFrontend();
        // 注册 JVM 关闭钩子，作为 @PreDestroy 的额外保障
        registerShutdownHook();
    }

    private void startFrontend() {
        String userDir = System.getProperty("user.dir");
        File frontendDir = locateFrontendDir(userDir);

        if (frontendDir == null) {
            log.warn("未找到 frontend 目录，跳过自动启动前端。user.dir={}", userDir);
            return;
        }

        File packageJson = new File(frontendDir, "package.json");
        if (!packageJson.exists()) {
            log.warn("frontend 目录下没有 package.json: {}", packageJson.getAbsolutePath());
            return;
        }

        log.info("正在启动前端 dev server，目录: {}", frontendDir.getAbsolutePath());
        log.info("执行命令: cmd.exe /c npm run dev");

        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "npm", "run", "dev");
            pb.directory(frontendDir);
            pb.redirectErrorStream(true);
            pb.inheritIO();

            frontendProcess = pb.start();
            log.info("✅ 前端 dev server 已启动 (PID: {})", frontendProcess.pid());
        } catch (IOException e) {
            log.error("❌ 启动前端 dev server 失败", e);
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // shuttingDown 标记防止 @PreDestroy 和 shutdownHook 重复执行
            if (!shuttingDown) {
                shuttingDown = true;
                log.info("[ShutdownHook] 正在关闭前端 dev server...");
                killProcessTree();
            }
        }));
    }

    @PreDestroy
    public void cleanup() {
        if (shuttingDown) return;
        shuttingDown = true;

        log.info("正在关闭前端 dev server...");
        killProcessTree();
    }

    /**
     * 彻底杀掉整个进程树（cmd -> npm -> node）
     * 使用 Windows taskkill /T 递归终止所有子进程
     */
    private void killProcessTree() {
        if (frontendProcess == null) {
            return;
        }

        long pid = frontendProcess.pid();

        if (!frontendProcess.isAlive()) {
            log.info("前端进程已自行退出 (PID: {})", pid);
            return;
        }

        // 第一步：用 taskkill /T 杀死整个进程树（cmd、npm、node 全杀掉）
        try {
            log.info("正在终止进程树 (PID: {})...", pid);
            Process taskKill = new ProcessBuilder(
                    "taskkill.exe", "/F", "/T", "/PID", String.valueOf(pid)
            ).inheritIO().start();

            boolean killed = taskKill.waitFor(5, TimeUnit.SECONDS);
            if (killed && taskKill.exitValue() == 0) {
                log.info("✅ 前端进程树已完全终止");
                return;
            }
        } catch (Exception e) {
            log.warn("taskkill 执行异常，尝试 fallback 方式: {}", e.getMessage());
        }

        // fallback: 标准 Java 进程销毁
        try {
            frontendProcess.destroy();
            if (frontendProcess.waitFor(3, TimeUnit.SECONDS)) {
                log.info("前端进程已正常关闭");
                return;
            }
            log.warn("前端进程未响应，强制终止...");
            frontendProcess.destroyForcibly();
            frontendProcess.waitFor(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("等待前端进程结束时被中断", e);
            frontendProcess.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private File locateFrontendDir(String userDir) {
        if (userDir != null) {
            File sibling = new File(new File(userDir).getParentFile(), "frontend");
            if (sibling.exists()) {
                return sibling;
            }
        }

        if (userDir != null) {
            File child = new File(userDir, "frontend");
            if (child.exists()) {
                return child;
            }
        }

        return null;
    }
}
