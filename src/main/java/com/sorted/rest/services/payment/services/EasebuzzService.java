package com.sorted.rest.services.payment.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.entity.EasebuzzVirtualAccountEntity;
import com.sorted.rest.services.payment.repository.EasebuzzVirtualAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EasebuzzService implements BaseService<EasebuzzVirtualAccountEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(EasebuzzService.class);

	@Autowired
	private EasebuzzVirtualAccountRepository easebuzzVirtualAccountRepository;

	@Transactional(propagation = Propagation.REQUIRED)
	public EasebuzzVirtualAccountEntity save(EasebuzzVirtualAccountEntity entity) {
		EasebuzzVirtualAccountEntity result = easebuzzVirtualAccountRepository.save(entity);
		return result;
	}

	public Optional<EasebuzzVirtualAccountEntity> findByEntityDetails(String entityId, PaymentConstants.EntityType entityType) {
		return easebuzzVirtualAccountRepository.findByEntityIdAndEntityTypeAndActive(entityId, entityType, 1);
	}

	public Optional<EasebuzzVirtualAccountEntity> findByVirtualAccountId(String virtualAccountId) {
		return easebuzzVirtualAccountRepository.findByVirtualAccountIdAndActive(virtualAccountId, 1);
	}

	@Override
	public Class<EasebuzzVirtualAccountEntity> getEntity() {
		return EasebuzzVirtualAccountEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return easebuzzVirtualAccountRepository;
	}

}