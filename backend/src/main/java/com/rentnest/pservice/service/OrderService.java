package com.rentnest.pservice.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.common.ratelimit.RateLimiter;
import com.rentnest.pservice.dto.OrderCreateReq;
import com.rentnest.pservice.dto.OrderQuery;
import com.rentnest.pservice.dto.OrderResp;
import com.rentnest.pservice.dto.OrderStatsResp;
import com.rentnest.pservice.entity.Product;
import com.rentnest.pservice.entity.ProductOrder;
import com.rentnest.pservice.entity.ProductOrderLog;
import com.rentnest.pservice.enums.OrderStatus;
import com.rentnest.pservice.enums.ProductStatus;
import com.rentnest.pservice.fulfillment.OrderFulfillmentHandler;
import com.rentnest.pservice.mapper.ProductOrderLogMapper;
import com.rentnest.pservice.mapper.ProductOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderService {
    @Resource
    private ProductOrderMapper orderMapper;
    @Resource
    private ProductOrderLogMapper orderLogMapper;
    @Resource
    private ProductService productService;
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private OrderFulfillmentHandler fulfillmentHandler;

    @Transactional
    public ProductOrder create(OrderCreateReq req, CurrentUser current) {
        if (!rateLimiter.tryAcquire("order:" + current.getId(), 5, Duration.ofMinutes(1))) {
            throw new BizException(ErrorCode.RATE_LIMITED, "下单过于频繁，请稍后再试");
        }

        Product product = productService.mustGet(req.getProductId());
        if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "商品已下架");
        }

        ProductOrder order = new ProductOrder();
        // 预生成 id，订单号由 id 派生，天然满足 uk_order_no
        long id = IdWorker.getId();
        order.setId(id);
        order.setOrderNo("NO" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + id);
        order.setUserId(current.getId());
        order.setProductId(product.getId());
        order.setQuantity(req.getQuantity());
        // 金额为下单时价格快照，商品后续调价不影响历史订单
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(req.getQuantity())));
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setDeliveryAddress(req.getDeliveryAddress());
        order.setRemark(req.getRemark());
        order.setStatus(OrderStatus.CREATED.name());
        orderMapper.insert(order);

        writeLog(id, null, OrderStatus.CREATED.name(), current.getId(), "用户下单");
        return order;
    }

    /** 用户仅可在开始配送前取消自己的订单 */
    @Transactional
    public ProductOrder cancelOwn(Long id, CurrentUser current) {
        ProductOrder order = mustGet(id);
        if (!order.getUserId().equals(current.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能取消自己的订单");
        }
        return moveTo(order, OrderStatus.CANCELLED, current, "用户取消");
    }

    @Transactional
    public ProductOrder updateStatus(Long id, String targetStatus, String remark, CurrentUser current) {
        ProductOrder order = mustGet(id);
        return moveTo(order, OrderStatus.parse(targetStatus), current, remark);
    }

    public PageResult<OrderResp> page(OrderQuery query) {
        long total = orderMapper.countByQuery(query);
        List<OrderResp> records = total == 0 ? List.of() : orderMapper.selectPage(query);
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    public List<OrderStatsResp> stats(OrderQuery query) {
        return orderMapper.statsByProduct(query);
    }

    public ProductOrder mustGet(Long id) {
        ProductOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private ProductOrder moveTo(ProductOrder order, OrderStatus to, CurrentUser current, String remark) {
        OrderStatus from = OrderStatus.valueOf(order.getStatus());
        if (!from.canMoveTo(to)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "订单状态不能从 " + from.name() + " 变更为 " + to.name());
        }
        order.setStatus(to.name());
        orderMapper.updateById(order);
        writeLog(order.getId(), from.name(), to.name(), current.getId(), remark);

        if (to == OrderStatus.DELIVERING) {
            fulfillmentHandler.dispatch(order);
        } else if (to == OrderStatus.CANCELLED) {
            fulfillmentHandler.onCancel(order);
        }
        return order;
    }

    private void writeLog(Long orderId, String from, String to, Long operatorId, String remark) {
        ProductOrderLog log = new ProductOrderLog();
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        orderLogMapper.insert(log);
    }
}
