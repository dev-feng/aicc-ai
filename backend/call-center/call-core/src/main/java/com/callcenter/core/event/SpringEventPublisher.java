package com.callcenter.core.event;

import com.callcenter.common.event.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring Event 的事件发布实现。
 */
@Component
public class SpringEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 构造 Spring 事件发布器。
     *
     * @param applicationEventPublisher spring publisher
     */
    public SpringEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 发布领域事件到 Spring 事件总线。
     *
     * @param event domain event
     */
    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}