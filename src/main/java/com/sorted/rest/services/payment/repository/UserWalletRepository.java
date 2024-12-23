package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.entity.UserWalletEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserWalletRepository extends BaseCrudRepository<UserWalletEntity, UUID> {

	UserWalletEntity findByEntityId(String entityId);

	@Query("SELECT u FROM UserWalletEntity u WHERE u.entityType = ?1 AND (u.amount - u.walletHold) < ?2")
	List<UserWalletEntity> findEntitiesWithBalanceLessThanAmount(EntityType entityType, Double amount);

	List<UserWalletEntity> findAllByActiveAndEntityType(int active, EntityType entityType);
}
