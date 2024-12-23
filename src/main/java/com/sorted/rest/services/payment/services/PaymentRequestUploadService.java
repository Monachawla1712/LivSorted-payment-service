package com.sorted.rest.services.payment.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.payment.beans.PaymentRequestUploadBean;
import com.sorted.rest.services.payment.beans.UserDetail;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentRequestStatus;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletTxnMode;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.entity.PaymentRequestEntity;
import com.sorted.rest.services.payment.repository.PaymentRequestRepository;
import com.sorted.rest.services.payment.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentRequestUploadService {

	static AppLogger _LOGGER = LoggingManager.getLogger(PaymentRequestUploadService.class);

	@Autowired
	private PaymentRequestRepository prRepository;

	@Autowired
	private UserUtils userUtils;

	@Autowired
	private ClientService clientService;

	public List<PaymentRequestUploadBean> preProcessPaymentRequestsUpload(List<PaymentRequestUploadBean> rawBeans, EntityType entityType, WalletType walletType) {
		HashSet<WalletType> walletTypes = Arrays.stream(WalletType.values()).collect(Collectors.toCollection(HashSet::new));
		UserDetail userDetail = userUtils.getUserDetail(SessionUtils.getAuthUserId());
		if (!walletTypes.contains(walletType)) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_VALUE, String.format("Invalid wallet type :%s", walletType.toString()), "walletType"));
		}
		rawBeans.forEach(bean -> {
			bean.setEntityType(entityType);
			bean.setWalletType(walletType);
			bean.setStatus(PaymentRequestStatus.REQUESTED);
			bean.getMetadata().setRequestedBy(userDetail);
		});

		List<PaymentRequestUploadBean> beans = sanitizeProjectedSrUpload(rawBeans);
		return beans;
	}

	private List<PaymentRequestUploadBean> sanitizeProjectedSrUpload(List<PaymentRequestUploadBean> rawBeans) {
		HashSet<String> storeIds = clientService.getStoreDetails(rawBeans.stream().map(PaymentRequestUploadBean::getEntityId).collect(Collectors.toSet()), null)
				.stream().map(s -> s.getId().toString()).collect(Collectors.toCollection(HashSet::new));
		//todo: uncomment duplicate row identification check in future (as per finance team)
		//        Set<String> processedKey = new HashSet<>();
		String paymentTypeLabels = ParamsUtils.getParam("PAYMENT_TYPE_LABELS", "Payment-CASH,Payment-UPI,Payment-BANK");
		HashSet<String> txnTypes = Arrays.asList(paymentTypeLabels.split(",")).stream().collect(Collectors.toCollection(HashSet::new));
		rawBeans.stream().forEach(bean -> {
			if (!storeIds.contains(bean.getEntityId())) {
				bean.getErrors().add(ErrorBean.withError(Errors.INVALID_VALUE, String.format("Entity id : %s not found", bean.getEntityId()), "entityId"));
			} else if (bean.getAmount().compareTo(0d) == 0) {
				bean.getErrors().add(ErrorBean.withError(Errors.INVALID_VALUE, String.format("Amount can not be zero for Txn Detail: %s", bean.getTxnDetail()),
						"amount"));
			} else if (!txnTypes.contains(bean.getTxnType())) {
				bean.getErrors().add(ErrorBean.withError(Errors.INVALID_VALUE,
						String.format("Transaction type can not be %s for Txn Detail: %s", bean.getTxnType(), bean.getTxnDetail()), "txnType"));
				//            } else if (processedKey.contains(bean.computedKey())) {
				//                bean.getErrors()
				//                        .add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Duplicate Transaction for the Txn Detail : %s", bean.getTxnDetail()), "computedKey"));
			} else {
				//                processedKey.add(bean.computedKey());
				if (bean.getAmount().compareTo(0d) == -1) {
					bean.setTxnMode(WalletTxnMode.DEBIT.toString());
					bean.setAmount(BigDecimal.valueOf(bean.getAmount()).multiply(BigDecimal.valueOf(-1d)).doubleValue());
				} else {
					bean.setTxnMode(WalletTxnMode.CREDIT.toString());
				}
			}
		});
		return rawBeans;
	}

	public void validatePaymentRequestOnUpload(PaymentRequestUploadBean bean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {
			if (CollectionUtils.isNotEmpty(bean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

	@Transactional
	public void bulkSave(List<PaymentRequestEntity> entityList) {
		prRepository.saveAll(entityList);
	}
}