package com.sorted.rest.services.payment.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.payment.beans.BulkActionPrRequest;
import com.sorted.rest.services.payment.beans.WalletTxnBean;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentRequestStatus;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletTxnMode;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.entity.PaymentRequestEntity;
import com.sorted.rest.services.payment.repository.PaymentRequestRepository;
import com.sorted.rest.services.payment.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentRequestService implements BaseService<PaymentRequestEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(PaymentRequestService.class);

	@Autowired
	private PaymentRequestRepository paymentRequestRepository;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private UserUtils userUtils;

	@Transactional(propagation = Propagation.REQUIRED)
	public PaymentRequestEntity save(PaymentRequestEntity entity) {
		PaymentRequestEntity result = paymentRequestRepository.save(entity);
		return result;
	}

	public PageAndSortResult<PaymentRequestEntity> getAllPaginatedPaymentRequests(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<PaymentRequestEntity> paymentRequests = null;
		paymentRequests = findPagedRecords(filters, sort, pageSize, pageNo);
		return paymentRequests;
	}

	public List<ErrorBean> bulkApproveOrRejectPaymentRequests(BulkActionPrRequest request) {
		List<ErrorBean> errors = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("id", request.getIds());
		Map<Long, PaymentRequestEntity> entityMap = findAllRecords(params).stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
		if (!request.getStatus().equals(PaymentRequestStatus.APPROVED) || !request.getStatus().equals(PaymentRequestStatus.REJECTED)) {
			for (Long id : request.getIds()) {
				if (!entityMap.containsKey(id)) {
					errors.add(ErrorBean.withError(null, String.format("Payment Requests with id : %s not found to process", id.toString()), null));
					continue;
				}
				PaymentRequestEntity entity = entityMap.get(id);
				if (!entity.getStatus().equals(PaymentRequestStatus.REQUESTED)) {
					errors.add(ErrorBean.withError(entity.getAmount().toString(), "Payment Requests not found in RECEIVED state", entity.getEntityId()));
					continue;
				}
				entity.setStatus(request.getStatus());
				entity.getMetadata().setApprovedBy(userUtils.getUserDetail(SessionUtils.getAuthUserId()));
				try {
					if (request.getStatus().equals(PaymentRequestStatus.APPROVED)) {
						savePrAndAdjustWallet(entity);
					} else {
						save(entity);
					}
				} catch (Exception e) {
					_LOGGER.error("Error while finalizing Payment Requests ", e);
					errors.add(ErrorBean.withError(entity.getAmount().toString(), e.getMessage(), entity.getEntityId()));
				}
			}
		} else {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "invalid action.", "action"));
		}
		return errors;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void savePrAndAdjustWallet(PaymentRequestEntity paymentRequestEntity) {
		if (paymentRequestEntity.getAmount().compareTo(0d) < 1) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Payment Requests of amount less than zero is not allowed", "amount"));
		}
		paymentRequestEntity = save(paymentRequestEntity);
		userWalletService.addOrDeduct(buildWalletTxnBean(paymentRequestEntity), paymentRequestEntity.getEntityId(), paymentRequestEntity.getEntityType(),
				PaymentConstants.PAYMENT_PR_KEY + paymentRequestEntity.getId().toString());
	}

	private WalletTxnBean buildWalletTxnBean(PaymentRequestEntity paymentRequest) {
		WalletTxnBean transaction = new WalletTxnBean();
		BigDecimal amount = BigDecimal.valueOf(paymentRequest.getAmount());
		if (paymentRequest.getTxnMode().equals(WalletTxnMode.DEBIT.toString())) {
			amount = amount.multiply(BigDecimal.valueOf(-1d));
		}
		transaction.setAmount(amount.doubleValue());
		transaction.setTxnType(paymentRequest.getTxnType());
		transaction.setTxnDetail(paymentRequest.getTxnDetail());
		transaction.setWalletType(WalletType.fromString(paymentRequest.getWalletType()));
		transaction.setRemarks(paymentRequest.getRemarks());
		_LOGGER.info(String.format(String.format("Created wallet payload for payment request %s", paymentRequest.getId())));
		return transaction;
	}

	@Override
	public Class<PaymentRequestEntity> getEntity() {
		return PaymentRequestEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return paymentRequestRepository;
	}

}