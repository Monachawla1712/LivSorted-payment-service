package com.sorted.rest.services.payment.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.entity.CashCollectionEntity;
import com.sorted.rest.services.payment.entity.CashCollectionOtpEntity;
import com.sorted.rest.services.payment.services.CashCollectionService;
import com.sorted.rest.services.payment.services.UserWalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Cash collection Services", description = "Cash collection related services.")
public class CashCollectionController implements BaseController {

	private AppLogger _LOGGER = LoggingManager.getLogger(CashCollectionController.class);

	@Autowired
	private CashCollectionService ccService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private BaseMapper<?, ?> baseMapper;

	@ApiOperation(value = "Collect Cash-collection", nickname = "collectedCashCollection")
	@PostMapping(path = "/payments/cash-collections/collect")
	@ResponseStatus(HttpStatus.OK)
	public void collectCashCollection(@RequestBody @Valid CollectCcRequest request) {
		CashCollectionEntity cashCollectionEntity = getMapper().mapSrcToDest(request, CashCollectionEntity.newInstance());
		cashCollectionEntity.setEntityId(request.getStoreId());
		cashCollectionEntity.setEntityType(EntityType.STORE);
		cashCollectionEntity.getMetadata().setCollectedBy(request.getUser());
		ccService.collectCashCollection(cashCollectionEntity, Boolean.FALSE);
	}

