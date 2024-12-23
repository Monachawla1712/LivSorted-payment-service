package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.entity.EasebuzzVirtualAccountEntity;

import java.util.Optional;

public interface EasebuzzVirtualAccountRepository extends BaseCrudRepository<EasebuzzVirtualAccountEntity, Integer> {

	Optional<EasebuzzVirtualAccountEntity> findByEntityIdAndEntityTypeAndActive(String entityId, PaymentConstants.EntityType entityType, Integer active);

	Optional<EasebuzzVirtualAccountEntity> findByVirtualAccountIdAndActive(String virtualAccountId, Integer active);

}