package com.zhj.exp4.service;

import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.dto.request.OfferCreateRequest;
import com.zhj.exp4.entity.BargainOffer;
import com.zhj.exp4.entity.Order;
import com.zhj.exp4.entity.Product;
import com.zhj.exp4.entity.SysUser;
import com.zhj.exp4.enums.OfferStatus;
import com.zhj.exp4.enums.ProductStatus;
import com.zhj.exp4.mapper.BargainOfferMapper;
import com.zhj.exp4.mapper.OrderMapper;
import com.zhj.exp4.mapper.ProductMapper;
import com.zhj.exp4.mapper.SysUserMapper;
import com.zhj.exp4.security.AuthInfo;
import com.zhj.exp4.security.LoginUser;
import com.zhj.exp4.service.impl.BargainServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 议价服务单元测试。
 *
 * <p>核心场景是"接受报价"的三步走：报价单 CAS → 商品 CAS → 生成订单。
 * 若商品已被别人买走（CAS 返回 0），必须整体失败且**不生成订单**，
 * 否则就会出现"报价被接受但商品已售出"的脏数据。
 */
@DisplayName("议价接受与并发保护")
class BargainServiceImplTest {

    private BargainOfferMapper offerMapper;
    private ProductMapper productMapper;
    private OrderMapper orderMapper;
    private SysUserMapper userMapper;
    private NotificationService notificationService;
    private BargainServiceImpl bargainService;

    private static final long SELLER_ID = 5L;
    private static final long BUYER_ID = 4L;
    private static final long OFFER_ID = 1L;
    private static final long PRODUCT_ID = 2L;

    @BeforeEach
    void setUp() {
        offerMapper = Mockito.mock(BargainOfferMapper.class);
        productMapper = Mockito.mock(ProductMapper.class);
        orderMapper = Mockito.mock(OrderMapper.class);
        userMapper = Mockito.mock(SysUserMapper.class);
        notificationService = Mockito.mock(NotificationService.class);
        bargainService = new BargainServiceImpl(offerMapper, productMapper, orderMapper,
                userMapper, notificationService, new AppProperties());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("卖家接受报价：锁定商品、生成议价订单、金额取报价")
    void acceptCreatesBargainOrder() {
        loginAs(SELLER_ID, "seller1", "USER", "SELLER");
        when(offerMapper.selectById(OFFER_ID)).thenReturn(pendingOffer());
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(onSaleProduct());
        when(offerMapper.markHandled(eq(OFFER_ID), eq(OfferStatus.ACCEPTED.name()), any())).thenReturn(1);
        when(productMapper.casStatus(eq(PRODUCT_ID),
                eq(ProductStatus.ON_SALE.name()), eq(ProductStatus.LOCKED.name()))).thenReturn(1);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return 1;
        });

        Long orderId = bargainService.accept(OFFER_ID);

        assertEquals(100L, orderId);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order saved = captor.getValue();
        assertEquals(0, new BigDecimal("180.00").compareTo(saved.getAmount()));
        assertEquals("BARGAIN", saved.getOrigin());
        assertEquals(OFFER_ID, saved.getOfferId());
        assertEquals(BUYER_ID, saved.getBuyerId());
        assertEquals(SELLER_ID, saved.getSellerId());
        assertEquals("UNPAID", saved.getStatus());
    }

    @Test
    @DisplayName("商品已被抢走：接受失败且不生成订单（事务回滚）")
    void acceptFailsWhenProductLocked() {
        loginAs(SELLER_ID, "seller1", "USER", "SELLER");
        when(offerMapper.selectById(OFFER_ID)).thenReturn(pendingOffer());
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(onSaleProduct());
        when(offerMapper.markHandled(anyLong(), anyString(), any())).thenReturn(1);
        // 关键：商品 CAS 失败
        when(productMapper.casStatus(anyLong(), anyString(), anyString())).thenReturn(0);

        BizException ex = assertThrows(BizException.class, () -> bargainService.accept(OFFER_ID));
        assertEquals(ResultCode.STATE_CONFLICT.getCode(), ex.getCode());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("非卖家不能接受报价（水平越权）")
    void nonSellerCannotAccept() {
        loginAs(BUYER_ID, "buyer2", "USER");
        when(offerMapper.selectById(OFFER_ID)).thenReturn(pendingOffer());

        BizException ex = assertThrows(BizException.class, () -> bargainService.accept(OFFER_ID));
        assertEquals(ResultCode.OWNERSHIP_DENIED.getCode(), ex.getCode());
        verify(offerMapper, never()).markHandled(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("出价次数达到上限时拒绝发起新报价")
    void rejectWhenMaxAttemptsReached() {
        loginAs(BUYER_ID, "buyer2", "USER");
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(onSaleProduct());
        when(offerMapper.countPending(PRODUCT_ID, BUYER_ID)).thenReturn(0);
        when(offerMapper.countTotal(PRODUCT_ID, BUYER_ID)).thenReturn(3);

        OfferCreateRequest request = new OfferCreateRequest();
        request.setProductId(PRODUCT_ID);
        request.setOfferPrice(new BigDecimal("180.00"));

        BizException ex = assertThrows(BizException.class, () -> bargainService.createOffer(request));
        assertTrue(ex.getMessage().contains("最多出价"));
        verify(offerMapper, never()).insert(any(BargainOffer.class));
    }

    @Test
    @DisplayName("出价必须低于售价")
    void rejectOfferNotLowerThanPrice() {
        loginAs(BUYER_ID, "buyer2", "USER");
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(onSaleProduct());

        OfferCreateRequest request = new OfferCreateRequest();
        request.setProductId(PRODUCT_ID);
        request.setOfferPrice(new BigDecimal("200.00"));

        BizException ex = assertThrows(BizException.class, () -> bargainService.createOffer(request));
        assertTrue(ex.getMessage().contains("低于售价"));
    }

    // ------------------------------------------------------------------

    private void loginAs(long userId, String username, String... roles) {
        AuthInfo info = AuthInfo.builder()
                .userId(userId)
                .username(username)
                .nickname(username)
                .roles(List.of(roles))
                .permissions(Set.of())
                .build();
        LoginUser loginUser = new LoginUser(info);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private BargainOffer pendingOffer() {
        BargainOffer offer = new BargainOffer();
        offer.setId(OFFER_ID);
        offer.setProductId(PRODUCT_ID);
        offer.setBuyerId(BUYER_ID);
        offer.setSellerId(SELLER_ID);
        offer.setOfferPrice(new BigDecimal("180.00"));
        offer.setStatus(OfferStatus.PENDING.name());
        return offer;
    }

    private Product onSaleProduct() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSellerId(SELLER_ID);
        product.setTitle("机械键盘 青轴 87 键");
        product.setPrice(new BigDecimal("200.00"));
        product.setAllowBargain(1);
        product.setStatus(ProductStatus.ON_SALE.name());
        return product;
    }
}
