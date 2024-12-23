package com.sorted.rest.services.payment.clients;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.entity.UserWalletEntity;
import com.sorted.rest.services.payment.utils.HashUtils;
import lombok.Getter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author mohitaggarwal
 */
@Service
public class ClientService {

	AppLogger _LOGGER = LoggingManager.getLogger(ClientService.class);

	@Autowired
	private CashfreeClient cashfreeClient;

	@Autowired
	private OrderClient orderClient;

	@Autowired
	private AuthClient authClient;

	@Autowired
	private NotificationClient notificationClient;

	@Autowired
	private StoreClient storeClient;

	@Autowired
	private WmsClient wmsClient;

	@Autowired
	private OfferClient offerClient;

	@Autowired
	private EasebuzzClient easebuzzClient;

	@Autowired
	private EasebuzzWireClient easebuzzWireClient;

	@Autowired
	private JuspayClient juspayClient;

	@Autowired
	private HashUtils hashUtils;

	@Value("${client.cashfree.app-id}")
	@Getter
	private String cashfreeAppId;

	@Value("${client.cashfree.secret}")
	@Getter
	private String cashfreeSecret;

	private RazorpayClient razorpay;

	@Value("${client.razorpay.app-id}")
	@Getter
	private String razorpayAppId;

	@Value("${client.razorpay.secret}")
	@Getter
	private String razorpaySecret;

	@Value("${client.wms.auth_key}")
	@Getter
	private String RZ_AUTH_VALUE;

	@Value("${client.easebuzz.key}")
	@Getter
	private String easebuzzKey;

	@Value("${client.easebuzz.salt}")
	@Getter
	private String easebuzzSalt;

	@Value("${client.easebuzz.pg-callback-url}")
	@Getter
	private String easebuzzPgCallbackUrl;

	@Value("${client.easebuzz.wire-key}")
	@Getter
	private String easebuzzWireKey;

	@Value("${client.easebuzz.wire-salt}")
	@Getter
	private String easebuzzWireSalt;

	@Value("${client.juspay.version}")
	@Getter
	private String juspayVersion;

	@Value("${client.juspay.merchant-id}")
	@Getter
	private String juspayMerchantId;

	@Value("${client.juspay.authorization}")
	@Getter
	private String juspayAuthorization;

	@Value("${client.juspay.return-url}")
	@Getter
	private String juspayReturnUrl;

	@Value("${client.juspay.consumer.version}")
	@Getter
	private String consumerJuspayVersion;

	@Value("${client.juspay.consumer.merchant-id}")
	@Getter
	private String consumerJuspayMerchantId;

	@Value("${client.juspay.consumer.authorization}")
	@Getter
	private String consumerJuspayAuthorization;

	@Value("${client.juspay.consumer.return-url}")
	@Getter
	private String consumerJuspayReturnUrl;

	@Value("${client.easebuzz.consumer.wire-key}")
	@Getter
	private String consumerEasebuzzWireKey;

	@Value("${client.easebuzz.consumer.wire-salt}")
	@Getter
	private String consumerEasebuzzWireSalt;

