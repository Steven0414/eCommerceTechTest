package com.ecommerce.notification.infrastructure.web;

import com.ecommerce.notification.application.service.NotificationService;
import com.ecommerce.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public Flux<Notification> getNotificationsByOrderId(@RequestParam UUID orderId) {
        log.debug("Getting notifications for order: {}", orderId);
        return notificationService.getNotificationsByOrderId(orderId);
    }
}
