package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.entity.PaymentRequestEntity;

public interface PaymentRequestRepository extends BaseCrudRepository<PaymentRequestEntity, Long> {
}