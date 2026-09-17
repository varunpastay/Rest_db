package com.restro.Impl;

import com.restro.Repo.AssistanceRequestRepository;
import com.restro.Service.AssistanceRequestService;
import com.restro.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssistanceRequestServiceImpl implements AssistanceRequestService {

    @Autowired
    private AssistanceRequestRepository assistanceRequestRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @Override
    @Transactional
    public AssistanceRequest raise(Restaurant restaurant, RestaurantTable table, AssistanceRequestType type, String message) {
        AssistanceRequest request = AssistanceRequest.builder()
                .restaurant(restaurant)
                .table(table)
                .requestType(type)
                .message(message)
                .status(AssistanceRequestStatus.OPEN)
                .build();
        AssistanceRequest saved = assistanceRequestRepository.save(request);
        eventPublisher.assistanceRequestChanged(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssistanceRequest> openRequests(Integer restaurantId) {
        return assistanceRequestRepository.findByRestaurant_RestaurantIdAndStatusOrderByCreatedAtAsc(
                restaurantId, AssistanceRequestStatus.OPEN);
    }

    @Override
    @Transactional
    public AssistanceRequest resolve(Integer requestId) {
        AssistanceRequest request = assistanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        request.setStatus(AssistanceRequestStatus.RESOLVED);
        request.setResolvedAt(LocalDateTime.now());
        AssistanceRequest saved = assistanceRequestRepository.save(request);
        eventPublisher.assistanceRequestChanged(saved);
        return saved;
    }
}
