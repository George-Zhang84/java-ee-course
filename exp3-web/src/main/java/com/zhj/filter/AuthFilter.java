package com.zhj.filter;

import com.zhj.dao.UserDao;
import com.zhj.infrastructure.JsonUtil;
import com.zhj.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 统一登录 + 角色拦截。
 *
 * <p>第一道防线是登录校验，第二道防线是按 URL/action 的角色白名单，
 * 与 Servlet 内部的业务级校验形成双重防护。</p>
 *
 * <p>注意：本系统是 JSP 同源部署，不对外提供跨域接口，
 * 因此不再根据 Origin 反射 CORS 头（原来的写法等于允许任意站点携带 Cookie 跨域调用）。</p>
 */
@WebFilter("/api/*")
public class AuthFilter implements Filter {

    private final UserDao userDao = new UserDao();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getServletPath();
        String action = req.getParameter("action");

        // 登录、注册放行
        if ("/api/auth".equals(path) && ("login".equals(action) || "register".equals(action))) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User loginUser = session == null ? null : (User) session.getAttribute("LOGIN_USER");
        if (loginUser == null || loginUser.getId() == null) {
            JsonUtil.error(resp, 401, "尚未登录或登录已过期，请重新登录");
            return;
        }

        // 每次请求都校验账号当前状态：管理员封禁账号后，已登录的会话应立即失效，
        // 同时顺带刷新余额/角色，避免 Session 中的数据过期。
        User freshUser = userDao.findById(loginUser.getId());
        if (freshUser == null || freshUser.getStatus() == null || freshUser.getStatus() != 1) {
            session.invalidate();
            JsonUtil.error(resp, 401, "账号已被禁用，请联系管理员");
            return;
        }
        freshUser.setPassword(null);
        session.setAttribute("LOGIN_USER", freshUser);

        if (!isAllowed(path, action, freshUser.getRoleCode())) {
            JsonUtil.error(resp, 403, "无权限执行该操作");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * URL/action 级别的角色白名单：只拦截明确属于管理端的接口。
     * 其余接口的具体归属（买家/卖家）仍由各 Servlet 结合数据归属做细粒度校验。
     */
    private boolean isAllowed(String path, String action, String roleCode) {
        boolean admin = "ADMIN".equals(roleCode) || "SUPER_ADMIN".equals(roleCode);
        if ("/api/audit".equals(path) || "/api/user".equals(path)) {
            return admin;
        }
        if ("/api/product".equals(path) && ("pendingList".equals(action) || "audit".equals(action))) {
            return admin;
        }
        return !("/api/review".equals(path) && "delete".equals(action)) || admin;
    }
}
