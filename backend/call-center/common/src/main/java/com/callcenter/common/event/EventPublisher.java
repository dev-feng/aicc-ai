package com.callcenter.common.event;

/**
 * 抽象事件发布接口，屏蔽具体事件总线实现。
 */
public interface EventPublisher {

    /**
     * 发布领域事件。
     *
     * @param event domain event
     */
    void publish(Object event);
}