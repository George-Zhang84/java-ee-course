package com.zhj.servlet.user;

import com.alibaba.fastjson2.JSONObject;
import com.zhj.dao.UserDao;
import com.zhj.infrastructure.JsonUtil;
import com.zhj.model.User;
import com.zhj.servlet.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/user")
public class UserServlet extends BaseServlet {

    private final UserDao userDao = new UserDao();

    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User loginUser = currentUser(req);
        if (loginUser == null) {
            JsonUtil.error(resp, 401, "尚未登录");
            return;
        }
        if (!isAdmin(loginUser)) {
            JsonUtil.error(resp, 403, "无权限查看用户列表");
            return;
        }
        String keyword = req.getParameter("keyword");
        String roleCode = req.getParameter("roleCode");
        List<User> users = userDao.findAll(keyword, roleCode);
        JsonUtil.success(resp, users);
    }

    public void updateStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User loginUser = currentUser(req);
        if (loginUser == null) {
            JsonUtil.error(resp, 401, "尚未登录");
            return;
        }
        if (!isAdmin(loginUser)) {
            JsonUtil.error(resp, 403, "无权限操作");
            return;
        }

        JSONObject body = JsonUtil.readRequest(req);
        String targetUserId = body.getString("userId");
        Integer status = body.getInteger("status");

        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            JsonUtil.error(resp, 400, "用户ID不能为空");
            return;
        }
        if (status == null || (status != 0 && status != 1)) {
            JsonUtil.error(resp, 400, "非法的用户状态");
            return;
        }

        User targetUser = userDao.findById(targetUserId);
        if (targetUser == null) {
            JsonUtil.error(resp, 404, "用户不存在");
            return;
        }

        // 防呆设计：不能禁用自己
        if (loginUser.getId().equals(targetUserId)) {
            JsonUtil.error(resp, 400, "不能禁用自己的账号！");
            return;
        }
        // 防提权：普通管理员只能管理买家/卖家，不能操作任何管理员账号
        if ("ADMIN".equals(loginUser.getRoleCode())
                && ("ADMIN".equals(targetUser.getRoleCode()) || "SUPER_ADMIN".equals(targetUser.getRoleCode()))) {
            JsonUtil.error(resp, 403, "您无权操作管理员账号！");
            return;
        }

        if (userDao.updateStatus(targetUserId, status)) {
            JsonUtil.success(resp, "操作成功");
        } else {
            JsonUtil.error(resp, 500, "操作失败");
        }
    }

    private User currentUser(HttpServletRequest req) {
        return (User) req.getSession().getAttribute("LOGIN_USER");
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRoleCode()) || "SUPER_ADMIN".equals(user.getRoleCode());
    }
}
