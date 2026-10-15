package com.rentnest.pservice.fulfillment;

import com.rentnest.pservice.entity.ProductOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认实现：仅记录日志，不做任何外部调用
 */
@Slf4j
@Component
public class StatusOnlyFulfillmentHandler implements OrderFulfillmentHandler {
    @Override
    public void dispatch(ProductOrder order) {
        log.info("order dispatched (status-only), orderNo={}", order.getOrderNo());
    }

    @Override
    public void onCancel(ProductOrder order) {
        log.info("order cancelled (status-only), orderNo={}", order.getOrderNo());
    }
}
