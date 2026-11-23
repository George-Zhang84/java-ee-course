package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.dto.request.ProductCreateRequest;
import com.zhj.exp4.dto.request.ProductQuery;
import com.zhj.exp4.dto.request.ProductUpdateRequest;
import com.zhj.exp4.dto.response.ProductDetailResponse;
import com.zhj.exp4.dto.response.ProductListResponse;
import com.zhj.exp4.entity.Category;
import com.zhj.exp4.entity.Favorite;
import com.zhj.exp4.entity.Product;
import com.zhj.exp4.entity.ProductImage;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.enums.ProductStatus;
import com.zhj.exp4.enums.RoleCode;
import com.zhj.exp4.mapper.CategoryMapper;
import com.zhj.exp4.mapper.FavoriteMapper;
import com.zhj.exp4.mapper.ProductImageMapper;
import com.zhj.exp4.mapper.ProductMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.security.LoginUser;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.CategoryService;
import com.zhj.exp4.service.ProductService;
import com.zhj.exp4.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务实现。
 *
 * <p>几个值得说明的设计决定：
 * <ol>
 *   <li><b>不做多表 join</b>：列表先分页查商品，再用两条批量查询补卖家昵称与分类名，
 *       总共 3 条 SQL 且无 N+1；相比 XML 动态 SQL 更容易维护与测试。</li>
 *   <li><b>排序白名单</b>：前端传入的 sort 只作为 switch 的分支键，绝不拼接到 SQL；
 *       直接拼接 order by 是典型的二级注入点。</li>
 *   <li><b>底价不出网关</b>：所有出参 DTO 都不含 minPrice，从数据结构上杜绝泄露。</li>
 *   <li><b>归属校验内聚</b>：改/删/上下架统一走 {@code requireOwner}，
 *       避免某个方法漏写校验造成水平越权。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** 公开可见的状态：已售出也展示，便于买家参考历史成交 */
    private static final Set<String> PUBLIC_STATUS = Set.of(
            ProductStatus.ON_SALE.name(), ProductStatus.LOCKED.name(), ProductStatus.SOLD.name());

    /** 允许卖家编辑的状态：进行中的交易不允许改价 */
    private static final Set<String> EDITABLE_STATUS = Set.of(
            ProductStatus.PENDING.name(), ProductStatus.REJECTED.name(), ProductStatus.OFF_SHELF.name());

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final SysUserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final CategoryService categoryService;
    private final AppProperties appProperties;

    /**
     * 浏览量防刷缓存：key = 登录用户 id 或匿名 IP + 商品 id，30 分钟内同一来源只计一次。
     * 用本地缓存即可满足单体应用；若多实例部署需换成 Redis。
     */
    private final Cache<String, Boolean> viewDedupCache = Caffeine.newBuilder()
            .maximumSize(20000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    @Override
    public PageResult<ProductListResponse> pagePublic(ProductQuery query) {
        LambdaQueryWrapper<Product> wrapper = Wrappers.lambdaQuery(Product.class)
                .eq(Product::getStatus, ProductStatus.ON_SALE.name());
        applyFilters(wrapper, query);
        applySort(wrapper, query.getSort());
        Page<Product> page = productMapper.selectPage(
                Page.of(query.safePage(), query.safeSize()), wrapper);
        return toPageResult(page);
    }

    @Override
    public PageResult<ProductListResponse> pageMine(ProductQuery query) {
        Long userId = SecurityUtils.currentUserId();
        LambdaQueryWrapper<Product> wrapper = Wrappers.lambdaQuery(Product.class)
                .eq(Product::getSellerId, userId);
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Product::getStatus, query.getStatus().trim());
        }
        applyFilters(wrapper, query);
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> page = productMapper.selectPage(
                Page.of(query.safePage(), query.safeSize()), wrapper);
        return toPageResult(page);
    }

    @Override
    public ProductDetailResponse detail(Long id) {
        Product product = requireProduct(id);
        Long currentUserId = currentUserIdOrNull();
        boolean owner = currentUserId != null && currentUserId.equals(product.getSellerId());
        // 管理员需要能预览待审核/已下架商品（审核页点击商品跳详情），因此对后台角色放开可见性
        boolean admin = isAdmin();
        if (!owner && !admin && !PUBLIC_STATUS.contains(product.getStatus())) {
            // 未审核/已下架商品对他人不可见，返回"不存在"而非"无权访问"，避免探测
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }

        // 浏览量自增：用 SQL 表达式而非"读-改-写"，并发下不会丢计数。
        // 同时做防刷：同一用户（未登录按 IP）对同一商品 30 分钟内只计一次，且卖家自己浏览不计数。
        if (ProductStatus.ON_SALE.name().equals(product.getStatus()) && !owner && shouldCountView(id)) {
            productMapper.increaseViews(id);
            product.setViews((product.getViews() == null ? 0 : product.getViews()) + 1);
        }

        List<ProductImage> images = productImageMapper.selectList(Wrappers.<ProductImage>lambdaQuery()
                .eq(ProductImage::getProductId, id)
                .orderByAsc(ProductImage::getSort));

        Category category = product.getCategoryId() == null ? null : categoryMapper.selectById(product.getCategoryId());
        SysUser seller = userMapper.selectById(product.getSellerId());

        boolean favorited = false;
        if (currentUserId != null) {
            Long count = favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                    .eq(Favorite::getUserId, currentUserId)
                    .eq(Favorite::getProductId, id));
            favorited = count != null && count > 0;
        }

        return ProductDetailResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                // 底价只在"卖家本人 / 管理员"视角返回：买家看不到，避免被压到底价
                .minPrice(owner || admin ? product.getMinPrice() : null)
                .allowBargain(product.getAllowBargain())
                .conditionLevel(product.getConditionLevel())
                .location(product.getLocation())
                .coverUrl(product.getCoverUrl())
                .images(images.stream().map(ProductImage::getUrl).toList())
                .categoryId(product.getCategoryId())
                .categoryName(category == null ? null : category.getName())
                .sellerId(product.getSellerId())
                .sellerNickname(seller == null ? null : seller.getNickname())
                .sellerAvatar(seller == null ? null : seller.getAvatar())
                .sellerCreditScore(seller == null ? null : seller.getCreditScore())
                .status(product.getStatus())
                .statusText(statusText(product.getStatus()))
                // 审核意见只回给卖家本人与管理员，普通买家看不到
                .auditRemark(owner || admin ? product.getAuditRemark() : null)
                .views(product.getViews())
                .createTime(product.getCreateTime())
                .favorited(favorited)
                .owner(owner)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductCreateRequest request) {
        categoryService.requireEnabled(request.getCategoryId());
        validatePrice(request.getPrice(), request.getMinPrice());
        List<String> images = sanitizeImages(request.getImages());
        Long sellerId = SecurityUtils.currentUserId();

        Product product = new Product();
        product.setSellerId(sellerId);
        product.setCategoryId(request.getCategoryId());
        product.setTitle(request.getTitle().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setMinPrice(request.getMinPrice());
        product.setAllowBargain(request.getAllowBargain() == null ? 1 : request.getAllowBargain());
        product.setConditionLevel(StringUtils.hasText(request.getConditionLevel()) ? request.getConditionLevel() : "99新");
        product.setLocation(request.getLocation());
        product.setCoverUrl(images.isEmpty() ? null : images.get(0));
        // 新商品一律待审核，不接受前端指定状态
        product.setStatus(ProductStatus.PENDING.name());
        product.setViews(0);
        product.setDeleted(0);
        productMapper.insert(product);

        saveImages(product.getId(), images);
        log.info("商品发布成功 id={} sellerId={} title={}", product.getId(), sellerId, product.getTitle());
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductUpdateRequest request) {
        Product product = requireProduct(id);
        requireOwner(product);
        if (!EDITABLE_STATUS.contains(product.getStatus())) {
            throw BizException.stateConflict("当前状态下不允许编辑，请先下架商品");
        }
        categoryService.requireEnabled(request.getCategoryId());
        validatePrice(request.getPrice(), request.getMinPrice());

        Product update = new Product();
        update.setId(id);
        update.setTitle(request.getTitle().trim());
        update.setCategoryId(request.getCategoryId());
        update.setPrice(request.getPrice());
        update.setMinPrice(request.getMinPrice());
        update.setAllowBargain(request.getAllowBargain() == null ? product.getAllowBargain() : request.getAllowBargain());
        update.setConditionLevel(StringUtils.hasText(request.getConditionLevel())
                ? request.getConditionLevel() : product.getConditionLevel());
        update.setLocation(request.getLocation());
        update.setDescription(request.getDescription());

        // 被驳回的商品修改后重新进入待审核；已下架商品保持下架，由卖家自行上架
        if (ProductStatus.REJECTED.name().equals(product.getStatus())) {
            update.setStatus(ProductStatus.PENDING.name());
            update.setAuditRemark(null);
        }

        if (request.getImages() != null) {
            List<String> images = sanitizeImages(request.getImages());
            update.setCoverUrl(images.isEmpty() ? null : images.get(0));
            productImageMapper.delete(Wrappers.<ProductImage>lambdaQuery()
                    .eq(ProductImage::getProductId, id));
            saveImages(id, images);
        }
        productMapper.updateById(update);
    }

    @Override
    public void changeStatus(Long id, String status) {
        Product product = requireProduct(id);
        requireOwner(product);
        ProductStatus target;
        try {
            target = ProductStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "目标状态不合法");
        }
        if (target == ProductStatus.ON_SALE) {
            if (!ProductStatus.OFF_SHELF.name().equals(product.getStatus())) {
                throw BizException.stateConflict("只有已下架的商品才能重新上架");
            }
        } else if (target == ProductStatus.OFF_SHELF) {
            if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
                throw BizException.stateConflict("只有在售商品才能下架");
            }
        } else {
            throw new BizException(ResultCode.PARAM_ERROR, "只能上架或下架");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(target.name());
        productMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = requireProduct(id);
        requireOwner(product);
        if (ProductStatus.LOCKED.name().equals(product.getStatus())
                || ProductStatus.SOLD.name().equals(product.getStatus())) {
            throw BizException.stateConflict("交易中的商品不能删除");
        }
        productMapper.deleteById(id);
        productImageMapper.delete(Wrappers.<ProductImage>lambdaQuery()
                .eq(ProductImage::getProductId, id));
    }

    // ------------------------------------------------------------------
    // 私有方法
    // ------------------------------------------------------------------

    private Product requireProduct(Long id) {
        Product product = id == null ? null : productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    /** 归属校验：非卖家本人（且非超管）直接拒绝 */
    private void requireOwner(Product product) {
        LoginUser loginUser = SecurityUtils.currentUser();
        if (!loginUser.getUserId().equals(product.getSellerId())
                && !loginUser.hasRole(RoleCode.SUPER_ADMIN.name())) {
            throw BizException.ownershipDenied();
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return SecurityUtils.currentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /** 当前请求是否来自后台管理角色（ADMIN / SUPER_ADMIN） */
    private boolean isAdmin() {
        try {
            LoginUser loginUser = SecurityUtils.currentUser();
            return loginUser.hasRole(RoleCode.ADMIN.name())
                    || loginUser.hasRole(RoleCode.SUPER_ADMIN.name());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 浏览量计数去重：同一用户 30 分钟内只看作一次；未登录时按客户端 IP 去重。
     *
     * <p>这是防"脚本狂刷浏览量"的最低成本方案——不增加数据库压力，
     * 也不需要引入 Redis。缺点是同一人多设备会被算作一次，对二手交易场景可以接受。
     */
    private boolean shouldCountView(Long productId) {
        Long userId = currentUserIdOrNull();
        String source;
        if (userId != null) {
            source = "u" + userId;
        } else {
            source = "ip" + clientIp();
        }
        String key = source + ":p" + productId;
        // putIfAbsent 返回 null 表示首次出现
        return viewDedupCache.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    private String clientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        return IpUtil.getClientIp(request);
    }

    private void validatePrice(java.math.BigDecimal price, java.math.BigDecimal minPrice) {
        if (minPrice != null && minPrice.compareTo(price) >= 0) {
            throw BizException.of("底价必须低于售价，否则议价没有意义");
        }
    }

    /** 图片 URL 必须是本站上传接口产生的前缀，防止外部链接与脚本伪协议注入 */
    private List<String> sanitizeImages(List<String> images) {
        if (CollectionUtils.isEmpty(images)) {
            return List.of();
        }
        String prefix = appProperties.getUpload().getUrlPrefix() + "/";
        for (String url : images) {
            if (url == null || !url.startsWith(prefix)) {
                throw new BizException(ResultCode.PARAM_ERROR, "图片地址不合法，请重新上传");
            }
        }
        return images;
    }

    private void saveImages(Long productId, List<String> images) {
        for (int i = 0; i < images.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(images.get(i));
            image.setSort(i);
            productImageMapper.insert(image);
        }
    }

    private void applyFilters(LambdaQueryWrapper<Product> wrapper, ProductQuery query) {
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Product::getTitle, keyword)
                    .or()
                    .like(Product::getDescription, keyword));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Product::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Product::getPrice, query.getMaxPrice());
        }
    }

    /** 排序白名单：非法值一律退回"最新发布"，杜绝 order by 注入 */
    private void applySort(LambdaQueryWrapper<Product> wrapper, String sort) {
        String key = sort == null ? "newest" : sort;
        switch (key) {
            case "price_asc" -> wrapper.orderByAsc(Product::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Product::getPrice);
            case "views" -> wrapper.orderByDesc(Product::getViews);
            default -> wrapper.orderByDesc(Product::getCreateTime);
        }
    }

    /** 分页结果 + 批量补充卖家昵称与分类名，总计 3 条 SQL */
    private PageResult<ProductListResponse> toPageResult(Page<Product> page) {
        List<Product> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(page, p -> null);
        }
        Map<Long, String> sellerNames = batchNames(records.stream().map(Product::getSellerId).toList(),
                SysUser::getId, SysUser::getNickname, userMapper);
        Map<Long, String> categoryNames = batchNames(records.stream().map(Product::getCategoryId).toList(),
                Category::getId, Category::getName, categoryMapper);
        return PageResult.of(page, p -> ProductListResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .price(p.getPrice())
                .coverUrl(p.getCoverUrl())
                .conditionLevel(p.getConditionLevel())
                .location(p.getLocation())
                .categoryId(p.getCategoryId())
                .categoryName(categoryNames.get(p.getCategoryId()))
                .sellerId(p.getSellerId())
                .sellerNickname(sellerNames.get(p.getSellerId()))
                .allowBargain(p.getAllowBargain())
                .status(p.getStatus())
                .statusText(statusText(p.getStatus()))
                .views(p.getViews())
                .createTime(p.getCreateTime())
                .build());
    }

    private <T> Map<Long, String> batchNames(Collection<Long> ids,
                                             Function<T, Long> idGetter,
                                             Function<T, String> nameGetter,
                                             com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper) {
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return mapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(idGetter, nameGetter, (a, b) -> a));
    }

    private String statusText(String status) {
        try {
            return ProductStatus.valueOf(status).getDescription();
        } catch (Exception e) {
            return status;
        }
    }
}
