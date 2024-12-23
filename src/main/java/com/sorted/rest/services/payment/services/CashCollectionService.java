package com.sorted.rest.services.payment.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.*;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.beans.BulkActionCcRequest.CcDetails;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.entity.CashCollectionEntity;
import com.sorted.rest.services.payment.entity.CashCollectionOtpEntity;
import com.sorted.rest.services.payment.entity.UserWalletEntity;
import com.sorted.rest.services.payment.repository.CashCollectionOtpRepository;
import com.sorted.rest.services.payment.repository.CashCollectionRepository;
import com.sorted.rest.services.payment.utils.UserUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CashCollectionService implements BaseService<CashCollectionEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(CashCollectionService.class);

	@Autowired
	private CashCollectionRepository ccRepository;

	@Autowired
	private CashCollectionOtpRepository ccOtpRepository;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private UserUtils userUtils;

	@Autowired
	private ClientService clientService;

	@Transactional(propagation = Propagation.REQUIRED)
	public CashCollectionEntity save(CashCollectionEntity entity) {
		CashCollectionEntity result = ccRepository.save(entity);
		return result;
	}

	public void collectCashCollection(CashCollectionEntity ccEntity, Boolean isConsumer) {
		if (!ccEntity.getStatus().equals(CashCollectionStatus.UNCOLLECTED)) {
			if (!ccEntity.getMetadata().getTxnMode().equalsIgnoreCase("Payment-UPI") && !(ccEntity.getStatus().equals(CashCollectionStatus.COLLECTED)
					&& ccEntity.getCollectedAmount() != null && ccEntity.getCollectedAmount().compareTo(0d) > 0)
					&& !ccEntity.getStatus().equals(CashCollectionStatus.CANCELLED)) {
				throw new ValidationException(
						ErrorBean.withError(Errors.INVALID_REQUEST, "Cash collection can either be cancelled or collected with amount greater than 0", ""));
			}
		} else if (ccEntity.getStatus().equals(CashCollectionStatus.UNCOLLECTED) && Boolean.TRUE.equals(ccEntity.getMetadata().getIsFailedByHandPicked())) {
			UserWalletEntity userWallet = userWalletService.getUserWallet(ccEntity.getEntityId(), EntityType.USER);
			if (Double.compare(userWallet.getAmount(), 0.0) < 0) {
				userWalletService.updateWalletCreditLimit(userWallet, Math.abs(userWallet.getAmount()));
			}
		}
		if (!isConsumer && isCashCollected(ccEntity.getDate(), ccEntity.getSlot(), ccEntity.getEntityId(), ccEntity.getEntityType())) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Cash collection already exists for this date and slot for storeId : %s", ccEntity.getEntityId()), ""));
		}
		save(ccEntity);
	}

	public Boolean isCashCollected(Date date, String slot, String entityId, EntityType entityType) {
		return ccRepository.existsByEntityIdAndDateAndSlotAndEntityType(entityId, date, slot, entityType);
	}

	public PageAndSortResult<CashCollectionEntity> getAllPaginatedCashCollections(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<CashCollectionEntity> cashCollections = null;
		cashCollections = findPagedRecords(filters, sort, pageSize, pageNo);
		return cashCollections;
	}

	public CashCollectionEntity receiveCashCollection(ReceiveCcRequest request) {
		if (!(request.getStatus().equals(CashCollectionStatus.RECEIVED) && request.getReceivedAmount() != null
				&& request.getReceivedAmount().compareTo(0d) == 1) && !request.getStatus().equals(CashCollectionStatus.CANCELLED)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Cash collection with id: %s can either be cancelled or received with amount greater than 0", request.getId()), ""));
		}
		Optional<CashCollectionEntity> cashCollectionEntity = ccRepository.findById(request.getId());
		if (!cashCollectionEntity.isPresent() || !cashCollectionEntity.get().getStatus().equals(CashCollectionStatus.COLLECTED)) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
					String.format("Cash collection with id : %s not found in collected status", request.getId()), ""));
		}
		cashCollectionEntity.get().setReceivedAmount(request.getReceivedAmount());
		cashCollectionEntity.get().getMetadata().setReceivedBy(request.getUser());
		cashCollectionEntity.get().setStatus(request.getStatus());
		CashCollectionEntity entity = save(cashCollectionEntity.get());
		return entity;
	}

	public List<ErrorBean> bulkApproveOrRejectCashCollection(BulkActionCcRequest request) {
		List<ErrorBean> errors = new ArrayList<>();
		List<Long> ids = request.getCcDetails().stream().map(CcDetails::getId).distinct().collect(Collectors.toList());
		Map<String, Object> params = new HashMap<>();
		params.put("id", ids);
		Map<Long, CashCollectionEntity> entityMap = findAllRecords(params).stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
		if (!request.getStatus().equals(CashCollectionStatus.APPROVED) || !request.getStatus().equals(CashCollectionStatus.REJECTED)) {
			for (CcDetails ccObj : request.getCcDetails()) {
				if (!entityMap.containsKey(ccObj.getId())) {
					errors.add(ErrorBean.withError(null, String.format("Cash-collection with id : %s not found to process", ccObj.getId().toString()), null));
					continue;
				}
				CashCollectionEntity ccEntity = entityMap.get(ccObj.getId());
				if (!ccEntity.getStatus().equals(CashCollectionStatus.RECEIVED)) {
					errors.add(ErrorBean.withError(ccEntity.getReceivedAmount().toString(), "Cash-collection not found in RECEIVED state", ccEntity.getEntityId()));
					continue;
				}
				ccEntity.setStatus(request.getStatus());
				ccEntity.getMetadata().setTxnMode(ccObj.getTxnMode());
				ccEntity.getMetadata().setApprovedBy(userUtils.getUserDetail(SessionUtils.getAuthUserId()));
				if (request.getReferenceId() != null) {
					ccEntity.setReferenceId(request.getReferenceId());
				}
				if (StringUtils.isNotEmpty(ccObj.getRemarks())) {
					ccEntity.getMetadata().setRemarks(ccObj.getRemarks());
				}
				try {
					if (ccEntity.getEntityType().equals(EntityType.STORE) && request.getStatus().equals(CashCollectionStatus.APPROVED)) {
						adjustWalletForCC(ccEntity, null);
					}
					save(ccEntity);
				} catch (Exception e) {
					_LOGGER.error("Error while finalizing cash-collection ", e);
					errors.add(ErrorBean.withError(ccEntity.getReceivedAmount().toString(), e.getMessage(), ccEntity.getEntityId()));
				}
			}
		} else {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "invalid action.", "action"));
		}
		return errors;
	}

	public void adjustWalletForCC(CashCollectionEntity ccEntity, String key) {
		if (ccEntity.getReceivedAmount().compareTo(0d) < 1) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Cash-collection of amount less than zero is not allowed", "amount"));
		}
		WalletTxnBean txnBean = new WalletTxnBean(ccEntity.getReceivedAmount(), ccEntity.getMetadata().getTxnMode(), "CC-" + ccEntity.getId().toString(),
				WalletType.WALLET, null, null);
		if (StringUtils.isEmpty(key)) {
			key = getKey(ccEntity.getId().toString(), txnBean.getWalletType());
		}
		if (ccEntity.getEntityType().equals(EntityType.STORE)) {
			userWalletService.addOrDeduct(txnBean, ccEntity.getEntityId(), EntityType.STORE, key);
		} else if (ccEntity.getEntityType().equals(EntityType.USER)) {
			userWalletService.addOrDeduct(txnBean, ccEntity.getEntityId(), EntityType.USER, key);
		} else {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Entity type not supported", "entityType"));
		}
	}

	public String getKey(String entityId, WalletType walletType) {
		StringBuilder sb = new StringBuilder();
		sb.append(PaymentConstants.PAYMENT_CC_KEY);
		sb.append(entityId);
		if (walletType != null) {
			sb.append("|");
			sb.append(walletType);
		}
		return sb.toString();
	}

	public void reverseWalletForCc(CashCollectionEntity ccEntity) {
		WalletTxnBean txnBean = new WalletTxnBean(-1 * ccEntity.getReceivedAmount(), ccEntity.getMetadata().getTxnMode(), "CC-" + ccEntity.getId().toString(),
				WalletType.WALLET, PaymentConstants.PAYMENT_REVERSAL, null);
		userWalletService.addOrDeduct(txnBean, ccEntity.getEntityId(), EntityType.USER,
				PaymentConstants.PAYMENT_CC_KEY + ccEntity.getId().toString() + "|Reverse");
	}

	public CashCollectionEntity cashCollectionFromRequest(CashCollectionRequest request) {
		LocalDate date = LocalDate.now(ZoneId.of("Asia/Kolkata"));
		String key = generateCcKey(request, date);
		if (keyExists(key)) {
			throw new ValidationException(
					ErrorBean.withError(Errors.INVALID_REQUEST, "Duplicate request found. If not duplicate, then retry after a minute", ""));
		}
		CashCollectionEntity ccEntity = CashCollectionEntity.newInstance();
		ccEntity.setKey(key);
		if (request.getStatus().equals(CashCollectionStatus.RECEIVED)) {
			ccEntity.setRequestedAmount(request.getBillAmount());
			ccEntity.setCollectedAmount(request.getAmount());
			ccEntity.setReceivedAmount(request.getAmount());
			ccEntity.getMetadata().setTxnMode(request.getTxnMode());
			ccEntity.getMetadata().setRemarks(request.getRemarks());
		} else if (request.getStatus().equals(CashCollectionStatus.UNCOLLECTED)) {
			ccEntity.getMetadata().setRemarks(request.getRemarks());
		}
		ccEntity.setStatus(request.getStatus());
		ccEntity.setEntityId(request.getStoreId());
		ccEntity.setEntityType(EntityType.STORE);
		ccEntity.setDate(Date.valueOf(date));
		UserDetail user = userUtils.getUserDetail(request.getUserId());
		ccEntity.getMetadata().setCollectedBy(user);
		ccEntity.getMetadata().setReceivedBy(user);
		CashCollectionEntity savedEntity = save(ccEntity);
		// moved to cash collection approval
//		Integer enableCashAutoCredit = ParamsUtils.getIntegerParam("ENABLE_CASH_AUTO_CREDIT", 1);
//		if (enableCashAutoCredit == 1 && request.getStatus().equals(CashCollectionStatus.RECEIVED) && request.getTxnMode().equals("Payment-CASH")) {
//			adjustWalletForCC(savedEntity);
//		}
		return savedEntity;
	}

	private String generateCcKey(CashCollectionRequest request, LocalDate date) {
		LocalTime time = LocalTime.now();
		return String.format("%s|%s|%s|%s", request.getStoreId(), request.getAmount(), request.getUserId(), date.atTime(time.getHour(), time.getMinute()));
	}

	@Override
	public Class<CashCollectionEntity> getEntity() {
		return CashCollectionEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return ccRepository;
	}

	public void VerifyCcVerificationCode(CashCollectionRequest request) {
		CashCollectionOtpEntity otp = ccOtpRepository.getPendingOtpForCashCollection(request.getStoreId(), request.getVerificationCode(), new java.util.Date());
		if (otp == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "OTP does not match or already Used.", ""));
		}
		otp.setVerified(1);
		ccOtpRepository.save(otp);
	}

	private static String generateOtp(Integer length) {
		if (ContextUtils.isProfileProd()) {
			return StringUtils.leftPad(RandomStringUtils.randomNumeric(length), length, '0');
		} else {
			return StringUtils.leftPad(ParamsUtils.getParam("DEFAULT_OTP", "123456"), length, '0');
		}
	}

	public static java.util.Date getOtpExpiry() {
		final Integer validity = ParamsUtils.getIntegerParam("OTP_EXPIRY", 600);
		return getOtpExpiry(validity);
	}

	public static java.util.Date getOtpExpiry(final Integer validity) {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, validity);
		return cal.getTime();
	}

	public CashCollectionOtpEntity generateAndSaveNewCcVerificationCode(FosCcOtpRequest request, StoreDataResponse storeDetails,
			UserDetailsResponse userDetails) {
		CashCollectionOtpEntity ccOtp = ccOtpRepository.getPendingCcOtp(storeDetails.getStoreId(), new java.util.Date());
		if (ccOtp == null) {
			ccOtpRepository.deactivateOldCcOtps(storeDetails.getStoreId());
			String otp = "123456";
			ccOtp = CashCollectionOtpEntity.buildCashCollectionOtpEntity(userDetails.getPhoneNumber(), storeDetails.getStoreId(), otp, getOtpExpiry(),
					request.getAmount());
		}
		ccOtp.setAttempts(ccOtp.getAttempts() + 1);
		ccOtp.setAmount(request.getAmount());
		ccOtpRepository.save(ccOtp);
		return ccOtp;
	}

	public List<CashCollectionEntity> fetchFosReceivedCCs(String userId, List<Long> ccIds) {
		Boolean skipCheckCcIds = CollectionUtils.isEmpty(ccIds);
		List<Long> ccIdsFilter = skipCheckCcIds ? List.of(0L) : ccIds; // dummy value
		return ccRepository.getByStatusAndReceivedByAndTxnModeAndCcIds(CashCollectionStatus.RECEIVED.toString(), userId, PaymentConstants.PAYMENT_CASH_TXN_TYPE,
				ccIdsFilter, skipCheckCcIds).stream().sorted(Comparator.comparing(CashCollectionEntity::getId)).collect(Collectors.toList());
	}

	public void addStoreNames(List<CashCollectionResponse> response) {
		Map<String, StoreDataResponse> storeDataResponseMap = clientService
				.getStoreDataMapFromIds(response.stream().map(CashCollectionResponse::getStoreId).collect(Collectors.toSet()));
		for (CashCollectionResponse cashCollectionResponse : response) {
			cashCollectionResponse.setStoreName(storeDataResponseMap.containsKey(cashCollectionResponse.getStoreId())
					? storeDataResponseMap.get(cashCollectionResponse.getStoreId()).getName()
					: null);
		}
	}

	public Boolean keyExists(String key) {
		return ccRepository.keyExists(key);
	}

	public BigDecimal getCCDueLimitForUser(Set<String> userRoles) {
		Map<String, BigDecimal> ccDueLimitRoleMap = getCCDueLimitRoleMap();
		return getCCDueLimit(ccDueLimitRoleMap, userRoles);
	}

	private Map<String, BigDecimal> getCCDueLimitRoleMap() {
		String ccDueLimitRoleMapParam = ParamsUtils.getParam("CC_DUE_LIMIT_ROLE_MAP");
		return Arrays.stream(ccDueLimitRoleMapParam.split(",")).map(pair -> pair.split(":"))
				.collect(Collectors.toMap(pair -> pair[0], pair -> new BigDecimal(pair[1])));
	}

	private BigDecimal getCCDueLimit(Map<String, BigDecimal> ccDueLimitRoleMap, Set<String> userRoles) {
		String ccDueLimitRole = userRoles.stream().filter(ccDueLimitRoleMap::containsKey).findFirst().orElse(null);
		if (ccDueLimitRole == null) {
			throw new ValidationException(new ErrorBean(Errors.AUTHORIZATION_EXCEPTION, "User not authorized to collect cash."));
		}
		return ccDueLimitRoleMap.get(ccDueLimitRole);
	}

	public List<CashCollectionEntity> fetchCashCollectionByUserId(String customerId, String mobileNumber, String date) {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("date", DateUtils.getDate(date));
		filters.put("entityId", customerId);
		filters.put("entityType", EntityType.USER);
		filters.put("status", CashCollectionStatus.COLLECTED);
		List<CashCollectionEntity> cashCollectionEntities = findAllRecords(filters);
		List<CashCollectionEntity> filteredCcEntities = cashCollectionEntities.stream()
				.filter(s -> s.getMetadata().getTxnMode().equals("Payment_CC") && s.getMetadata().getCollectedBy().getPhone().equals(mobileNumber.trim()))
				.collect(Collectors.toList());
		return filteredCcEntities;
	}

	public List<CashCollectionEntity> fetchCashCollectionBulkUsers(BulkFetchCcRequest request, List<CashCollectionStatus> statuses) {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("date", DateUtils.getDate(request.getDate()));
		filters.put("entityId", request.getCustomerIds());
		filters.put("entityType", EntityType.USER);
		filters.put("status", statuses);
		return findAllRecords(filters);
	}

	public List<CashCollectionEntity> fetchAllCashCollections(String date) {
		Map<String, Object> filters = defaultFilterMap();
		List<CashCollectionStatus> statuses = Arrays.asList(CashCollectionStatus.REQUESTED, CashCollectionStatus.UNCOLLECTED, CashCollectionStatus.COLLECTED,
				CashCollectionStatus.REVERSED);
		filters.put("date", Date.valueOf(date));
		filters.put("entityType", EntityType.USER);
		filters.put("status", statuses);
		return findAllRecords(filters);
	}

	public List<CashCollectionEntity> fetchExistingRequestedCcByUserIds(List<String> customerIds) {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("entityId", customerIds);
		filters.put("entityType", EntityType.USER);
		filters.put("status", CashCollectionStatus.REQUESTED);
		return findAllRecords(filters);
	}

	public List<CashCollectionEntity> fetchExistingUnCollectedCcByDate(Date date) {
		Map<String, Object> filters = defaultFilterMap();

		filters.put("date", date);
		filters.put("entityType", EntityType.USER);
		filters.put("status", CashCollectionStatus.UNCOLLECTED);
		return findAllRecords(filters);
	}

	public java.sql.Date getDateOfCashCollection() {
		LocalDateTime now = LocalDateTime.now();
		Date date;
		String param = ParamsUtils.getParam("CASH_COLLECTION_DATE_RESET_TIME", "06:00:00");
		if (now.toLocalTime().isAfter(LocalTime.parse(param))) {
			date = Date.valueOf(now.toLocalDate().plusDays(1));
		} else {
			date = Date.valueOf(now.toLocalDate());
		}
		return new java.sql.Date(date.getTime());
	}

	public void saveAll(List<CashCollectionEntity> ccEntities) {
		ccRepository.saveAll(ccEntities);
	}

	public void inactivateExistingAndGenerateNewCcRequest(List<String> userIds) {
		List<CashCollectionEntity> existingCcEntities = fetchExistingRequestedCcByUserIds(userIds);
		Map<String, CashCollectionEntity> userIdAndCcMap = new HashMap<>();
		Date date = getDateOfCashCollection();
		if (CollectionUtils.isNotEmpty(existingCcEntities)) {
			inactiveExistingCCEntities(existingCcEntities, date, userIdAndCcMap);
		} else {
			existingCcEntities = new ArrayList<>();
		}
		UserDetail authUser = userUtils.getUserDetail(SessionUtils.getAuthUserId());
		for (String userId : userIds) {
			if (!userIdAndCcMap.containsKey(userId)) {
				CashCollectionEntity value = createCcEntity(userId, authUser, date);
				value.getMetadata().setCcType(PaymentConstants.CashCollectionType.DUE_COLLECTION);
				userIdAndCcMap.put(userId, value);
				existingCcEntities.add(value);
			}
		}
		saveAll(existingCcEntities);
	}

	private void inactiveExistingCCEntities(List<CashCollectionEntity> existingCcEntities, Date date, Map<String, CashCollectionEntity> userIdAndCcMap) {
		for (CashCollectionEntity entity : existingCcEntities) {
			if (entity.getStatus().equals(CashCollectionStatus.REQUESTED) && entity.getDate().equals(date)) {
				userIdAndCcMap.put(entity.getEntityId(), entity);
				continue;
			}
			entity.setActive(0);
		}
	}

	public void checkExistingAndGenerateCcRequest(List<CashCollectionEntity> ccEntities, String customerId) {
		Date date = getDateOfCashCollection();
		if (!CollectionUtils.isEmpty(ccEntities)) {
			CashCollectionEntity requestedCcToday = null;
			for (CashCollectionEntity ccEntity : ccEntities) {
				if (ccEntity.getDate().equals(date)) {
					requestedCcToday = ccEntity;
					continue;
				}
				ccEntity.setActive(0);
			}
			if (!Objects.isNull(requestedCcToday)) {
				//this error  code is being user on frontend level for this specific case,use some other code for other cases
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "Request is already present", "entityId"));
			}
			saveAll(ccEntities);
		}
		UserDetail authUser = userUtils.getUserDetail(SessionUtils.getAuthUserId());
		CashCollectionEntity entity = createCcEntity(customerId, authUser, date);
		entity.getMetadata().setCcType(PaymentConstants.CashCollectionType.ADVANCE_REQUEST);
		save(entity);
	}

	public CashCollectionEntity createCcEntity(String customerId, UserDetail authUser, Date date) {
		CashCollectionEntity ccEntity = CashCollectionEntity.newInstance();
		CcMetadata metadata = CcMetadata.newInstance();
		ccEntity.setEntityId(customerId);
		ccEntity.setEntityType(EntityType.USER);
		ccEntity.setStatus(CashCollectionStatus.REQUESTED);
		ccEntity.setDate(date);
		metadata.setRequestedBy(authUser);
		ccEntity.setMetadata(metadata);
		return ccEntity;
	}

	public List<CashCollectionEntity> fetchExistingRequestedCcByUserIdAndDate(String customerId, Date date) {
		Map<String, Object> filters = defaultFilterMap();
		filters.put("entityId", customerId);
		filters.put("entityType", EntityType.USER);
		filters.put("status", CashCollectionStatus.REQUESTED);
		filters.put("date", date);
		return findAllRecords(filters);
	}

	public void cancelCashCollection(CashCollectionEntity cashCollectionEntity) {
		if (cashCollectionEntity == null) {
			throw new ValidationException(new ErrorBean(Errors.NO_DATA_FOUND, "Cash collection not found", "id"));
		}
		if (cashCollectionEntity.getStatus().equals(CashCollectionStatus.COLLECTED)) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Collected cash collection cannot be cancelled", "status"));
		}
		cashCollectionEntity.setActive(0);
		ccRepository.save(cashCollectionEntity);
	}
}