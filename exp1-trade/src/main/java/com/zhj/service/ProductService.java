package com.zhj.service;

import com.zhj.exception.InvalidOperationException;
import com.zhj.exception.ProductNotFoundException;
import com.zhj.model.Product;
import com.zhj.model.enums.Category;
import com.zhj.model.enums.ProductStatus;
import com.zhj.repository.DataStore;
import com.zhj.strategy.SortStrategy;
import com.zhj.util.IdGenerator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品业务逻辑层
 */
public class ProductService {

    private final DataStore store = DataStore.getInstance();

    // ================== 卖家操作 ==================

    /**
     * 卖家发布商品，初始状态为待审核
     */
    public Product publishProduct(String sellerId, String name, String description,
                                  double price, Category category) {
        if (price <= 0) {
            throw new InvalidOperationException("商品价格必须大于 0");
        }
        Product product = new Product(
                IdGenerator.nextProductId(),
                name, description, price, category, sellerId
        );
        store.addProduct(product);
        return product;
    }

    /**
     * 卖家编辑商品（仅待审核或已下架状态可编辑）
     */
    public void editProduct(String productId, String sellerId, String newName,
                            String newDesc, double newPrice) {
        Product p = getProductOrThrow(productId);
        checkOwner(p, sellerId);
        if (p.getStatus() != ProductStatus.PENDING && p.getStatus() != ProductStatus.OFF_SHELF) {
            throw new InvalidOperationException("当前状态不可编辑，仅待审核或已下架的商品可编辑");
        }
        if (newPrice <= 0) {
            throw new InvalidOperationException("价格必须大于 0");
        }
        p.setName(newName);
        p.setDescription(newDesc);
        p.setPrice(newPrice);
        // 修改后重新进入待审核
        p.setStatus(ProductStatus.PENDING);
    }

    /**
     * 卖家下架商品
     */
    public void offShelf(String productId, String sellerId) {
        Product p = getProductOrThrow(productId);
        checkOwner(p, sellerId);
        if (p.getStatus() != ProductStatus.ON_SALE) {
            throw new InvalidOperationException("只有【在售】状态可以下架");
        }
        p.setStatus(ProductStatus.OFF_SHELF);
    }

    /**
     * 卖家重新上架（已审核过的商品无需再审）
     */
    public void reOnShelf(String productId, String sellerId) {
        Product p = getProductOrThrow(productId);
        checkOwner(p, sellerId);
        if (p.getStatus() != ProductStatus.OFF_SHELF) {
            throw new InvalidOperationException("只有【已下架】状态可以重新上架");
        }
        p.setStatus(ProductStatus.ON_SALE);
    }

    // ================== 管理员操作 ==================

    /**
     * 管理员审核通过
     */
    public void approveProduct(String productId) {
        Product p = getProductOrThrow(productId);
        if (p.getStatus() != ProductStatus.PENDING) {
            throw new InvalidOperationException("只有【待审核】状态可以审核");
        }
        p.setStatus(ProductStatus.ON_SALE);
    }

    /**
     * 管理员驳回
     */
    public void rejectProduct(String productId) {
        Product p = getProductOrThrow(productId);
        if (p.getStatus() != ProductStatus.PENDING) {
            throw new InvalidOperationException("只有【待审核】状态可以驳回");
        }
        p.setStatus(ProductStatus.REJECTED);
    }

    /**
     * 管理员下架违规商品
     */
    public void adminOffShelf(String productId) {
        Product p = getProductOrThrow(productId);
        p.setStatus(ProductStatus.OFF_SHELF);
    }

    // ================== 买家操作 ==================

    /**
     * 查看商品详情（会自动增加浏览量）
     */
    public Product viewProductDetail(String productId) {
        Product p = getProductOrThrow(productId);
        p.setViewCount(p.getViewCount() + 1);
        return p;
    }

    /**
     * 浏览所有在售商品
     */
    public List<Product> listOnSaleProducts() {
        return store.findProductsByStatus(ProductStatus.ON_SALE);
    }

    /**
     * 按分类筛选在售商品
     */
    public List<Product> listOnSaleByCategory(Category category) {
        return store.findProductsByStatus(ProductStatus.ON_SALE).stream()
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * 关键词搜索在售商品
     */
    public List<Product> searchOnSaleProducts(String keyword) {
        return store.searchProductsByName(keyword).stream()
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE)
                .collect(Collectors.toList());
    }

    /**
     * 使用排序策略对商品列表排序
     */
    public List<Product> sortProducts(List<Product> products, SortStrategy strategy) {
        return strategy.sort(products);
    }

    // ================== 分页 ==================

    /**
     * 分页显示
     *
     * @return 分页对象：包含本页数据和总页数
     */
    public PageResult<Product> paginate(List<Product> products, int page, int pageSize) {
        int total = products.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        if (totalPages == 0) totalPages = 1;

        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);

        List<Product> pageData = (from >= total)
                ? List.of()
                : products.subList(from, to);

        return new PageResult<>(pageData, total, page, totalPages);
    }

    // ================== 查询 ==================

    public Product getProductById(String id) {
        return getProductOrThrow(id);
    }

    public List<Product> listProductsBySeller(String sellerId) {
        return store.findProductsBySeller(sellerId);
    }

    public List<Product> listPendingProducts() {
        return store.findProductsByStatus(ProductStatus.PENDING);
    }

    // ================== 内部工具 ==================

    private Product getProductOrThrow(String id) {
        Product p = store.findProductById(id);
        if (p == null) {
            throw new ProductNotFoundException("商品 ID [" + id + "] 不存在");
        }
        return p;
    }

    private void checkOwner(Product p, String sellerId) {
        if (!p.getSellerId().equals(sellerId)) {
            throw new InvalidOperationException("无权操作他人的商品");
        }
    }

    /**
     * 分页结果封装类（内部类，仅本类使用）
     */
    public static class PageResult<T> {
        public final List<T> data;
        public final int total;
        public final int currentPage;
        public final int totalPages;

        public PageResult(List<T> data, int total, int currentPage, int totalPages) {
            this.data = data;
            this.total = total;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
        }

        public boolean hasNext() {
            return currentPage < totalPages;
        }

        public boolean hasPrev() {
            return currentPage > 1;
        }
    }
}