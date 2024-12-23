package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.entity.WalletStatementEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface WalletStatementRepository extends BaseCrudRepository<WalletStatementEntity, Integer> {

	List<WalletStatementEntity> findAllStatementByEntityId(String entityId);

	@Query("select m from WalletStatementEntity m WHERE m.createdAt >= :from and m.createdAt <= :to and m.entityId = :storeId and m.walletType <> 'HOLD' and m.active = 1 order by m.createdAt ASC")
	List<WalletStatementEntity> findByWalletStatement(String storeId, Date from, Date to);

	@Query(value = "select exists(select 1 from payment.wallet_statement where key = ?1)", nativeQuery = true)
	Boolean keyExists(String key);

	@Query(value = "SELECT * FROM payment.wallet_statement ws WHERE ws.entity_id = :storeId AND ws.active = 1 AND lower(ws.wallet_type) IN ('wallet','hold') AND ws.entity_type = 'STORE' AND ws.txn_detail = (SELECT ws2.txn_detail FROM payment.wallet_statement ws2 WHERE ws2.active = 1 AND lower(ws2.entity_id) = lower(:storeId) AND ws2.txn_type = 'Franchise-Order' AND ((lower(ws2.wallet_type) = 'hold' AND ws2.txn_mode = 'CREDIT') OR (lower(ws2.wallet_type) = 'wallet' AND ws2.txn_mode = 'DEBIT')) AND ws2.entity_type = 'STORE' ORDER BY ws2.id DESC LIMIT 1)", nativeQuery = true)
	List<WalletStatementEntity> getLastOrderTransactions(String storeId);

	@Query(value = "SELECT ws FROM WalletStatementEntity ws WHERE ws.entityId = :storeId AND ws.id > :wsId AND ws.active = 1 AND lower(ws.walletType) = 'wallet' AND ws.txnType IN ('Payment-PG', 'Payment-CASH', 'Payment-BANK', 'Payment-UPI', 'Payment_CC', 'Payment') AND ws.entityType = 'STORE'")
	List<WalletStatementEntity> getAfterTransactionTopup(String storeId, Integer wsId);

	@Query(value = "WITH window_closing_balance AS (SELECT LEAST(balance,0) as balance FROM payment.wallet_statement w1 WHERE w1.entity_id = :storeId AND w1.active = 1 AND lower(w1.wallet_type) = 'wallet' AND w1.id < :wsId AND w1.created_at >= :date AND w1.entity_type = 'STORE' ORDER BY w1.id DESC LIMIT 1) SELECT CASE WHEN (SELECT balance FROM window_closing_balance) IS NULL THEN 0 ELSE COALESCE( (SELECT LEAST(balance,0) as balance FROM payment.wallet_statement w2 WHERE w2.entity_id = :storeId AND w2.active = 1 AND lower(w2.wallet_type) = 'wallet' AND w2.id < :wsId AND w2.created_at < :date AND w2.entity_type = 'STORE' ORDER BY w2.id DESC LIMIT 1), 0) - (SELECT balance FROM window_closing_balance) END as window_outstanding", nativeQuery = true)
	Double getBeforeTransactionWindowOutstanding(String storeId, Integer wsId, Date date);
}
