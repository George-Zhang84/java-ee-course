package com.zhj.servlet.auth;

import com.alibaba.fastjson2.JSONObject;
import com.zhj.dao.UserDao;
import com.zhj.infrastructure.JsonUtil;
import com.zhj.model.User;
import com.zhj.service.AuthService;
import com.zhj.servlet.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/auth")
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new UserDao();

    // 前端请求：POST /api/auth?action=login
    public void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1. 读取前端传来的 JSON 数据
        JSONObject body = JsonUtil.readRequest(req);
        String username = body.getString("username");
        String password = body.getString("password");

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            JsonUtil.error(resp, 400, "用户名和密码不能为空");
            return;
        }

        // 2. 业务层校验
        User user = authService.login(username, password);
        if (user == null) {
            JsonUtil.error(resp, 401, "用户名或密码错误，或账号被禁用");
            return;
        }

        // 3. 登录成功：获取该角色的权限列表，存入 Session
        List<String> permissions = userDao.findPermissionsByRole(user.getRoleCode());

        // 防会话固定攻击：先销毁可能存在的旧 Session，再创建新的
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = req.getSession(true);

        // 将用户和权限放入 Session 中（密码为了安全置空）
        user.setPassword(null);
        session.setAttribute("LOGIN_USER", user);
        session.setAttribute("USER_PERMISSIONS", permissions);

        // 4. 返回前端需要的数据（不包含密码）
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("permissions", permissions);

        JsonUtil.success(resp, result);
    }

    // 登出功能
    public void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate(); // 销毁 Session
        }
        JsonUtil.success(resp, "登出成功");
    }

    // 注册功能
    public void register(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject body = JsonUtil.readRequest(req);
        String username = body.getString("username");
        String password = body.getString("password");
        String roleCode = body.getString("roleCode"); // 前端传 BUYER 或 SELLER
        String shopName = body.getString("shopName"); // 卖家才有

        if (username == null || password == null || roleCode == null) {
            JsonUtil.error(resp, 400, "参数不完整");
            return;
        }

        String result = authService.register(username, password, roleCode, shopName);
        if ("success".equals(result)) {
            JsonUtil.success(resp, "注册成功");
        } else {
            JsonUtil.error(resp, 400, result);
        }
    }
}
