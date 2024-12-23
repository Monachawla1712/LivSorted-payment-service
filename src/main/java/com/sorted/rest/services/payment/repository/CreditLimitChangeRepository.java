package com.sorted.rest.services.payment.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.payment.entity.CreditLimitChangeEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface CreditLimitChangeRepository extends BaseCrudRepository<CreditLimitChangeEntity, Long> {

	@Query("select clc from CreditLimitChangeEntity clc WHERE clc.storeId = :storeId and clc.date = :date and clc.active = 1")
	CreditLimitChangeEntity findByStoreIdAndDate(String storeId, Date date);

	@Query("select clc from CreditLimitChangeEntity clc WHERE clc.storeId in :storeIds and clc.date = :date and clc.active = 1")
	List<CreditLimitChangeEntity> findByStoreIdsAndDate(List<String> storeIds, Date date);

}
