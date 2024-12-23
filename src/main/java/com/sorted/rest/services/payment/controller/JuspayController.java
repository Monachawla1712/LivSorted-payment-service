package com.sorted.rest.services.payment.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.JuspayOrderEvent;
import com.sorted.rest.services.payment.constants.PaymentConstants.JuspayOrderStatus;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentMode;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentStatus;
import com.sorted.rest.services.payment.entity.CashCollectionEntity;
import com.sorted.rest.services.payment.entity.TransactionEntity;
import com.sorted.rest.services.payment.services.CashCollectionService;
import com.sorted.rest.services.payment.services.PaymentService;
import com.sorted.rest.services.payment.utils.ExceptionHandlerUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@Api(tags = "Juspay Services", description = "Manage Juspay related services.")
public class JuspayController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(JuspayController.class);

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private ExceptionHandlerUtils exceptionHandlerUtils;

	@Autowired
	private CashCollectionService cashCollectionService;

	@ApiOperation(value = "Initiate Juspay Payment", nickname = "initiateJuspayTxn")
	@PostMapping(value = "/payments/juspay/initiate")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<JuspaySessionResponse> initiateJuspayTxn(@Valid @RequestBody InitiateTxnRequestV2 request) {
		double amount = request.getAmount();
		validateMinRecharge(amount);
		String paymentGateway = ParamsUtils.getParam("CURRENT_ENABLED_PG");
		if (!PaymentMode.JUSPAY.toString().equals(paymentGateway)) {
			throw new ValidationException(
					new ErrorBean(Errors.INVALID_REQUEST, "Juspay transactions are not allowed. Please update your app", "paymentGateway"));
		}
		TransactionEntity txn = paymentService.initiateTransaction(amount, PaymentMode.JUSPAY, null, PaymentStatus.PENDING, request.getCcIds());
		UserDetailsResponse userDetails = clientService.getUserDetails(UUID.fromString(txn.getEntityId()));
		String customerId = null;
		if (userDetails != null) {
			customerId = userDetails.getPhoneNumber();
		}
		_LOGGER.info(String.format("Transaction is initiated for transactionId : %s and customerId: %s", txn.getId(), customerId));
		Object juspayResponse = clientService.initiateJuspayPayment(txn.getId(), txn.getAmount(), txn.getEntityType(), customerId);
		JuspaySessionResponse response = getMapper().convertValue(juspayResponse, JuspaySessionResponse.class);
		paymentService.verifyAndUpdateTransaction(buildPaymentNotifyBean(response, juspayResponse), txn, PaymentMode.JUSPAY, null);
		return ResponseEntity.ok(response);
	}

	private void validateMinRecharge(double amount) {
		Double minRechargeAmount = Double.valueOf(ParamsUtils.getParam("MINIMUM_PG_RECHARGE_AMOUNT", "500d"));
		if (Double.compare(amount, minRechargeAmount) < 0) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST,
					String.format("The minimum recharge amount required is ₹%s", minRechargeAmount.intValue()), "amount"));
		}
	}

	private PaymentNotifyBean buildPaymentNotifyBean(JuspaySessionResponse response, Object juspayResponse) {
		PaymentNotifyBean paymentNotifyBean = PaymentNotifyBean.newInstance();
		paymentNotifyBean.setId(response.getOrderId());
		paymentNotifyBean.setReferenceId(response.getId());
		paymentNotifyBean.setOrderAmount(response.getSdkPayload().getPayload().getAmount());
		paymentNotifyBean.setTxStatus(PaymentStatus.PENDING.toString());
		paymentNotifyBean.setPaymentGatewayResponse(Arrays.asList(juspayResponse));
		return paymentNotifyBean;
	}

	@ApiOperation(value = "verify Juspay Payment.", nickname = "verifyJuspayPayment")
	@PostMapping("/payments/juspay/verify")
	@ResponseStatus(code = HttpStatus.OK)
	public PaymentNotifyBean verifyJuspayPayment(@RequestParam String id) {
		_LOGGER.info(String.format("verifyJuspayPayment:: transaction id %s", id));
		TransactionEntity transaction = paymentService.findRecordById(id);
		if (transaction == null) {
			throw new ValidationException(new ErrorBean(Errors.NO_DATA_FOUND, "Transaction not found", "id"));
		} else if (!transaction.getStatus().equals(PaymentStatus.PENDING.toString()) && !transaction.getStatus().equals(PaymentStatus.IN_PROGRESS.toString())) {
			clientService.updateUserPreference(String.valueOf(transaction.getCustomerId()), PaymentConstants.UPI);
			return transaction.getMetadata().getPaymentNotification();
		}
		Object juspayResponse = clientService.fetchJuspayOrderStatus(id, transaction.getEntityType());
		JuspayOrderBean response = getMapper().convertValue(juspayResponse, JuspayOrderBean.class);
		PaymentNotifyBean paymentNotifyBean = buildPaymentNotifyBean(JuspayOrderStatus.fromString(response.getStatus()).getValue(), response, juspayResponse);
		String paymentGateway = response.getTxnDetail() != null ? response.getTxnDetail().getGateway() : null;
		try {
			paymentService.handleTxnV2(paymentNotifyBean, PaymentMode.JUSPAY, paymentGateway, transaction);
		} catch (Exception e) {
			if (!exceptionHandlerUtils.isHandledWsException(e)) {
				_LOGGER.error("Something went wrong.", e);
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Something went wrong. Please contact support.", null));
			}
		}
		if (paymentNotifyBean.getTxStatus().equals(PaymentConstants.PaymentStatus.SUCCESS.toString())) {
			clientService.updateUserPreference(String.valueOf(transaction.getCustomerId()), PaymentConstants.UPI);
		}
		checkIfRequestedCashCollection(paymentNotifyBean, transaction.getCustomerId());
		return paymentNotifyBean;
	}

	private void checkIfRequestedCashCollection(PaymentNotifyBean paymentNotifyBean, UUID customerId) {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		try {
			List<CashCollectionEntity> cashCollections = cashCollectionService.fetchExistingRequestedCcByUserIdAndDate(customerId.toString(),
					java.sql.Date.valueOf(tomorrow));
			if (CollectionUtils.isNotEmpty(cashCollections)) {
				CashCollectionEntity requestedCollection = cashCollections.get(0);
				paymentNotifyBean.setCcId(requestedCollection.getId());
			}
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while checking requested cc for id %s", customerId), e);
		}
	}

	private PaymentNotifyBean buildPaymentNotifyBean(PaymentStatus paymentStatus, JuspayOrderBean response, Object juspayResponse) {
		PaymentNotifyBean paymentNotifyBean = PaymentNotifyBean.newInstance();
		paymentNotifyBean.setId(response.getOrderId());
		paymentNotifyBean.setReferenceId(response.getId());
		if (response.getAmount() != null) {
			paymentNotifyBean.setOrderAmount(response.getAmount().toString());
		}
		paymentNotifyBean.setPaymentMode(response.getPaymentMethodType());
		paymentNotifyBean.setTxStatus(paymentStatus.toString());
		paymentNotifyBean.setTxMsg(response.getTxnDetail() != null && !StringUtils.isEmpty(response.getTxnDetail().getErrorMsg()) ?
				response.getTxnDetail().getErrorMsg() :
				getTxnMsg(paymentNotifyBean.getTxStatus()));
		paymentNotifyBean.setPaymentGatewayResponse(Arrays.asList(juspayResponse));
		return paymentNotifyBean;
	}

	private String getTxnMsg(String status) {
		if (status.equals(PaymentStatus.PENDING.toString())) {
			return "Your payment is pending. Please complete it.";
		} else if (status.equals(PaymentStatus.IN_PROGRESS.toString())) {
			return "Your payment is in progress. We will get back to you once we have an update.";
		} else if (status.equals(PaymentStatus.FAILED.toString())) {
			return "Your payment has failed. Please try again.";
		} else if (status.equals(PaymentStatus.SUCCESS.toString())) {
			return "Your payment is successful";
		}
		return null;
	}

	@ApiOperation(value = "create Juspay Customer.", nickname = "createJuspayCustomer")
	@PostMapping("/payments/juspay/customer")
	@ResponseStatus(code = HttpStatus.OK)
	public JuspayCustomerResponse createJuspayCustomer(@Valid @RequestBody JuspayCustomerRequest request) {
		_LOGGER.info(String.format("createJuspayCustomer:: request %s", request));
		Object juspayResponse = clientService.createJuspayCustomer(request, EntityType.USER);
		JuspayCustomerResponse response = getMapper().convertValue(juspayResponse, JuspayCustomerResponse.class);
		return response;
	}

	@ApiOperation(value = "notify Juspay Payment Signature.", nickname = "notifyJuspayPayment")
	@PostMapping("/payments/juspay/notify")
	@ResponseStatus(code = HttpStatus.OK)
	public void notifyJuspayPayment(@RequestBody Object req) {
		_LOGGER.info(String.format("notifyJuspayPayment:: request %s", req));
		JuspayOrderPaidRequest request = getMapper().convertValue(req, JuspayOrderPaidRequest.class);
		if (request != null && request.getContent() != null && request.getContent().getOrder() != null) {
			JuspayOrderEvent event = JuspayOrderEvent.fromString(request.getEventName());
			if (event != null) {
				if (request.getContent().getOrder().getOrderId() == null) {
					throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "No Payment Entity Found"));
				}
				PaymentNotifyBean paymentNotifyBean = buildPaymentNotifyBean(event.getValue(), request.getContent().getOrder(), req);
				String paymentGateway = request.getContent().getOrder().getTxnDetail() != null ?
						request.getContent().getOrder().getTxnDetail().getGateway() :
						null;
				try {
					paymentService.handleTxnV2(paymentNotifyBean, PaymentMode.JUSPAY, paymentGateway, null);
				} catch (Exception e) {
					if (!exceptionHandlerUtils.isHandledWsException(e)) {
						_LOGGER.error("Something went wrong.", e);
						throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Something went wrong. Please contact support.", null));
					}
				}
			}
		}
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}