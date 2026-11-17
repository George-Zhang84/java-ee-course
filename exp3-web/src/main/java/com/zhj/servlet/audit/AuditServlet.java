package com.zhj.servlet.audit;

import com.zhj.infrastructure.JsonUtil;
import com.zhj.model.User;
import com.zhj.servlet.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

@WebServlet("/api/audit")
public class AuditServlet extends BaseServlet {

    private static final int MAX_LINES = 100;

    public void readLog(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        // 审计日志包含全站操作记录，只允许管理员查看
        if (!"ADMIN".equals(user.getRoleCode()) && !"SUPER_ADMIN".equals(user.getRoleCode())) {
            JsonUtil.error(resp, 403, "无权限查看审计日志");
            return;
        }

        // 与 logback.xml 中的 ${catalina.base}/logs/audit.log 保持一致；
        // catalina.base 不存在时（例如单元测试）退回当前目录，避免拼出 "null/logs/..."。
        String catalinaBase = System.getProperty("catalina.base");
        File logFile = catalinaBase == null
                ? new File("logs/audit.log")
                : new File(catalinaBase, "logs" + File.separator + "audit.log");

        if (!logFile.isFile()) {
            JsonUtil.error(resp, 404, "暂无日志文件，请先进行操作");
            return;
        }

        try (Stream<String> lines = Files.lines(logFile.toPath())) {
            // 只保留最后 100 行，避免日志文件很大时一次性全部读进内存
            Deque<String> tail = new ArrayDeque<>(MAX_LINES + 1);
            lines.forEach(line -> {
                if (tail.size() == MAX_LINES) {
                    tail.removeFirst();
                }
                tail.addLast(line);
            });
            List<String> result = new ArrayList<>(tail);
            Collections.reverse(result);
            JsonUtil.success(resp, result);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.error(resp, 500, "日志读取失败");
        }
    }
}
