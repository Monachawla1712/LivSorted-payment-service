package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.entity.CashCollectionOtpEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public interface CashCollectionOtpRepository extends BaseCrudRepository<CashCollectionOtpEntity, Long> {

	@Query(value = "SELECT cco FROM CashCollectionOtpEntity cco WHERE cco.otp = :otp AND cco.active = 1 AND cco.verified = 0 AND cco.storeId = :storeId AND cco.expiry > :date")
	CashCollectionOtpEntity getPendingOtpForCashCollection(String storeId, String otp, Date date);

	@Transactional(propagation = Propagation.REQUIRED)
	@Modifying
	@Query(value = "UPDATE CashCollectionOtpEntity cco SET cco.active = 0 WHERE cco.storeId = :storeId")
	void deactivateOldCcOtps(String storeId);

	@Query(value = "SELECT cco FROM CashCollectionOtpEntity cco WHERE cco.expiry > :date AND cco.active = 1 AND cco.verified = 0 AND cco.storeId = :storeId")
	CashCollectionOtpEntity getPendingCcOtp(String storeId, Date date);
}