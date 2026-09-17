package com.restro.Impl;

import com.restro.entity.AssistanceRequest;
import com.restro.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Pushes a lightweight "something changed, id=X" ping over WebSocket so the owner board and the
 *  customer tracking page can refresh instantly; the AJAX poll remains the source of truth.
 *  No interface (matches the reference project's plain EmailService, which is also a helper
 *  component consumed by another Impl class rather than a business Service in its own right). */
@Component
public class OrderEventPublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void orderChanged(Order order) {
        messagingTemplate.convertAndSend("/topic/owner-orders",
                Map.of("orderId", order.getOrderId(), "orderNo", order.getOrderNo(), "status", order.getStatus().name()));
        messagingTemplate.convertAndSend("/topic/order-" + order.getOrderNo(),
                Map.of("status", order.getStatus().name()));
        // Lets every phone showing this table's shared "already ordered" list refresh instantly
        // instead of waiting for its next poll tick.
        messagingTemplate.convertAndSend("/topic/table-orders-" + order.getTable().getTableId(),
                Map.of("status", order.getStatus().name()));
    }

    public void assistanceRequestChanged(AssistanceRequest request) {
        messagingTemplate.convertAndSend("/topic/owner-assistance",
                Map.of("assistanceRequestId", request.getAssistanceRequestId(), "status", request.getStatus().name()));
    }
}
