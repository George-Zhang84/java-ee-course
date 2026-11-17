package com.zhj.servlet.product;

import com.alibaba.fastjson2.JSONObject;
import com.zhj.dao.ProductDao;
import com.zhj.infrastructure.IdUtil;
import com.zhj.infrastructure.JsonUtil;
import com.zhj.model.Product;
import com.zhj.model.User;
import com.zhj.servlet.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WebServlet("/api/product")
public class ProductServlet extends BaseServlet {

    private static final Set<String> CATEGORIES = Set.of("电子", "教材", "生活", "运动", "其他");
    private static final Set<String> AUDIT_STATUS = Set.of("ON_SALE", "OFFLINE");
    private static final double MAX_PRICE = 999999.99;

    private final ProductDao productDao = new ProductDao();

    // 请求：GET /api/product?action=list&page=1&keyword=xxx&category=xxx&sortBy=xxx
    public void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String keyword = req.getParameter("keyword");
        String category = req.getParameter("category");
        String sortBy = req.getParameter("sortBy");

        // 分页参数做上下界保护，避免负数 OFFSET 或超大 pageSize 打挂数据库
        int page = parsePositive(req.getParameter("page"), 1);
        int pageSize = parsePositive(req.getParameter("pageSize"), 6);
        pageSize = Math.min(pageSize, 50);

        List<Product> list = productDao.findList(keyword, category, sortBy, page, pageSize);
        int total = productDao.count(keyword, category);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        JsonUtil.success(resp, result);
    }

    // 卖家发布
    public void create(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        if (!"SELLER".equals(user.getRoleCode())) {
            JsonUtil.error(resp, 403, "只有卖家可以发布商品");
            return;
        }
        JSONObject body = JsonUtil.readRequest(req);

        String name = trim(body.getString("name"));
        String category = body.getString("category");
        Double price = body.getDouble("price");
        String description = trim(body.getString("description"));

        if (name.isEmpty() || name.length() > 100) {
            JsonUtil.error(resp, 400, "商品名称长度需为 1-100 个字符");
            return;
        }
        if (!CATEGORIES.contains(category)) {
            JsonUtil.error(resp, 400, "商品分类不合法");
            return;
        }
        if (price == null || price <= 0 || price > MAX_PRICE) {
            JsonUtil.error(resp, 400, "商品价格必须大于 0 且不超过 " + MAX_PRICE + " 元");
            return;
        }
        if (description.length() > 1000) {
            JsonUtil.error(resp, 400, "商品描述不能超过 1000 个字符");
            return;
        }

        Product p = new Product();
        p.setId(IdUtil.next("P"));
        p.setSellerId(user.getId());
        p.setName(name);
        p.setCategory(category);
        p.setPrice(price);
        p.setDescription(description);
        p.setStatus("PENDING");

        if (productDao.insert(p)) {
            // 【审计日志】
            com.zhj.audit.AuditLogger.log(user.getUsername(), "发布商品", "商品名:" + p.getName() + ", 价格:" + p.getPrice());
            JsonUtil.success(resp, "发布成功，等待管理员审核");
        } else {
            JsonUtil.error(resp, 500, "发布失败");
        }
    }

    // 卖家获取自己的商品
    public void myList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        if (!"SELLER".equals(user.getRoleCode())) {
            JsonUtil.error(resp, 403, "只有卖家可以查看自己的发布");
            return;
        }
        List<Product> list = productDao.findBySellerId(user.getId());
        JsonUtil.success(resp, list);
    }

    // 管理员获取待审核商品
    public void pendingList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        if (!isAdmin(user)) {
            JsonUtil.error(resp, 403, "无权限");
            return;
        }
        List<Product> list = productDao.findPending();
        JsonUtil.success(resp, list);
    }

    // 管理员审核
    public void audit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getSession().getAttribute("LOGIN_USER");
        if (!isAdmin(user)) {
            JsonUtil.error(resp, 403, "无权限");
            return;
        }
        JSONObject body = JsonUtil.readRequest(req);
        String productId = body.getString("productId");
        String status = body.getString("status");

        if (productId == null || productId.trim().isEmpty()) {
            JsonUtil.error(resp, 400, "商品ID不能为空");
            return;
        }
        if (!AUDIT_STATUS.contains(status)) {
            JsonUtil.error(resp, 400, "非法的审核结果");
            return;
        }

        if (productDao.audit(productId, status)) {
            // 【审计日志】
            com.zhj.audit.AuditLogger.log(user.getUsername(), "审核商品", "商品ID:" + productId + ", 结果:" + status);
            JsonUtil.success(resp, "审核完成");
        } else {
            JsonUtil.error(resp, 400, "商品不存在或已被审核");
        }
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRoleCode()) || "SUPER_ADMIN".equals(user.getRoleCode());
    }

    private int parsePositive(String raw, int defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
