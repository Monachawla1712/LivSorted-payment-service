package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.entity.CashCollectionEntity;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;

public interface CashCollectionRepository extends BaseCrudRepository<CashCollectionEntity, Long> {


	boolean existsByEntityIdAndDateAndSlotAndEntityType(String entityId, Date date, String slot, EntityType entityType);

	@Query(value = "SELECT * from payment.cash_collections where active = 1 AND status = :status AND JSONB_EXTRACT_PATH_TEXT(metadata, 'receivedBy', 'id') = :receivedBy AND JSONB_EXTRACT_PATH_TEXT(metadata, 'txnMode') = :txnMode AND ((CAST(CAST(:skipCheckCcIds as varchar) as BOOLEAN)) OR id IN :ccIds)", nativeQuery = true)
	List<CashCollectionEntity> getByStatusAndReceivedByAndTxnModeAndCcIds(String status, String receivedBy, String txnMode, List<Long> ccIds,
			Boolean skipCheckCcIds);

	@Query(value = "select exists(select 1 from payment.cash_collections where key = ?1)", nativeQuery = true)
	Boolean keyExists(String key);
}