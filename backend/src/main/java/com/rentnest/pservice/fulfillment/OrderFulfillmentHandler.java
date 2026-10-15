package com.rentnest.pservice.fulfillment;

import com.rentnest.pservice.entity.ProductOrder;

/**
 * 配送履约扩展点
 * 当前只做状态流转，不接支付与第三方配送，
 * 未来接入自建配送或第三方平台时，替换本接口的实现 Bean 即可，业务代码不感知
 */
public interface OrderFulfillmentHandler {
    /**
     * 订单进入待配送（CREATED → DELIVERING）时的派单动作
     */
    void dispatch(ProductOrder order);

    /**
     * 订单取消时的回收动作，如通知配送方撤单
     */
    void onCancel(ProductOrder order);
}
