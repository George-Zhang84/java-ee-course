package com.zhj.servlet;

import com.zhj.infrastructure.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 反射路由基类：根据 action 参数把请求分发到子类的同名方法。
 *
 * <p>安全约束：
 * <ul>
 *   <li>action 只允许字母开头的标识符，避免把任意字符串拼进反射查找；</li>
 *   <li>只查找当前子类声明的方法，不会调用到父类或 Object 的方法；</li>
 *   <li>反射调用抛出的真实异常会被解包并记录日志，返回给前端的只有笼统提示，
 *       不泄露 SQL、堆栈等内部信息。</li>
 * </ul>
 */
public abstract class BaseServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(BaseServlet.class);
    private static final Pattern ACTION_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9]{0,40}");

    /**
     * 只读动作白名单。除这些动作外，其余动作一律要求 POST，
     * 避免通过 <img src>、页面链接等 GET 请求触发写操作（降低 CSRF 攻击面）。
     */
    private static final Set<String> READ_ONLY_ACTIONS = Set.of(
            "list", "myList", "pendingList", "getByOrder", "readLog"
    );

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String action = req.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            JsonUtil.error(resp, 400, "缺少 action 参数");
            return;
        }
        action = action.trim();
        if (!ACTION_PATTERN.matcher(action).matches()) {
            JsonUtil.error(resp, 400, "非法的 action 参数");
            return;
        }

        Method method;
        try {
            method = this.getClass().getDeclaredMethod(action, HttpServletRequest.class, HttpServletResponse.class);
        } catch (NoSuchMethodException e) {
            JsonUtil.error(resp, 404, "方法不存在: " + action);
            return;
        }

        // 除只读动作外，其余动作一律要求 POST，避免被 GET 链接/图片触发写操作
        if (!"POST".equalsIgnoreCase(req.getMethod()) && !READ_ONLY_ACTIONS.contains(action)) {
            JsonUtil.error(resp, 405, "该操作只允许使用 POST 请求");
            return;
        }

        try {
            method.setAccessible(true);
            method.invoke(this, req, resp);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof IllegalArgumentException) {
                // 例如请求体不是合法 JSON，属于客户端问题。
                // 具体原因只写日志，不回显给前端，避免泄露内部实现细节。
                logger.warn("请求参数不合法: {} {} action={}, 原因: {}", req.getMethod(), req.getRequestURI(), action, cause.getMessage());
                JsonUtil.error(resp, 400, "请求参数不合法");
                return;
            }
            logger.error("接口执行失败: {} {} action={}", req.getMethod(), req.getRequestURI(), action, cause);
            JsonUtil.error(resp, 500, "服务器内部错误，请稍后重试");
        } catch (IllegalAccessException e) {
            logger.error("接口无访问权限: action={}", action, e);
            JsonUtil.error(resp, 500, "服务器内部错误，请稍后重试");
        } catch (Exception e) {
            logger.error("反射分发失败: action={}", action, e);
            JsonUtil.error(resp, 500, "服务器内部错误，请稍后重试");
        }
    }
}