	@ApiOperation(value = "Collect Cash-collection", nickname = "collectedCashCollection")
	@PostMapping(path = "/payments/cash-collections/consumer/collect")
	@ResponseStatus(HttpStatus.OK)
	@Transactional
	public void collectConsumerCcCollection(@RequestBody @Valid CollectConsumerCcRequest request) {
		_LOGGER.info(String.format("collectConsumerCcCollection:: request : %s", request));
		CashCollectionEntity cashCollectionEntity = null;
		if (Objects.isNull(request.getCcId())) {
			cashCollectionEntity = getMapper().mapSrcToDest(request, CashCollectionEntity.newInstance());
		} else {
			cashCollectionEntity = ccService.findRecordById(request.getCcId());
			if (request.getStatus().equals(CashCollectionStatus.UNCOLLECTED) && cashCollectionEntity.getStatus().equals(CashCollectionStatus.COLLECTED)) {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, "COLLECTED Cash Collection can't be marked UNCOLLECTED, Try cancelling current collection!",
								""));
			}
			if (request.getStatus().equals(CashCollectionStatus.COLLECTED) && cashCollectionEntity.getStatus().equals(CashCollectionStatus.UNCOLLECTED)) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						"UNCOLLECTED cash collection can't be marked COLLECTED, Try creating new request for Cash Collection", ""));
			}
			cashCollectionEntity.setCollectedAmount(request.getCollectedAmount());
			cashCollectionEntity.setStatus(request.getStatus());
		}
		cashCollectionEntity.setEntityId(request.getCustomerId());
		cashCollectionEntity.setEntityType(EntityType.USER);
		cashCollectionEntity.getMetadata().setCollectedBy(request.getUser());
		cashCollectionEntity.getMetadata().setRemarks(request.getRemarks());
		cashCollectionEntity.getMetadata().setImages(request.getImages());
		cashCollectionEntity.getMetadata().setStoreId(request.getStoreId());
		cashCollectionEntity.getMetadata().setIsFailedByHandPicked(request.getIsFailedByHandPicked());

		if (cashCollectionEntity.getStatus().equals(CashCollectionStatus.COLLECTED)) {
			cashCollectionEntity.setReceivedAmount(request.getCollectedAmount());
			if (request.getTxnMode().equalsIgnoreCase("UPI")) {
				cashCollectionEntity.getMetadata().setTxnMode("Payment-UPI");
				cashCollectionEntity.setStatus(CashCollectionStatus.RECEIVED);
				clientService.updateUserPreference(cashCollectionEntity.getEntityId(), PaymentConstants.UPI);
			} else if (request.getTxnMode().equalsIgnoreCase("CASH")) {
				cashCollectionEntity.getMetadata().setTxnMode("Payment_CC");
				clientService.updateUserPreference(cashCollectionEntity.getEntityId(), PaymentConstants.CASH);
			} else {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Txn mode not supported.", ""));
			}
		}
		ccService.collectCashCollection(cashCollectionEntity, true);
		if (cashCollectionEntity.getStatus().equals(CashCollectionStatus.COLLECTED) && cashCollectionEntity.getMetadata().getTxnMode().equals("Payment_CC")) {
			String key = ccService.getKey(cashCollectionEntity.getId().toString(), null);
			ccService.adjustWalletForCC(cashCollectionEntity, key);
		}
	}

	@ApiOperation(value = "Is Cash-collection Collected", nickname = "collectedCashCollection")
	@GetMapping(path = "/payments/cash-collections/is-collected")
	@ResponseStatus(HttpStatus.OK)
	public Boolean isCashCollected(@RequestParam Date date, @RequestParam String slot, @RequestParam String storeId) {
		return ccService.isCashCollected(date, slot, storeId, EntityType.STORE);
	}

	@ApiOperation(value = "fetch user collected entry", nickname = "collectedCashCollection")
	@PostMapping(path = "/payments/consumer/cash-collections/user")
	@ResponseStatus(HttpStatus.OK)
	public List<CashCollectionResponse> fetchCashCollectionByUserId(@RequestBody @Valid FetchCashCollectionRequest request) {
		_LOGGER.info(String.format("fetchCashCollectionByUserId:: request : %s",request));
		List<CashCollectionEntity> ccList = ccService.fetchCashCollectionByUserId(request.getCustomerId(), request.getCollectorMobileNumber(), request.getDate());
		return prepareConsumerResponseData(ccList);
	}

	@ApiOperation(value = "fetch user collected entry", nickname = "collectedCashCollection")
	@PostMapping(path = "/payments/consumer/cash-collections/user/bulk")
	@ResponseStatus(HttpStatus.OK)
	public List<CashCollectionResponse> fetchCashCollectionBulkUsers(@RequestBody @Valid BulkFetchCcRequest request) {
		_LOGGER.info(String.format("fetchCashCollectionByUserId:: request : %s",request));
		List<CashCollectionStatus> statuses = Arrays.asList(CashCollectionStatus.COLLECTED, CashCollectionStatus.REVERSED);
		List<CashCollectionEntity> ccList = ccService.fetchCashCollectionBulkUsers(request, statuses);
		return prepareConsumerResponseData(ccList);
	}

	@ApiOperation(value = "fetch user cash collection in requested state", nickname = "fetchRequestedCashCollectionBulkUsers")
	@GetMapping(path = "/payments/consumer/cash-collections/requested/user/bulk")
	@ResponseStatus(HttpStatus.OK)
	public List<CashCollectionResponse> fetchCashCollections(@RequestParam String date) {
		_LOGGER.info(String.format("fetchCashCollections:: date : %s", date));
		List<CashCollectionEntity> ccList = ccService.fetchAllCashCollections(date);
		return prepareConsumerResponseData(ccList);
	}

	@ApiOperation(value = "fetch user collected entry", nickname = "collectedCashCollection")
	@PostMapping(path = "/payments/consumer/cash-collections/{ccId}/reverse")
	@ResponseStatus(HttpStatus.OK)
	public void reverseCashCollection(@PathVariable Long ccId, @RequestParam String mobileNumber) {
		CashCollectionEntity cashCollectionEntity = ccService.findRecordById(ccId);
		if (cashCollectionEntity.getMetadata().getCollectedBy().getPhone().equals(mobileNumber)) {
			if (cashCollectionEntity.getStatus() == CashCollectionStatus.COLLECTED) {
				if (cashCollectionEntity.getMetadata().getTxnMode().equals("Payment_CC")) {
					cashCollectionEntity.setStatus(CashCollectionStatus.REVERSED);
					ccService.reverseWalletForCc(cashCollectionEntity);
					ccService.save(cashCollectionEntity);
				}
			} else {
				throw new ValidationException(
						new ErrorBean(Errors.INVALID_REQUEST, String.format("Cash collection entry is in %s status", cashCollectionEntity.getStatus()),
								"status"));
			}

		} else {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "This payment was not done by you, so can not be reversed.", "phone"));
		}
	}

	@ApiOperation(value = "get Cash-collections", nickname = "getCashCollections")
	@GetMapping(path = "/payments/cash-collections")
	public PageAndSortResult<CashCollectionResponse> getCashCollections(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "25") Integer pageSize, @RequestParam(required = false) String sortBy,
			@RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection, HttpServletRequest request) {
		Map<String, PageAndSortRequest.SortDirection> sort = null;
		if (sortBy != null) {
			sort = buildSortMap(sortBy, sortDirection);
		} else {
			sort = new LinkedHashMap<>();
			sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		}
		Map<String, Object> params = getSearchParams(request, CashCollectionEntity.class);
		if (params.containsKey("status")) {
			params.put("status", CashCollectionStatus.fromString(params.get("status").toString()));
		}
		params.put("entityType", EntityType.STORE);
		PageAndSortResult<CashCollectionEntity> ccList = ccService.getAllPaginatedCashCollections(pageSize, pageNo, params, sort);
		PageAndSortResult<CashCollectionResponse> response = new PageAndSortResult<>();
		if (ccList != null && ccList.getData() != null) {
			List<CashCollectionResponse> responseData = prepareResponseData(ccList.getData());
			response = prepareResponsePageData(ccList, CashCollectionResponse.class);
			response.setData(responseData);
		}
		return response;
	}

	@ApiOperation(value = "get Cash-collections", nickname = "getCashCollections")
	@GetMapping(path = "/payments/consumer/cash-collections")
	public PageAndSortResult<CashCollectionResponse> getConsumerCashCollections(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "25") Integer pageSize, @RequestParam(required = false) String sortBy,
			@RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection, HttpServletRequest request) {
		Map<String, PageAndSortRequest.SortDirection> sort = null;
		if (sortBy != null) {
			sort = buildSortMap(sortBy, sortDirection);
		} else {
			sort = new LinkedHashMap<>();
			sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		}
		Map<String, Object> params = getSearchParams(request, CashCollectionEntity.class);
		if (params.containsKey("status")) {
			params.put("status", CashCollectionStatus.fromString(params.get("status").toString()));
		}
		params.put("entityType", EntityType.USER);
		PageAndSortResult<CashCollectionEntity> ccList = ccService.getAllPaginatedCashCollections(pageSize, pageNo, params, sort);
		PageAndSortResult<CashCollectionResponse> response = new PageAndSortResult<>();
		if (ccList != null && ccList.getData() != null) {
			List<CashCollectionResponse> responseData = prepareConsumerResponseData(ccList.getData());
			response = prepareResponsePageData(ccList, CashCollectionResponse.class);
			response.setData(responseData);
		}
		return response;
	}

	private List<CashCollectionResponse> prepareConsumerResponseData(List<CashCollectionEntity> ccList) {
		List<CashCollectionResponse> responseList = new ArrayList<>();
		ccList.forEach(item -> {
			if (item.getEntityType().equals(EntityType.USER)) {
				CashCollectionResponse response = CashCollectionResponse.builder().id(item.getId()).collectedAmount(item.getCollectedAmount())
						.requestedAmount(item.getRequestedAmount()).receivedAmount(item.getReceivedAmount()).status(item.getStatus().name())
						.metadata(item.getMetadata()).date(item.getDate()).slot(item.getSlot()).customerId(item.getEntityId()).build();
				response.setCreatedAt(item.getCreatedAt().toString());
				response.setModifiedAt(item.getModifiedAt().toString());
				response.setCreationTime(item.getCreatedAt());
				responseList.add(response);
			}
		});
		return responseList;
	}

	private List<CashCollectionResponse> prepareResponseData(List<CashCollectionEntity> ccList) {
		List<CashCollectionResponse> responseList = new ArrayList<>();
		ccList.forEach(item -> {
			if (item.getEntityType().equals(EntityType.STORE)) {
				CashCollectionResponse response = CashCollectionResponse.builder().id(item.getId()).collectedAmount(item.getCollectedAmount())
						.requestedAmount(item.getRequestedAmount()).receivedAmount(item.getReceivedAmount()).status(item.getStatus().name())
						.metadata(item.getMetadata()).date(item.getDate()).slot(item.getSlot()).storeId(item.getEntityId()).build();
				responseList.add(response);
			}
		});
		return responseList;
	}

	@ApiOperation(value = "Receive Cash-collection", nickname = "receiveCashCollection")
	@PostMapping(path = "/payments/cash-collections/receive")
	public ResponseEntity<CashCollectionResponse> receiveCashCollection(@RequestBody @Valid ReceiveCcRequest request) {
		CashCollectionEntity entity = ccService.receiveCashCollection(request);
		CashCollectionResponse response = new CashCollectionResponse();
		response = getMapper().mapSrcToDest(entity, response);
		if (entity.getEntityType().equals(EntityType.STORE)) {
			response.setStoreId(entity.getEntityId());
		} else if (entity.getEntityType().equals(EntityType.USER)) {
			response.setCustomerId(entity.getEntityId());
		}
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Bulk Receive Cash-collection", nickname = "receiveCashCollection")
	@PostMapping(path = "/payments/cash-collections/bulk/receive")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<ErrorBean>> bulkReceiveCashCollection(@RequestBody @Valid List<ReceiveCcRequest> ccRequests) {
		List<ErrorBean> errors = new ArrayList<>();
		for (ReceiveCcRequest request : ccRequests) {
			try {
				ccService.receiveCashCollection(request);
			} catch (Exception e) {
				errors.add(ErrorBean.withError(null, e.getMessage()));
			}
		}
		return ResponseEntity.ok(errors);
	}

	@ApiOperation(value = "Approve/Reject Cash-collection", nickname = "bulkApproveOrRejectCashCollection")
	@PostMapping(path = "/payments/cash-collections/approve")
	public ResponseEntity<List<ErrorBean>> bulkApproveOrRejectCashCollection(@RequestBody @Valid BulkActionCcRequest request) {
		return ResponseEntity.ok(ccService.bulkApproveOrRejectCashCollection(request));
	}

	@ApiOperation(value = "Send Cash-collection OTP", nickname = "fosCashCollection")
	@PostMapping(path = "/payments/cash-collections/fos/otp")
	@ResponseStatus(HttpStatus.OK)
	public void sendFosCashCollectionVerificationCode(@RequestBody @Valid FosCcOtpRequest request) {
		if (!Objects.equals(request.getTxnMode(), "Payment-CASH")) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Txn Mode should be Cash Only.", ""));
		}
		StoreDataResponse storeDetails = clientService.getStoreDataFromId(request.getStoreId());
		if (storeDetails == null || storeDetails.getOwnerId() == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Store details not found.", ""));
		}
		UserDetailsResponse userDetails = clientService.getUserDetails(storeDetails.getOwnerId());
		if (userDetails == null || userDetails.getPhoneNumber() == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User details not found.", ""));
		}
		CashCollectionOtpEntity ccOtp = ccService.generateAndSaveNewCcVerificationCode(request, storeDetails, userDetails);
		clientService.sendCcVerificationCode(ccOtp.getOtp(), UUID.fromString(userDetails.getId()), request.getAmount());
	}

	@ApiOperation(value = "Internal Cash-collection", nickname = "internalCashCollection")
	@PostMapping(path = "/payments/cash-collections/internal")
	@ResponseStatus(HttpStatus.OK)
	public Long internalCashCollection(@RequestBody @Valid CashCollectionRequest request) {
		if (request.getUserId() == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User Id not found.", "userId"));
		}
		validateCashCollectionRequest(request);
		CashCollectionEntity entity = ccService.cashCollectionFromRequest(request);
		return entity.getId();
	}

	@ApiOperation(value = "Receive Cash-collection By FOS", nickname = "fosCashCollection")
	@PostMapping(path = "/payments/cash-collections/fos")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public Long fosCashCollection(@RequestBody @Valid CashCollectionRequest request) {
		validateCashCollectionRequest(request);
		validateFosUnpaidCashCcEligibility(request);
		request.setUserId(SessionUtils.getAuthUserId());
		CashCollectionEntity entity = ccService.cashCollectionFromRequest(request);
		return entity.getId();
	}

	private void validateCashCollectionRequest(CashCollectionRequest request) {
		//		if (Objects.equals(request.getTxnMode(), "Payment-CASH") && request.getVerificationCode() == null) {
		//			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Verification Code is required.", ""));
		//		} else if (Objects.equals(request.getTxnMode(), "Payment-CASH")) {
		//			ccService.VerifyCcVerificationCode(request);
		//		}
		if (!request.getStatus().equals(CashCollectionStatus.RECEIVED) && !request.getStatus().equals(CashCollectionStatus.UNCOLLECTED)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Cash collection status can either be UNCOLLECTED or RECEIVED", ""));
		}
		if (request.getStatus().equals(CashCollectionStatus.RECEIVED)) {
			if (request.getAmount() == null || request.getAmount().compareTo(0d) < 1) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Amount must be greater than 0", ""));
			}
			String paymentTypeLabels = ParamsUtils.getParam("PAYMENT_TYPE_LABELS",
					"Payment-CASH,Payment-UPI(HDFC-QR),Payment-UPI(Store-QR),CHEQUE,BANK_TRANSFER,Partner-App-PG");
			HashSet<String> txnTypes = Arrays.asList(paymentTypeLabels.split(",")).stream().collect(Collectors.toCollection(HashSet::new));
			if (request.getTxnMode() == null || !txnTypes.contains(request.getTxnMode())) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Transaction mode must be one of " + String.join(",", txnTypes), ""));
			}
		} else if (request.getStatus().equals(CashCollectionStatus.UNCOLLECTED)) {
			if (StringUtils.isEmpty(request.getRemarks())) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Reason can not be empty", ""));
			}
		}
	}

	// validating if Fos has restricting unpaid CCs
	private void validateFosUnpaidCashCcEligibility(CashCollectionRequest request) {
		Set<String> userRoles = SessionUtils.getAuthUserRoles();
		if (!userRoles.contains("ADMIN") && request.getTxnMode().equals(PaymentConstants.PAYMENT_CASH_TXN_TYPE)) {
			Integer sinceDays = ParamsUtils.getIntegerParam("FOS_UNPAID_CC_LIMIT_DAYS", 0);
			java.util.Date cutoffDate = DateUtils.addDays(Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata"))), -1 * sinceDays);
			List<CashCollectionEntity> unpaidCCs = ccService.fetchFosReceivedCCs(SessionUtils.getAuthUserId().toString(), null);
			Date oldestUnpaidCC = unpaidCCs.stream().map(CashCollectionEntity::getDate).min(Date::compareTo).orElse(null);
			if (oldestUnpaidCC != null && oldestUnpaidCC.before(cutoffDate)) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("You need to settle unpaid cash collections older than %s to continue",
								DateUtils.toString(DateUtils.DATE_MM_FMT, cutoffDate)), null));
			}
			BigDecimal totalUnpaidCC = unpaidCCs.stream().map(e -> BigDecimal.valueOf(e.getReceivedAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal unpaidCCLimit = ccService.getCCDueLimitForUser(userRoles);
			if (totalUnpaidCC.add(BigDecimal.valueOf(request.getAmount())).compareTo(unpaidCCLimit) > 0) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("Your total unpaid cash collections exceed the limit of %s. Pay your current unpaid cash collections of %s to continue",
								unpaidCCLimit, totalUnpaidCC), null));
			}
		}
	}

	@ApiOperation(value = "Fos get cash-collections", nickname = "getFosReceivedCashCc")
	@GetMapping(path = "/payments/cash-collections/fos/cash-received")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<List<CashCollectionResponse>> fetchFosReceivedCCs() {
		List<CashCollectionEntity> entities = ccService.fetchFosReceivedCCs(SessionUtils.getAuthUserId().toString(), null);

		List<CashCollectionResponse> response = prepareResponseData(entities);
		ccService.addStoreNames(response);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Create Cash Collection Request from Backoffice", nickname = "createCashCollectionRequest")
	@PostMapping("/payments/cash-collections/customer")
	public ResponseEntity<Void> createCashCollectionRequest(@Valid @RequestBody CcCreationRequest request) {
		List<CashCollectionEntity> ccEntities = ccService.fetchExistingRequestedCcByUserIds(List.of(request.getCustomerId()));
		ccService.checkExistingAndGenerateCcRequest(ccEntities, request.getCustomerId());
		return ResponseEntity.ok().build();
	}

	@ApiOperation(value = "Create Cash Collection Request from Consumer App", nickname = "createAppCashCollectionRequest")
	@PostMapping("/payments/cash-collections/customer/app")
	public ResponseEntity<Void> createAppCashCollectionRequest() {
		String userId = SessionUtils.getAuthUserId().toString();
		List<CashCollectionEntity> ccEntities = ccService.fetchExistingRequestedCcByUserIds(List.of(userId));
		ccService.checkExistingAndGenerateCcRequest(ccEntities, userId);
		return ResponseEntity.ok().build();
	}

	@ApiOperation(value = "generate cash collections", nickname = "generateCashCollections")
	@PostMapping("/payments/generate/cash-collections")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public void generateCashCollections() {
		_LOGGER.info("generateCashCollections triggered");
		List<String> userIds = userWalletService.getUsersBalanceLessThanAmount(EntityType.USER, Double.valueOf(ParamsUtils.getParam("MIN_CC_AMOUNT", "-200")));
		List<UserDetailsResponse> userDetails = clientService.getUserDetailsByIds(userIds);
		if (CollectionUtils.isEmpty(userDetails)) {
			throw new ValidationException(ErrorBean.withError(Errors.NO_DATA_FOUND, "No users found for cash collection", "null"));
		}
		List<String> filteredUserIds = userDetails.stream()
				.filter(user -> user.getUserPreferences() != null && user.getUserPreferences().getPaymentMethod() != null && user.getUserPreferences()
						.getPaymentMethod().equalsIgnoreCase("CASH")).map(UserDetailsResponse::getId).collect(Collectors.toList());
//		List<CashCollectionEntity> unCollectedCcEntities= ccService.fetchExistingUnCollectedCcByDate(Date.valueOf(LocalDate.now().minusDays(1)));
//		filteredUserIds.addAll(unCollectedCcEntities.stream().map(CashCollectionEntity::getEntityId).collect(Collectors.toList()));
//		filteredUserIds = filteredUserIds.stream().distinct().collect(Collectors.toList());
		ccService.inactivateExistingAndGenerateNewCcRequest(filteredUserIds);
	}

	@PutMapping("/payments/cash-collections/cancel/{id}")
	public void cancelCashCollection(@PathVariable("id") Long id) {
		String userId = SessionUtils.getAuthUserId().toString();
		CashCollectionEntity cashCollectionEntity = ccService.findRecordById(id);
		if (!Objects.equals(cashCollectionEntity.getEntityId(), userId)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "You are not authorized to cancel this cash collection", ""));
		}
		ccService.cancelCashCollection(cashCollectionEntity);
	}

	@GetMapping("/payments/internal/cash-collections/{userId}")
	public ResponseEntity<CashCollectionResponse> fetchRequestedCashCollection(@PathVariable("userId") String userId) {
		List<CashCollectionEntity> entities = ccService.fetchExistingRequestedCcByUserIds(new ArrayList<>(Collections.singleton(userId)));
		if (CollectionUtils.isEmpty(entities)) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}
		Date date = ccService.getDateOfCashCollection();
		CashCollectionEntity entity = entities.get(0);
		if (entity.getDate().equals(date)) {
			CashCollectionResponse response = new CashCollectionResponse();
			response = getMapper().mapSrcToDest(entity, response);
			response.setStoreId(entity.getEntityId());
			return ResponseEntity.ok(response);
		}
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return baseMapper;
	}
}
