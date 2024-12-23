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
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.beans.BulkActionCcRequest.CcDetails;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.*;
import com.sorted.rest.services.payment.entity.CashCollectionEntity;
import com.sorted.rest.services.payment.entity.TransactionEntity;
import com.sorted.rest.services.payment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by mohit on 20.6.20.
 */
@Service
public class PaymentService implements BaseService<TransactionEntity> {

	private static final AppLogger _LOGGER = LoggingManager.getLogger(PaymentService.class);

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private CashCollectionService cashCollectionService;

	/**
	 * V2
	 *
	 * @param paymentRequest
	 * @param transaction
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void handleTxnV2(PaymentNotifyBean paymentRequest, PaymentMode paymentMode, String paymentGateway, TransactionEntity transaction) {
		if (transaction == null) {
			Optional<TransactionEntity> transactionOpt = transactionRepository.findById(paymentRequest.getId());
			_LOGGER.info(String.format("handleTxnV2:: transaction %s", transactionOpt.get()));
			if (!transactionOpt.isPresent() || !transactionOpt.get().getId().equals(paymentRequest.getId())) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Payment OrderId not matching", "orderId"));
			}
			transaction = transactionOpt.get();
		}
		if (transaction.getStatus().equals(PaymentConstants.PaymentStatus.SUCCESS.toString())) {
			return;
		}
		transaction = verifyAndUpdateTransaction(paymentRequest, transaction, paymentMode, paymentGateway);
		if (transaction.getStatus().equals(PaymentConstants.PaymentStatus.SUCCESS.toString())) {
			EntityType entityType = transaction.getEntityType();
			if (entityType.equals(EntityType.AM)) {
				List<Long> ccIds = transaction.getMetadata().getCcIds();
				List<CcDetails> ccDetails = prepareCcDetails(ccIds);
				List<ErrorBean> errors = cashCollectionService.bulkApproveOrRejectCashCollection(
						BulkActionCcRequest.builder().ccDetails(ccDetails).status(CashCollectionStatus.APPROVED).referenceId(transaction.getId()).build());
				_LOGGER.info(String.format("Cash collections %s approved with %d errors", ccIds, errors.size()));
			} else {
				WalletTxnBean walletPayload = addToWalletPayload(transaction.getAmount(), paymentRequest.getReferenceId());
				String key = null;
				if (PaymentMode.QR.equals(paymentMode)) {
					key = PaymentConstants.PAYMENT_QR_KEY + transaction.getReferenceId();
				} else if (PaymentMode.PG.equals(paymentMode) || PaymentMode.JUSPAY.equals(paymentMode)) {
					key = PaymentConstants.PAYMENT_PG_KEY + transaction.getReferenceId();
				}
				userWalletService.addOrDeduct(walletPayload, transaction.getEntityId(), entityType, key);
				_LOGGER.info(String.format("%s amount is added in wallet for customer %s", transaction.getAmount(), transaction.getCustomerId()));
			}
		}
	}

	private List<CcDetails> prepareCcDetails(List<Long> ccIds) {
		List<CcDetails> ccDetails = new ArrayList<>();
		ccIds.forEach(id -> {
			CcDetails details = CcDetails.newInstance();
			details.setId(id);
			details.setTxnMode("Payment_CC");
			details.setRemarks(PaymentConstants.AM_PAYMENT_REMARKS);
			ccDetails.add(details);
		});
		return ccDetails;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TransactionEntity verifyAndUpdateTransaction(PaymentNotifyBean paymentRequest, final TransactionEntity transaction, PaymentMode paymentMode,
			String paymentGateway) {
		if (transaction.getPaymentMode() == null && paymentMode != null) {
			transaction.setPaymentMode(paymentMode.toString());
		}
		if (transaction.getPaymentGateway() == null && paymentGateway != null) {
			transaction.setPaymentGateway(paymentGateway);
		}
		transaction.setProcessedAt(new Date());
		if (paymentRequest.getPaymentMode() != null) {
			transaction.setMedium(paymentRequest.getPaymentMode());
		}
		if (transaction.getReferenceId() == null && paymentRequest.getReferenceId() != null) {
			transaction.setReferenceId(paymentRequest.getReferenceId());
		}
		transaction.setError(paymentRequest.getTxMsg());
		transaction.setStatus(paymentRequest.getTxStatus());
		transaction.getMetadata().setPaymentNotification(paymentRequest);
		return save(transaction);
	}

	private WalletTxnBean addToWalletPayload(double amount, String paymentId) {
		WalletTxnBean transaction = new WalletTxnBean();
		transaction.setAmount(amount);
		transaction.setTxnType("Payment-PG");
		transaction.setTxnDetail(paymentId);
		_LOGGER.info(String.format(String.format("Created wallet payload for paymentId %s", paymentId)));
		return transaction;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TransactionEntity handleTxn(OrderBean order, PaymentGateway paymentGateway, PaymentNotifyBean paymentRequest, EntityType entityType) {
		TransactionEntity transaction = null;
		if (paymentRequest.getReferenceId() != null) {
			transaction = transactionRepository.findByReferenceId(paymentRequest.getReferenceId());
		}
		_LOGGER.info(String.format("handleTxn:: transaction %s", transaction));
		if (transaction != null && !transaction.getOrderId().equals(order.getId())) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Payment OrderId not matching", "orderId"));
		} else if (transaction == null) {
			transaction = new TransactionEntity();
			transaction.setCustomerId(order.getCustomerId());
			transaction.setEntityType(entityType);
			transaction.setOrderId(order.getId());
			transaction.setStoreId(order.getStoreId());

			transaction.setPaymentMode(PaymentMode.PG.toString());
			transaction.setPaymentGateway(paymentGateway.toString());
			transaction.setProcessedAt(new Date());
		}
		transaction.setMedium(paymentRequest.getPaymentMode());
		transaction.setAmount(Double.parseDouble(paymentRequest.getOrderAmount()));
		transaction.setReferenceId(paymentRequest.getReferenceId());
		transaction.setError(paymentRequest.getTxMsg());

		transaction.setStatus(paymentRequest.getTxStatus());

		transaction.setMetadata(buildMetadata(paymentRequest, order.getDisplayOrderId()));
		return save(transaction);
	}

	private TxnMetadata buildMetadata(PaymentNotifyBean paymentRequest, String displayOrderId) {
		TxnMetadata metadata = new TxnMetadata();
		metadata.setDisplayOrderId(displayOrderId);
		metadata.setPaymentNotification(paymentRequest);
		return metadata;
	}

	public UUID getCustomerId() {
		UUID customerId = SessionUtils.getAuthUserId();
		Assert.notNull(customerId, "CustomerId could not be empty");
		return customerId;
	}

	public String getStoreId() {
		String storeId = SessionUtils.getStoreId();
		return storeId;
	}

	public EntityType getEntityType() {
		String appType = SessionUtils.getAppId();
		Set<String> roles = SessionUtils.getAuthUserRoles();
		if (appType != null) {
			if (appType.equals(AppType.FOS.getValue()) && roles.contains(PaymentConstants.FOS_USER_ROLE)) {
				return EntityType.AM;
			} else if (appType.equals(AppType.PARTNER.getValue()) && roles.contains(PaymentConstants.PARTNER_APP_USER_ROLE) && getStoreId() != null) {
				return EntityType.STORE;
			} else if (appType.equals(AppType.CONSUMER.getValue())) {
				return EntityType.USER;
			}
		}
		throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "You are not Authorised to Recharge on this APP. Please use ", "userRole"));
	}

	public PageAndSortResult<TransactionEntity> findPurchaseOrdersByPage(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<TransactionEntity> poList = null;
		try {
			poList = findPagedRecords(filters, sort, pageSize, pageNo);
		} catch (Exception e) {
			_LOGGER.error(e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return poList;
	}

	private TransactionEntity save(TransactionEntity transaction) {
		return transactionRepository.save(transaction);
	}

	public TransactionEntity initiateTransaction(Double amount, PaymentMode paymentMode, String paymentGateway, PaymentStatus status, List<Long> ccIds) {
		UUID userId = getCustomerId();
		EntityType entityType = getEntityType();
		String entityId = entityType.equals(EntityType.STORE) ? getStoreId() : userId.toString();
		_LOGGER.info(String.format("initiateTransaction::Entity Type: %s, Entity Id: %s", entityType, entityId));
		TransactionEntity transaction = new TransactionEntity();
		transaction.setCustomerId(userId);
		transaction.setEntityType(entityType);
		transaction.setEntityId(entityId);
		transaction.setAmount(amount);
		transaction.setPaymentMode(paymentMode.toString());
		transaction.setPaymentGateway(paymentGateway);
		transaction.setStatus(status.toString());
		if (transaction.getEntityType().equals(EntityType.AM)) {
			validateFosCc(transaction, ccIds);
			transaction.getMetadata().setCcIds(ccIds);
		}
		return save(transaction);
	}

	private void validateFosCc(TransactionEntity entity, List<Long> ccIds) {
		if (CollectionUtils.isEmpty(ccIds)) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "AM must provide Cash Collections to initiate transactions", null));
		} else {
			Map<Long, CashCollectionEntity> validCcMap = cashCollectionService.fetchFosReceivedCCs(entity.getEntityId(), ccIds).stream()
					.collect(Collectors.toMap(CashCollectionEntity::getId, Function.identity()));
			BigDecimal totalAmount = BigDecimal.ZERO;
			for (Long ccId : ccIds) {
				if (!validCcMap.containsKey(ccId)) {
					throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, String.format("Invalid Cash Collection id %d", ccId), null));
				} else {
					totalAmount = totalAmount.add(BigDecimal.valueOf(validCcMap.get(ccId).getReceivedAmount()));
				}
			}
			entity.setAmount(totalAmount.doubleValue());
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void handleQrTxn(PaymentNotifyBean paymentRequest, String paymentGateway, EntityType entityType, String entityId) {
		TransactionEntity transaction = transactionRepository.findByReferenceId(paymentRequest.getReferenceId());
		if (transaction == null || !transaction.getStatus().equals(PaymentStatus.SUCCESS.toString())) {
			if (transaction == null) {
				transaction = new TransactionEntity();
				transaction.setEntityType(entityType);
				transaction.setEntityId(entityId);
				transaction.setCustomerId(getCustomerId());
				transaction.setAmount(Double.valueOf(paymentRequest.getOrderAmount()));
				transaction.setStatus(PaymentStatus.PENDING.toString());
			}
			handleTxnV2(paymentRequest, PaymentMode.QR, paymentGateway, transaction);
		}
	}

	@Override
	public Class<TransactionEntity> getEntity() {
		return TransactionEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return transactionRepository;
	}
}