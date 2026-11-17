package com.zhj.servlet.order;

import com.alibaba.fastjson2.JSONObject;
import com.zhj.dao.UserDao;
import com.zhj.infrastructure.JsonUtil;
import com.zhj.model.Order;
import com.zhj.model.User;
import com.zhj.service.OrderService;
import com.zhj.servlet.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/order")
public class OrderServlet extends BaseServlet {

    private final OrderService orderService = new OrderService();
    private final UserDao userDao = new UserDao();

    // 下单
    public void create(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        if (!"BUYER".equals(user.getRoleCode())) {
            JsonUtil.error(resp, 403, "只有买家可以下单");
            return;
        }
        JSONObject body = JsonUtil.readRequest(req);
        String productId = body.getString("productId");

        String result = orderService.createOrder(user.getId(), productId);
        if ("success".equals(result)) {
            // 重新读取数据库中的最新余额，避免 Session 里的旧数据造成前端显示错误
            User freshUser = userDao.findById(user.getId());
            if (freshUser != null) {
                freshUser.setPassword(null);
                req.getSession().setAttribute("LOGIN_USER", freshUser);
            }

            // 【审计日志】
            com.zhj.audit.AuditLogger.log(user.getUsername(), "买家下单", "商品ID:" + productId);
            JsonUtil.success(resp, freshUser != null ? freshUser : user);
        } else {
            JsonUtil.error(resp, 400, result);
        }
    }

    // 获取订单列表
    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        List<Order> list = orderService.getOrders(user.getId());
        JsonUtil.success(resp, list);
    }

    // 更新订单状态
    public void updateStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        JSONObject body = JsonUtil.readRequest(req);
        String orderId = body.getString("orderId");
        String newStatus = body.getString("newStatus");

        String result = orderService.updateOrderStatus(orderId, newStatus, user.getId(), user.getRoleCode());
        if ("success".equals(result)) {
            // 【审计日志】
            com.zhj.audit.AuditLogger.log(user.getUsername(), "更新订单状态", "订单号:" + orderId + ", 状态变更为:" + newStatus);
            JsonUtil.success(resp, "操作成功");
        } else {
            JsonUtil.error(resp, 400, result);
        }
    }
}
