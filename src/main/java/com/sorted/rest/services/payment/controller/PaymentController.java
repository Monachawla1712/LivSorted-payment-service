package com.sorted.rest.services.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.beans.EasebuzzInstacollectTransactionBean.EasebuzzInstacollectEvent;
import com.sorted.rest.services.payment.beans.EasebuzzInstacollectTransactionBean.EasebuzzInstacollectPaymentStatus;
import com.sorted.rest.services.payment.beans.EasebuzzInstacollectTransactionBean.EasebuzzInstacollectTransactionPayload;
import com.sorted.rest.services.payment.beans.RazorpayOrderPaidRequest.RazorpayOrderEntity;
import com.sorted.rest.services.payment.beans.RazorpayOrderPaidRequest.RazorpayPaymentEntity;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.*;
import com.sorted.rest.services.payment.entity.EasebuzzVirtualAccountEntity;
import com.sorted.rest.services.payment.entity.TransactionEntity;
import com.sorted.rest.services.payment.services.EasebuzzService;
import com.sorted.rest.services.payment.services.PaymentService;
import com.sorted.rest.services.payment.utils.ExceptionHandlerUtils;
import com.sorted.rest.services.payment.utils.HashUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.Valid;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mohit on 19.6.20.
 */
@RestController
@Api(tags = "Payment Services", description = "Manage Payment related services.")
public class PaymentController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(PaymentController.class);

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private ClientService clientService;

	@Autowired
	private HashUtils hashUtils;

	private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

	@Autowired
	private EasebuzzService easebuzzService;

	@Autowired
	private ExceptionHandlerUtils exceptionHandlerUtils;

	@ApiOperation(value = "Initiate Razorpay Payment in application store.", nickname = "initiateRazorpayTxn")
	@PostMapping("/payments/v2/initiate/razorpay")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<PaymentInitiateResponse> initiateRazorpayTxnV2(@Valid @RequestBody InitiateTxnRequestV2 request) {
		double amount = request.getAmount();
		String paymentGateway = ParamsUtils.getParam("CURRENT_ENABLED_PG");
		if (!PaymentGateway.RAZORPAY.toString().equals(paymentGateway)) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Razorpay transactions are not allowed", "paymentgateway"));
		}
		TransactionEntity entity = paymentService.initiateTransaction(amount, PaymentMode.PG, PaymentGateway.RAZORPAY.toString(), PaymentStatus.PENDING, null);
		String transactionId = entity.getId();
		_LOGGER.info(String.format("Transaction is initiated for transactionId : %s", transactionId));
		RazorpayCreateOrderRequest razorpayCreateOrderRequest = generateRazorpayTokenRequestV2(amount, transactionId);
		Order razorpayJsonResponse = clientService.createRazorpayOrder(razorpayCreateOrderRequest);
		RazorpayOrderEntity razorpayCreateOrderResponse = getMapper().mapJsonAsObject(razorpayJsonResponse.toString(), RazorpayOrderEntity.class);
		PaymentInitiateResponse response = new PaymentInitiateResponse();
		response.setTokenId(razorpayCreateOrderResponse.getId());
		return ResponseEntity.ok(response);
	}

	private RazorpayCreateOrderRequest generateRazorpayTokenRequestV2(double transactionAmount, String transactionId) {
		RazorpayCreateOrderRequest RazorpayCreateOrderRequest = new RazorpayCreateOrderRequest();
		Integer amount = BigDecimal.valueOf(transactionAmount).multiply(BigDecimal.valueOf(100)).intValue();
		RazorpayCreateOrderRequest.setAmount(amount);
		RazorpayCreateOrderRequest.setCurrency("INR");
		RazorpayCreateOrderRequest.setReceipt(transactionId);
		return RazorpayCreateOrderRequest;
	}

	@ApiOperation(value = "verify Razorpay Payment Signature.", nickname = "verifyRazorpayPaymentV2")
	@PostMapping("/payments/v2/confirm/razorpay")
	@ResponseStatus(code = HttpStatus.OK)
	public PaymentNotifyBean verifyRazorpayPaymentV2(@RequestBody RazorpayPaymentConfirmBean request) {
		_LOGGER.info(String.format("verifyRazorpayPaymentV2:: request %s", request));
		validateRazorpayPaymentSignature(request);
		_LOGGER.info("verifyRazorpayPaymentV2:: signature verified");
		PaymentNotifyBean paymentNotifyBean = fetchAndbuildPaymentNotifyBeanV2(request);
		_LOGGER.info(String.format("verifyRazorpayPaymentV2:: paymentNotifyBean %s", paymentNotifyBean));
		try {
			paymentService.handleTxnV2(paymentNotifyBean, PaymentMode.PG, null, null);
		} catch (Exception e) {
			if (!exceptionHandlerUtils.isHandledWsException(e)) {
				_LOGGER.error("Something went wrong.", e);
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Something went wrong. Please contact support.", null));
			}
		}
		return paymentNotifyBean;
	}

	private PaymentNotifyBean fetchAndbuildPaymentNotifyBeanV2(RazorpayPaymentConfirmBean request) {
		RazorpayPaymentEntity payment = getRazorpayPaymentEntity(request.getRazorpayPaymentId());
		RazorpayOrderEntity order = getRazorpayOrderEntity(request.getRazorpayOrderId());
		return buildPaymentNotifyBeanV2(order, payment);
	}

	private PaymentNotifyBean buildPaymentNotifyBeanV2(RazorpayOrderEntity order, RazorpayPaymentEntity payment) {
		PaymentNotifyBean paymentNotifyBean = PaymentNotifyBean.newInstance();
		paymentNotifyBean.setId(order.getReceipt());
		paymentNotifyBean.setPaymentMode(payment.getMethod());
		paymentNotifyBean.setReferenceId(payment.getId());
		paymentNotifyBean.setTxMsg(payment.getErrorDescription());
		if (payment.getCaptured()) {
			paymentNotifyBean.setTxStatus(PaymentStatus.SUCCESS.toString());
		} else {
			paymentNotifyBean.setTxStatus(PaymentStatus.FAILED.toString());
		}
		paymentNotifyBean.setPaymentGatewayResponse(Arrays.asList(order, payment));
		return paymentNotifyBean;
	}

	@ApiOperation(value = "notify Razorpay Payment Signature.", nickname = "notifyRazorpayPayment")
	@PostMapping("/payments/notify/v2/razorpay")
	@ResponseStatus(code = HttpStatus.OK)
	public void notifyRazorpayPaymentV2(@RequestBody RazorpayOrderPaidRequest request) {
		_LOGGER.info(String.format("notifyRazorpayPayment:: request %s", request));
		if (request != null && request.getPayload() != null) {
			if (request.getPayload().getPayment() == null || request.getPayload().getPayment().getEntity() == null) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "No Payment Entity Found"));
			}
			RazorpayPaymentEntity razorpayPayment = request.getPayload().getPayment().getEntity();
			RazorpayOrderEntity razorpayOrder = null;
			if (request.getPayload().getOrder() == null) {
				_LOGGER.info("notifyRazorpayPayment:: razorpayOrder not found in the request. fetching");
				razorpayOrder = getRazorpayOrderEntity(razorpayPayment.getOrderId());
				_LOGGER.info(String.format("notifyRazorpayPayment:: razorpayOrder %s", razorpayOrder));
			} else {
				razorpayOrder = request.getPayload().getOrder().getEntity();
			}
			PaymentNotifyBean paymentNotifyBean = buildPaymentNotifyBeanV2(razorpayOrder, razorpayPayment);
			_LOGGER.info(String.format("notifyRazorpayPayment:: paymentNotifyBean %s", paymentNotifyBean));
			try {
				paymentService.handleTxnV2(paymentNotifyBean, PaymentMode.PG, null, null);
			} catch (Exception e) {
				if (!exceptionHandlerUtils.isHandledWsException(e)) {
					_LOGGER.error("Something went wrong.", e);
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Something went wrong. Please contact support.", null));
				}
			}
		}
		_LOGGER.info("notifyRazorpayPayment:: Exiting");
	}

	/**
	 * Find all.
	 */
	@ApiOperation(value = "Initiate Payment in application store.", nickname = "initiateTxn")
	@PostMapping("/payments/initiate")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<PaymentInitiateResponse> initiateTxn(@Valid @RequestBody InitiateTxnRequest request) {
		OrderBean order = clientService.getOrderById(request.getOrderId());
		validatePaymentRequest(order);
		String paymentGateway = ParamsUtils.getParam("CURRENT_ENABLED_PG");
		if (!PaymentGateway.CASHFREE.toString().equals(paymentGateway)) {
			throw new ValidationException(
					new ErrorBean(Errors.INVALID_REQUEST, "Cashfree transactions are not allowed. Please update your app", "paymentgateway"));
		}
		CashfreeTokenRequest cashfreeTokenRequest = generateCashfreeTokenRequest(order);
		CashfreeTokenResponse cashfreeTokenResponse = clientService.getCfToken(cashfreeTokenRequest);
		PaymentInitiateResponse response = new PaymentInitiateResponse();
		response.setTokenId(cashfreeTokenResponse.getCftoken());
		response.setNotifyUrl(ParamsUtils.getParam("PAYMENT_NOTIFY_URL"));
		return ResponseEntity.ok(response);
	}

	private CashfreeTokenRequest generateCashfreeTokenRequest(OrderBean order) {
		CashfreeTokenRequest cashfreeTokenRequest = new CashfreeTokenRequest();
		cashfreeTokenRequest.setOrderAmount(order.getFinalBillAmount());
		cashfreeTokenRequest.setOrderCurrency("INR");
		cashfreeTokenRequest.setOrderId(order.getId().toString());
		return cashfreeTokenRequest;
	}

	/**
	 * Find all.
	 */
	@ApiOperation(value = "Initiate Razorpay Payment in application store.", nickname = "initiateRazorpayTxn")
	@PostMapping("/payments/initiate/razorpay")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<PaymentInitiateResponse> initiateRazorpayTxn(@Valid @RequestBody InitiateTxnRequest request) {
		OrderBean order = clientService.getOrderById(request.getOrderId());
		validatePaymentRequest(order);
		String paymentGateway = ParamsUtils.getParam("CURRENT_ENABLED_PG");
		if (!PaymentGateway.RAZORPAY.toString().equals(paymentGateway)) {
			throw new ValidationException(
					new ErrorBean(Errors.INVALID_REQUEST, "Razorpay transactions are not allowed. Please update your app", "paymentgateway"));
		}
		RazorpayCreateOrderRequest razorpayCreateOrderRequest = generateRazorpayTokenRequest(order);
		Order razorpayJsonResponse = clientService.createRazorpayOrder(razorpayCreateOrderRequest);
		RazorpayOrderEntity razorpayCreateOrderResponse = getMapper().mapJsonAsObject(razorpayJsonResponse.toString(), RazorpayOrderEntity.class);
		PaymentInitiateResponse response = new PaymentInitiateResponse();
		response.setTokenId(razorpayCreateOrderResponse.getId());
		return ResponseEntity.ok(response);
	}

	private RazorpayCreateOrderRequest generateRazorpayTokenRequest(OrderBean order) {
		RazorpayCreateOrderRequest RazorpayCreateOrderRequest = new RazorpayCreateOrderRequest();
		Integer amount = BigDecimal.valueOf(order.getFinalBillAmount()).multiply(BigDecimal.valueOf(100)).intValue();
		RazorpayCreateOrderRequest.setAmount(amount);
		RazorpayCreateOrderRequest.setCurrency("INR");
		RazorpayCreateOrderRequest.setReceipt(order.getId().toString());
		return RazorpayCreateOrderRequest;
	}

	@PostMapping(path = "/payments/notify", consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE })
	@ResponseStatus(code = HttpStatus.OK)
	public void paymentNotify(@RequestBody MultiValueMap<String, String> paramMap) throws JsonProcessingException {
		Map<String, String> map = paramMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
		final PaymentNotifyBean request = getMapper().mapSrcToDest(map, PaymentNotifyBean.newInstance());
		_LOGGER.info("PaymentController:PaymentNotify" + request);
		validatePaymentNotifySignature(request);
		processPaymentNotify(request, PaymentGateway.CASHFREE);
	}

	@PostMapping("/payments/confirm")
	@ResponseStatus(code = HttpStatus.OK)
	public PaymentNotifyBean paymentConfirm(@RequestBody PaymentNotifyBean request) throws JsonProcessingException {
		validatePaymentNotifySignature(request);
		processPaymentNotify(request, PaymentGateway.CASHFREE);
		return request;
	}

	private void processPaymentNotify(PaymentNotifyBean request, PaymentGateway paymentGateway) {
		if (request.getOrderId() != null) {
			OrderBean order = clientService.getOrderById(UUID.fromString(request.getOrderId()));
			_LOGGER.info(String.format("processPaymentNotify:: order %s", order));
			TransactionEntity transaction = paymentService.handleTxn(order, paymentGateway, request, PaymentConstants.EntityType.USER);
			updateOrder(order, transaction);
		}
	}

	private void updateOrder(OrderBean order, TransactionEntity transaction) {
		if (order.getPaymentDetail() == null) {
			PaymentDetail paymentDetail = new PaymentDetail();
			order.setPaymentDetail(paymentDetail);
		}
		if (transaction.getStatus().equals(PaymentNotifyStatus.SUCCESS.toString())) {
			order.setAmountReceived(Double.valueOf(transaction.getAmount()));
			order.getPaymentDetail().setPaymentStatus(PaymentStatus.SUCCESS);
		} else if (transaction.getStatus().equals(PaymentNotifyStatus.PENDING.toString())) {
			order.getPaymentDetail().setPaymentStatus(PaymentStatus.IN_PROGRESS);
		} else if (transaction.getStatus().equals(PaymentNotifyStatus.FAILED.toString()) || transaction.getStatus()
				.equals(PaymentNotifyStatus.FLAGGED.toString())) {
			order.getPaymentDetail().setPaymentStatus(PaymentStatus.FAILED);
		}
		if (order.getPaymentDetail().getTransactions() == null) {
			Set<String> transactions = new HashSet<>();
			order.getPaymentDetail().setTransactions(transactions);
		}
		order.getPaymentDetail().getTransactions().add(transaction.getId());
		order.getPaymentDetail().setPaymentGateway(transaction.getPaymentGateway());
		updateOrderClient(order);
	}

	private void updateOrderClient(OrderBean order) {
		clientService.sendOrderPaymentUpdate(order);

	}

	private void validatePaymentRequest(OrderBean order) {
		if (order == null) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Order Not Found", "order"));
		} else if (order.getAmountReceived() == null || Double.compare(order.getAmountReceived(), 0) > 0) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Payment already done for the order", "amountReceived"));
		} else if (!order.getStatus().equals(OrderStatus.ORDER_BILLED) && !order.getStatus().equals(OrderStatus.READY_FOR_PICKUP) && !order.getStatus()
				.equals(OrderStatus.ORDER_OUT_FOR_DELIVERY) && !order.getStatus().equals(OrderStatus.ORDER_DELIVERED)) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Order Status not valid for initiating Payment", "status"));
		}
	}

	private void validatePaymentNotifySignature(PaymentNotifyBean paymentNotifyRequest) {
		LinkedHashMap<String, String> postData = new LinkedHashMap<String, String>();

		postData.put("orderId", paymentNotifyRequest.getOrderId());
		postData.put("orderAmount", paymentNotifyRequest.getOrderAmount());
		postData.put("referenceId", paymentNotifyRequest.getReferenceId());
		postData.put("txStatus", paymentNotifyRequest.getTxStatus());
		postData.put("paymentMode", paymentNotifyRequest.getPaymentMode());
		postData.put("txMsg", paymentNotifyRequest.getTxMsg());
		postData.put("txTime", paymentNotifyRequest.getTxTime());

		String data = "";
		Set<String> keys = postData.keySet();

		for (String key : keys) {
			data = data + postData.get(key);
		}
		String secretKey = clientService.getCashfreeSecret();

		byte[] rawHmac = calculateRFC2104HMAC(data, secretKey);
		// base64-encode the hmac
		String signature = Base64.getEncoder().encodeToString(rawHmac);

		if (paymentNotifyRequest.getSignature() == null || !paymentNotifyRequest.getSignature().equals(signature)) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Error while validating Payment Signature"));
		}
	}

	public byte[] calculateRFC2104HMAC(String data, String secret) {
		byte[] rawHmac;
		try {
			SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA256_ALGORITHM);

			Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
			mac.init(signingKey);

			// compute the hmac on input data bytes
			rawHmac = mac.doFinal(data.getBytes());

		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			_LOGGER.error("Error while validating Payment Signature", e);
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Error while validating Payment Signature"));
		}
		return rawHmac;
	}

	@GetMapping("/payments/customer")
	@ResponseStatus(code = HttpStatus.OK)
	public PageAndSortResult<TransactionResponseBean> getCustomerTransactions(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "20") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = null;
		sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);

		final UUID customerId = paymentService.getCustomerId();
		final Map<String, Object> params = new HashMap<>();
		params.put("customerId", customerId);

		PageAndSortResult<TransactionEntity> poEntityList = paymentService.findPurchaseOrdersByPage(pageSize, pageNo, params, sort);
		PageAndSortResult<TransactionResponseBean> response = new PageAndSortResult<TransactionResponseBean>();
		if (poEntityList != null && poEntityList.getData() != null) {
			response = prepareResponsePageData(poEntityList, TransactionResponseBean.class);
		}
		return response;
	}

	@ApiOperation(value = "notify Razorpay Payment Signature.", nickname = "notifyRazorpayPayment")
	@PostMapping("/payments/notify/razorpay")
	@ResponseStatus(code = HttpStatus.OK)
	public void notifyRazorpayPayment(@RequestBody RazorpayOrderPaidRequest request) {
		_LOGGER.info(String.format("notifyRazorpayPayment:: request %s", request));
		if (request != null && request.getPayload() != null) {
			if (request.getPayload().getPayment() == null || request.getPayload().getPayment().getEntity() == null) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "No Payment Entity Found"));
			}
			RazorpayPaymentEntity razorpayPayment = request.getPayload().getPayment().getEntity();
			RazorpayOrderEntity razorpayOrder = null;
			if (request.getPayload().getOrder() == null) {
				_LOGGER.info("notifyRazorpayPayment:: razorpayOrder not found in the request. fetching");
				razorpayOrder = getRazorpayOrderEntity(razorpayPayment.getOrderId());
				_LOGGER.info(String.format("notifyRazorpayPayment:: razorpayOrder %s", razorpayOrder));
			} else {
				razorpayOrder = request.getPayload().getOrder().getEntity();
			}
			PaymentNotifyBean paymentNotifyBean = buildPaymentNotifyBean(razorpayOrder, razorpayPayment);
			_LOGGER.info(String.format("notifyRazorpayPayment:: paymentNotifyBean %s", paymentNotifyBean));
			processPaymentNotify(paymentNotifyBean, PaymentGateway.RAZORPAY);
		}
		_LOGGER.info("notifyRazorpayPayment:: Exiting");
	}

	@ApiOperation(value = "verify Razorpay Payment Signature.", nickname = "verifyRazorpayPayment")
	@PostMapping("/payments/confirm/razorpay")
	@ResponseStatus(code = HttpStatus.OK)
	public PaymentNotifyBean verifyRazorpayPayment(@RequestBody RazorpayPaymentConfirmBean request) {
		_LOGGER.info(String.format("verifyRazorpayPayment:: request %s", request));
		validateRazorpayPaymentSignature(request);
		_LOGGER.info("verifyRazorpayPayment:: signature verified");
		PaymentNotifyBean paymentNotifyBean = fetchAndbuildPaymentNotifyBean(request);
		_LOGGER.info(String.format("verifyRazorpayPayment:: paymentNotifyBean %s", paymentNotifyBean));
		processPaymentNotify(paymentNotifyBean, PaymentGateway.RAZORPAY);
		return paymentNotifyBean;
	}

	private PaymentNotifyBean fetchAndbuildPaymentNotifyBean(RazorpayPaymentConfirmBean request) {
		RazorpayPaymentEntity payment = getRazorpayPaymentEntity(request.getRazorpayPaymentId());
		RazorpayOrderEntity order = getRazorpayOrderEntity(request.getRazorpayOrderId());
		return buildPaymentNotifyBean(order, payment);
	}

	private RazorpayPaymentEntity getRazorpayPaymentEntity(String razorpayPaymentId) {
		Payment razorpayPaymentResponse = clientService.fetchRazorpayPaymentDetails(razorpayPaymentId);
		RazorpayPaymentEntity payment = getMapper().mapJsonAsObject(razorpayPaymentResponse.toString(), RazorpayPaymentEntity.class);
		return payment;
	}

	private RazorpayOrderEntity getRazorpayOrderEntity(String razorpayOrderId) {
		Order razorpayOrderResponse = clientService.fetchRazorpayOrderDetails(razorpayOrderId);
		RazorpayOrderEntity order = getMapper().mapJsonAsObject(razorpayOrderResponse.toString(), RazorpayOrderEntity.class);
		return order;
	}

	private PaymentNotifyBean buildPaymentNotifyBean(RazorpayOrderEntity order, RazorpayPaymentEntity payment) {
		PaymentNotifyBean paymentNotifyBean = PaymentNotifyBean.newInstance();
		paymentNotifyBean.setOrderId(order.getReceipt());
		paymentNotifyBean.setOrderAmount(Double.valueOf(order.getAmount() / 100).toString());
		paymentNotifyBean.setPaymentMode(payment.getMethod());
		paymentNotifyBean.setReferenceId(payment.getId());
		paymentNotifyBean.setTxMsg(payment.getErrorDescription());
		if (payment.getCaptured()) {
			paymentNotifyBean.setTxStatus(PaymentStatus.SUCCESS.toString());
		} else {
			paymentNotifyBean.setTxStatus(PaymentStatus.FAILED.toString());
		}
		paymentNotifyBean.setPaymentGatewayResponse(Arrays.asList(order, payment));
		return paymentNotifyBean;
	}

	private void validateRazorpayPaymentSignature(RazorpayPaymentConfirmBean request) {
		StringBuilder data = new StringBuilder();
		data.append(request.getRazorpayOrderId()).append("|").append(request.getRazorpayPaymentId());
		String secretKey = clientService.getRazorpaySecret();

		byte[] rawHmac = calculateRFC2104HMAC(data.toString(), secretKey);
		// base64-encode the hmac
		String signature = DatatypeConverter.printHexBinary(rawHmac).toLowerCase();

		if (request.getRazorpaySignature() == null || !request.getRazorpaySignature().equals(signature)) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Error while validating Payment Signature"));
		}
	}

	@ApiOperation(value = "notify Lithos Payment.", nickname = "notifyLithosPayment")
	@PostMapping("/payments/confirm/lithos")
	@ResponseStatus(code = HttpStatus.OK)
	public PaymentNotifyBean notifyLithosPayment(@RequestBody PaymentNotifyBean request) throws JsonProcessingException {
		if (request.getReferenceId() == null && request.getOrderId() != null) {
			request.setReferenceId(request.getOrderId());
		}
		processPaymentNotify(request, PaymentGateway.LITHOS);
		return request;
	}

	@ApiOperation(value = "Initiate Easebuzz Payment", nickname = "initiateEasebuzzTxn")
	@PostMapping(value = "/payments/initiate/easebuzz")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<PaymentInitiateResponse> initiateEasebuzzTxn(@Valid @RequestBody InitiateTxnRequestV2 request) {
		double amount = request.getAmount();
		String paymentGateway = ParamsUtils.getParam("CURRENT_ENABLED_PG");
		if (!PaymentGateway.EASEBUZZ.toString().equals(paymentGateway)) {
			throw new ValidationException(
					new ErrorBean(Errors.INVALID_REQUEST, "Easebuzz transactions are not allowed. Please update your app", "paymentgateway"));
		}
		TransactionEntity entity = paymentService.initiateTransaction(amount, PaymentMode.PG, paymentGateway, PaymentStatus.PENDING, null);
		String transactionId = entity.getId();
		_LOGGER.info(String.format("Transaction is initiated for transactionId : %s", transactionId));
		EasebuzzTokenRequest easebuzzTokenRequest = generateEasebuzzTokenRequest(amount, transactionId);
		Map<String, Object> params = getMapper().convertValue(easebuzzTokenRequest, Map.class);
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>(
				params.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue().toString()))));
		Object easebuzzResponse = clientService.initiateEasebuzzPayment(multiValueMap);
		EasebuzzTokenResponse easebuzzTokenResponse = getMapper().convertValue(easebuzzResponse, EasebuzzTokenResponse.class);
		if (easebuzzTokenResponse == null || easebuzzTokenResponse.getStatus() != 1) {
			_LOGGER.error("Error while initiating Easebuzz payment");
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
		PaymentInitiateResponse response = new PaymentInitiateResponse();
		response.setTokenId(easebuzzTokenResponse.getData());
		return ResponseEntity.ok(response);
	}

	private EasebuzzTokenRequest generateEasebuzzTokenRequest(Double transactionAmount, String transactionId) {
		EasebuzzTokenRequest easebuzzTokenRequest = new EasebuzzTokenRequest();
		easebuzzTokenRequest.setAmount(transactionAmount);
		easebuzzTokenRequest.setTxnid(transactionId);
		easebuzzTokenRequest.setKey(clientService.getEasebuzzKey());
		easebuzzTokenRequest.setHash(getEasebuzzInitiatePaymentHash(easebuzzTokenRequest));
		String callbackUrl = clientService.getEasebuzzPgCallbackUrl();
		easebuzzTokenRequest.setSurl(callbackUrl);
		easebuzzTokenRequest.setFurl(callbackUrl);
		return easebuzzTokenRequest;
	}

	private String getEasebuzzInitiatePaymentHash(EasebuzzTokenRequest request) {
		try {
			String salt = clientService.getEasebuzzSalt();
			String udf = "|||||||||"; // 10/10 user-defined-fields are null (blank)
			String hash = String.join("|", request.getKey(), request.getTxnid(), request.getAmount().toString(), request.getProductinfo(),
					request.getFirstname(), request.getEmail(), udf, salt);
			return hashUtils.getSha512encryptedString(hash);
		} catch (Exception e) {
			_LOGGER.error("Error while encrypting Easebuzz hash", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
	}

	//	todo: API not in use now, to be used when Easebuzz web-integration is done
	//	@ApiOperation(value = "notify Easebuzz Payment Signature.", nickname = "notifyEasebuzzPayment")
	//	@PostMapping("/payments/notify/easebuzz")
	//	@ResponseStatus(code = HttpStatus.OK)
	//	public void notifyEasebuzzPayment(@RequestBody Object req) {
	//		_LOGGER.info(String.format("notifyEasebuzzPayment:: request %s", req));
	//		EasebuzzTransactionBean request = getMapper().convertValue(req, EasebuzzTransactionBean.class);
	//		if (request != null && request.getPayload() != null) {
	//			if (request.getPayload().getTxnId() == null) {
	//				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "No Payment Entity Found"));
	//			}
	//			PaymentStatus status = EasebuzzPaymentStatus.fromString(request.getPayload().getStatus()).getValue();
	//			validateEasebuzzTransaction(status, request.getPayload(), req);
	//			PaymentNotifyBean paymentNotifyBean = buildPaymentNotifyBean(status, request.getPayload(), req);
	//			_LOGGER.info(String.format("notifyEasebuzzPayment:: paymentNotifyBean %s", paymentNotifyBean));
	//			try {
	//				paymentService.handleTxnV2(paymentNotifyBean, PaymentMode.PG, null, null);
	//			} catch (Exception e) {
	//				if (!exceptionHandlerUtils.isHandledWsException(e)) {
	//					throw e;
	//				}
	//			}
	//		}
	//		_LOGGER.info("notifyEasebuzzPayment:: Exiting");
	//	}

	@ApiOperation(value = "verify Easebuzz Payment Signature.", nickname = "verifyEasebuzzPaymentV2")
	@PostMapping("/payments/confirm/easebuzz")
	@ResponseStatus(code = HttpStatus.OK)
	public PaymentNotifyBean verifyEasebuzzPayment(@RequestBody Object req) {
		_LOGGER.info(String.format("verifyEasebuzzPayment:: request %s", req));
		EasebuzzTransactionResponse request = getMapper().convertValue(req, EasebuzzTransactionResponse.class);
		PaymentNotifyBean paymentNotifyBean = null;
		if (request != null && request.getPayload() != null) {
			if (request.getPayload().getTxnId() == null) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "No Payment Entity Found"));
			}
			PaymentStatus status = EasebuzzPaymentStatus.fromString(request.getPayload().getStatus()).getValue();
			validateEasebuzzTransaction(status, request.getPayload(), req);
			paymentNotifyBean = buildPaymentNotifyBean(status, request.getPayload(), req);
			_LOGGER.info(String.format("verifyEasebuzzPayment:: paymentNotifyBean %s", paymentNotifyBean));
			try {
				paymentService.handleTxnV2(paymentNotifyBean, PaymentMode.PG, null, null);
			} catch (Exception e) {
				if (!exceptionHandlerUtils.isHandledWsException(e)) {
					_LOGGER.error("Something went wrong.", e);
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Something went wrong. Please contact support.", null));
				}
			}
		}
		_LOGGER.info("verifyEasebuzzPayment:: Exiting");
		return paymentNotifyBean;
	}

	private void validateEasebuzzTransaction(PaymentStatus status, EasebuzzTransactionPayload payload, Object req) {
		if (!status.equals(PaymentStatus.FAILED)) {
			String key = clientService.getEasebuzzKey();
			if (!key.equals(payload.getKey())) {
				_LOGGER.error("Error while validating Easebuzz transaction key from callback, request from easebuzz was " + req);
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Easebuzz Transaction Key Mismatch"));
			}
			if (!payload.getHash().equals(getEasebuzzTransactionHash(payload))) {
				_LOGGER.error("Error while validating Easebuzz transaction hash from callback, request from easebuzz was " + req);
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Easebuzz Transaction Hash Mismatch"));
			}
		}
	}

	private String getEasebuzzTransactionHash(EasebuzzTransactionPayload request) {
		try {
			String salt = clientService.getEasebuzzSalt();
			String udf = "|||||||||"; // 10/10 user-defined-fields are null (blank)
			String hash = String.join("|", salt, request.getStatus(), udf, request.getEmail(), request.getFirstname(), request.getProductInfo(),
					request.getAmount(), request.getTxnId(), request.getKey());
			return hashUtils.getSha512encryptedString(hash);
		} catch (Exception e) {
			_LOGGER.error("Error while encrypting Easebuzz hash", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
	}

	private PaymentNotifyBean buildPaymentNotifyBean(PaymentStatus status, EasebuzzTransactionPayload payment, Object request) {
		PaymentNotifyBean paymentNotifyBean = PaymentNotifyBean.newInstance();
		paymentNotifyBean.setId(payment.getTxnId());
		paymentNotifyBean.setPaymentMode(payment.getMode());
		paymentNotifyBean.setReferenceId(payment.getEasepayId());
		paymentNotifyBean.setOrderAmount(payment.getAmount());
		paymentNotifyBean.setTxStatus(status.toString());
		paymentNotifyBean.setPaymentGatewayResponse(Arrays.asList(request));
		paymentNotifyBean.setTxMsg(getTxnMsg(paymentNotifyBean.getTxStatus()));
		return paymentNotifyBean;
	}

	private String getTxnMsg(String status) {
		if (status.equals(PaymentStatus.IN_PROGRESS.toString())) {
			return "Your payment is in progress. We will get back to you once we have an update.";
		} else if (status.equals(PaymentStatus.FAILED.toString())) {
			return "Your payment has failed. Please try again.";
		}
		return null;
	}

	@ApiOperation(value = "reflect Easebuzz Instacollect Payments", nickname = "creditEasebuzzInstacollectPayment")
	@PostMapping("/payments/notify/credit/easebuzz")
	@ResponseStatus(code = HttpStatus.OK)
	public void creditEasebuzzInstacollectPayment(@RequestBody Object req) {
		_LOGGER.info(String.format("notifyEasebuzzInstacollectPayment:: request %s", req));
		EasebuzzInstacollectTransactionBean request = getMapper().convertValue(req, EasebuzzInstacollectTransactionBean.class);
		if (request != null && request.getEvent().equals(EasebuzzInstacollectEvent.TRANSACTION_CREDIT.toString()) && request.getData() != null) {
			if (request.getData().getId() == null || request.getData().getAmount() == null || Double.valueOf(request.getData().getAmount())
					.compareTo(0d) != 1 || request.getData().getVirtualAccount() == null || request.getData().getVirtualAccount().getId() == null) {
				_LOGGER.error("Invalid credit transaction");
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Invalid credit transaction"));
			}
			Optional<EasebuzzVirtualAccountEntity> virtualAccountEntity = easebuzzService.findByVirtualAccountId(request.getData().getVirtualAccount().getId());
			if (!virtualAccountEntity.isPresent()) {
				_LOGGER.error("No Virtual Account Found");
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "No Virtual Account Found"));
			}
			if (!request.getData().getAuthorization()
					.equals(getEasebuzzInstacollectTransactionHash(request.getData(), virtualAccountEntity.get().getEntityType()))) {
				_LOGGER.error("Error while validating Easebuzz instacollect transaction hash");
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Easebuzz Transaction Hash Mismatch"));
			}
			PaymentNotifyBean paymentNotifyBean = buildPaymentNotifyBean(request, req);
			_LOGGER.info(String.format("creditEasebuzzInstacollectPayment:: paymentNotifyBean %s", paymentNotifyBean));
			try {
				paymentService.handleQrTxn(paymentNotifyBean, PaymentGateway.EASEBUZZ.toString(), virtualAccountEntity.get().getEntityType(),
						virtualAccountEntity.get().getEntityId());
			} catch (Exception e) {
				if (!exceptionHandlerUtils.isHandledWsException(e)) {
					_LOGGER.error("Something went wrong.", e);
					throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Something went wrong. Please contact support.", null));
				}
			}
		}
		_LOGGER.info("creditEasebuzzInstacollectPayment:: Exiting");
	}

	private String getEasebuzzInstacollectTransactionHash(EasebuzzInstacollectTransactionPayload request, EntityType entityType) {
		try {
			String key = entityType.equals(EntityType.STORE) ? clientService.getEasebuzzWireKey() : clientService.getConsumerEasebuzzWireKey();
			String salt = entityType.equals(EntityType.STORE) ? clientService.getEasebuzzWireSalt() : clientService.getConsumerEasebuzzWireSalt();
			String hash = String.join("|", key, request.getUniqueTransactionReference(), request.getAmount(), request.getStatus(), salt);
			return hashUtils.getSha512encryptedString(hash);
		} catch (Exception e) {
			_LOGGER.error("Error while encrypting Easebuzz hash", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while encrypting Easebuzz hash"));
		}
	}

	private PaymentNotifyBean buildPaymentNotifyBean(EasebuzzInstacollectTransactionBean request, Object req) {
		EasebuzzInstacollectTransactionPayload payment = request.getData();
		PaymentNotifyBean paymentNotifyBean = PaymentNotifyBean.newInstance();
		paymentNotifyBean.setPaymentMode(payment.getPaymentMode());
		paymentNotifyBean.setReferenceId(payment.getId());
		paymentNotifyBean.setOrderAmount(payment.getAmount());
		paymentNotifyBean.setTxStatus(EasebuzzInstacollectPaymentStatus.fromString(payment.getStatus()).getValue().toString());
		paymentNotifyBean.setPaymentGatewayResponse(Arrays.asList(req));
		return paymentNotifyBean;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}