	public CashfreeTokenResponse getCfToken(CashfreeTokenRequest request) {
		CashfreeTokenResponse response = null;

		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("x-client-id", cashfreeAppId);
		headerMap.put("x-client-secret", cashfreeSecret);
		try {
			response = cashfreeClient.getToken(headerMap, request);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Cashfree Token", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
		return response;
	}

	public Order createRazorpayOrder(RazorpayCreateOrderRequest request) {
		initRazorpayClient();
		Order response = null;
		try {
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", request.getAmount());
			orderRequest.put("currency", request.getCurrency());
			orderRequest.put("receipt", request.getReceipt());
			response = razorpay.orders.create(orderRequest);
			_LOGGER.info(response);

		} catch (Exception e) {
			_LOGGER.error("Error while fetching Cashfree Token", e);
			throw new ServerException(
					new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to initiate your payment. Kindly try again."));
		}
		return response;
	}

	public Payment fetchRazorpayPaymentDetails(String paymentId) {
		initRazorpayClient();
		Payment response = null;
		try {
			response = razorpay.payments.fetch(paymentId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Cashfree Token", e);
			throw new ServerException(
					new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to fetch your payment details. Kindly try again."));
		}
		return response;
	}

	public Order fetchRazorpayOrderDetails(String orderId) {
		initRazorpayClient();
		Order response = null;
		try {
			response = razorpay.orders.fetch(orderId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Cashfree Token", e);
			throw new ServerException(
					new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to fetch your payment details. Kindly try again."));
		}
		return response;
	}

	public OrderBean getOrderById(UUID orderId) {
		OrderBean response = null;
		try {
			response = orderClient.getOrderById(orderId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Order", e);
			throw new ServerException(
					new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are unable to fetch your order details. Kindly try again."));
		}
		return response;
	}

	public void sendOrderPaymentUpdate(OrderBean order) {
		try {
			orderClient.sendOrderPaymentUpdate(order);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Order", e);
			throw new ServerException(
					new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are unable to update your order details. Kindly try again."));
		}
	}

	private void initRazorpayClient() {
		if (razorpay == null) {
			try {
				razorpay = new RazorpayClient(razorpayAppId, razorpaySecret);
			} catch (RazorpayException e) {
				_LOGGER.error("Error while generating RazorpayClient", e);
				throw new ServerException(
						new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to initiate your payment. Kindly try again."));
			}
		}
	}

	public UserDetailsResponse getUserDetails(UUID id) {
		if (id == null)
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User details not found", ""));
		try {
			return authClient.getUserDetails(id.toString());
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for id ", id.toString()), e);
			return null;
		}
	}

	public void updateUserPreference(String id, String paymentMode) {
		if (id == null)
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User details not found", ""));
		try {
			authClient.updateUserPreference(paymentMode, id);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while updating user payment mode for user :: %s", id), e);
		}
	}

	public List<UserDetailsResponse> getUserDetailsByIds(List<String> userIds) {
		if (CollectionUtils.isEmpty(userIds))
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User details not found", ""));
		try {
			UserIdsRequest userIdsRequest = new UserIdsRequest(userIds);
			return authClient.getUserDetailsByIds(userIdsRequest);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for id ", userIds, e));
			return null;
		}
	}

	public List<WmsStoreDataResponse> getStoreDetails(Set<String> storeIds, String storeType) {
		List<WmsStoreDataResponse> response = new ArrayList<>();
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("rz-auth-key", RZ_AUTH_VALUE);
		Set<Integer> filteredStoreIds = storeIds.stream().filter(e -> e.matches("\\d+")).map(e -> Integer.parseInt(e)).collect(Collectors.toSet());
		if (!filteredStoreIds.isEmpty()) {
			try {
				response = wmsClient.getStoreDetails(headerMap, filteredStoreIds, storeType);
			} catch (Exception e) {
				_LOGGER.error("Error while getting storeDetails", e);
				throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while getting store details. Kindly try again."));
			}
		}
		return response;
	}

	public StoreDataResponse getStoreDataFromId(String storeId) {
		StoreDataResponse response = null;
		try {
			response = storeClient.getStoreDataFromIds(storeId).get(0);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreFromId", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are unable to fetch store details. Kindly try again."));
		}
		return response;
	}

	public List<OrderDetailResponse> getOrderDetails(Set<String> displayOrderIds) {
		try {
			return orderClient.getOrderDetailsByDisplayOrderIds(displayOrderIds);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Order", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are unable to fetch order details. Kindly try again."));
		}
	}

	public List<StoreOrderCount> getStoreOrderCount(List<String> storeIds) {
		if (CollectionUtils.isEmpty(storeIds)) {
			return new ArrayList<>();
		} else {
			try {
				StoreOrderCountRequest request = new StoreOrderCountRequest(storeIds);
				return orderClient.getFranchiseOrdersForStores(request);
			} catch (Exception e) {
				_LOGGER.error("Error while fetching store order count", e);
				throw new ServerException(
						new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are unable to fetch order details. Kindly try again."));
			}
		}
	}

	public Object initiateEasebuzzPayment(MultiValueMap<String, String> body) {
		try {
			_LOGGER.info(String.format("Easebuzz initiate transaction request body : %s", body));
			Object response = easebuzzClient.initiatePayment(body);
			_LOGGER.info(String.format("Easebuzz initiate transaction response : %s", response));
			return response;
		} catch (Exception e) {
			_LOGGER.error("Error while initiating Easebuzz payment", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
	}

	public Object createEasebuzzVA(CreateEasebuzzVAInternalRequest internalRequest) {
		try {
			CreateEasebuzzVARequest easebuzzVaRequest = CreateEasebuzzVARequest.newInstance();
			Map<String, Object> headerMap = new HashMap<>();
			easebuzzVaRequest.setLabel(internalRequest.getLabel());
			if (internalRequest.getEntityType().equals(PaymentConstants.EntityType.STORE)) {
				easebuzzVaRequest.setKey(easebuzzWireKey);
				String hash = hashUtils.getSha512encryptedString(String.join("|", easebuzzWireKey, internalRequest.getLabel(), easebuzzWireSalt));
				headerMap.put("WIRE-API-KEY", easebuzzWireKey);
				headerMap.put("Authorization", hash);
				_LOGGER.info(String.format("Easebuzz create STORE virtual account request authorization hash : %s", hash));
			} else {
				easebuzzVaRequest.setKey(consumerEasebuzzWireKey);
				String hash = hashUtils
						.getSha512encryptedString(String.join("|", consumerEasebuzzWireKey, internalRequest.getLabel(), consumerEasebuzzWireSalt));
				headerMap.put("WIRE-API-KEY", consumerEasebuzzWireKey);
				headerMap.put("Authorization", hash);
				_LOGGER.info(String.format("Easebuzz create USER virtual account request authorization hash : %s", hash));
			}
			Object response = easebuzzWireClient.createEasebuzzVA(headerMap, easebuzzVaRequest);
			_LOGGER.info(String.format("Easebuzz create virtual account response : %s", response));
			return response;
		} catch (Exception e) {
			_LOGGER.error("Error while creating Easebuzz Virtual Account", e);
			throw new ServerException(
					new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while creating Easebuzz Virtual Account. Kindly try again."));
		}
	}

	public void sendCcVerificationCode(String otp, UUID userId, Double amount) {
		NotificationServiceSmsRequest aa = new NotificationServiceSmsRequest();
		aa.setUserId(userId);
		aa.setTemplateName("CC_OTP");
		Map<String, String> bb = new HashMap<>();
		bb.put("amount", amount.toString());
		bb.put("otp", otp);
		aa.setFillers(bb);
		notificationClient.sendCcVerificationCode(Collections.singletonList(aa));
	}

	public Object initiateJuspayPayment(String transactionId, Double transactionAmount, PaymentConstants.EntityType entityType, String customerId) {
		try {
			_LOGGER.info(
					String.format("Juspay initiate txn request for :%s, transactionId : %s and amount : %s", entityType, transactionId, transactionAmount));
			Map<String, Object> headerMap = getJuspayHeaderMap(entityType);
			JuspaySessionRequest body = new JuspaySessionRequest();
			body.setCustomerId(customerId);
			body.setOrderId(transactionId);
			body.setAmount(transactionAmount);
			if (entityType.equals(PaymentConstants.EntityType.USER)) {
				body.setPaymentPageClientId(consumerJuspayMerchantId);
				body.setReturnUrl(consumerJuspayReturnUrl);
			} else {
				body.setPaymentPageClientId(juspayMerchantId);
				body.setReturnUrl(juspayReturnUrl);
			}
			Object response = juspayClient.createSession(headerMap, body);
			_LOGGER.info(String.format("Juspay initiate transaction response : %s", response));
			return response;
		} catch (Exception e) {
			_LOGGER.error("Error while initiating Juspay payment", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
	}

	public Object fetchJuspayOrderStatus(String transactionId, PaymentConstants.EntityType entityType) {
		try {
			_LOGGER.info(String.format("Juspay fetch order status request for :%s, transactionId : %s", entityType, transactionId));
			Map<String, Object> headerMap = getJuspayHeaderMap(entityType);
			Object response = juspayClient.fetchOrderStatus(headerMap, transactionId);
			_LOGGER.info(String.format("Juspay fetch order status response : %s", response));
			return response;
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Juspay payment", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
	}

	private Map<String, Object> getJuspayHeaderMap(EntityType entityType) {
		Map<String, Object> headerMap = new HashMap<>();
		if (entityType.equals(PaymentConstants.EntityType.USER)) {
			headerMap.put("version", consumerJuspayVersion);
			headerMap.put("x-merchantid", consumerJuspayMerchantId);
			headerMap.put("Authorization", consumerJuspayAuthorization);
		} else {
			headerMap.put("version", juspayVersion);
			headerMap.put("x-merchantid", juspayMerchantId);
			headerMap.put("Authorization", juspayAuthorization);
		}
		return headerMap;
	}

	public void markStoreEligibleForTargetCashback(String requesterEntityId) {
		try {
			TargetCBWalletEligibleRequest markWalletEligibleRequest = new TargetCBWalletEligibleRequest();
			markWalletEligibleRequest
					.setWalletEligibilityDate(LocalDateTime.now().plusHours(5).plusMinutes(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
			markWalletEligibleRequest.setStoreIds(List.of(requesterEntityId));
			_LOGGER.info(String.format("Mark Wallet Eligible Request for store : %s ", requesterEntityId));
			offerClient.markStoreEligibleForTargetCashback(markWalletEligibleRequest);
		} catch (Exception e) {
			_LOGGER.info(String.format("error while running mark wallet eligible update : %s", e));
		}
	}

	public Map<String, StoreDataResponse> getStoreDataMapFromIds(Set<String> storeIds) {
		try {
			List<StoreDataResponse> stores = storeClient.getStoreDataFromIds(storeIds.stream().collect(Collectors.joining(",")));
			Map<String, StoreDataResponse> response = stores.stream().collect(Collectors.toMap(StoreDataResponse::getStoreId, Function.identity()));
			return response;
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Stores ", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are unable to fetch store details. Kindly try again."));
		}
	}

	public void refreshCartInternal(UserWalletEntity userWallet) {
		try {
			UpdateFranchiseCartRequest updateFranchiseCartRequest = new UpdateFranchiseCartRequest();
			updateFranchiseCartRequest.setStoreId(userWallet.getEntityId());
			updateFranchiseCartRequest
					.setWalletAmount(BigDecimal.valueOf(userWallet.getAmount()).subtract(BigDecimal.valueOf(userWallet.getWalletHold())).doubleValue());
			orderClient.refreshCart(updateFranchiseCartRequest);
		} catch (Exception e) {
			_LOGGER.error(String.format("error while refreshing cart for store with id %s", userWallet.getEntityId()), e);
		}
	}

	public Object createJuspayCustomer(JuspayCustomerRequest request, EntityType entityType) {
		try {
			_LOGGER.info(String.format("Juspay fetch order status request for :%s", request));
			Map<String, Object> headerMap = getJuspayHeaderMap(entityType);
			Object response = juspayClient.createCustomer(headerMap, request);
			return response;
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Juspay payment", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to make your payment. Kindly try again."));
		}
	}
}
