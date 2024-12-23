package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.entity.TransactionEntity;

public interface TransactionRepository extends BaseCrudRepository<TransactionEntity, String> {

	TransactionEntity findByReferenceId(String referenceId);

}
