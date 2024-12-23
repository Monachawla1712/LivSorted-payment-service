package com.sorted.rest.services.payment.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletStatus;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletTxnMode;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.entity.CreditLimitChangeEntity;
import com.sorted.rest.services.payment.entity.UserWalletEntity;
import com.sorted.rest.services.payment.entity.WalletStatementEntity;
import com.sorted.rest.services.payment.repository.CreditLimitChangeRepository;
import com.sorted.rest.services.payment.repository.UserWalletRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserWalletService implements BaseService<UserWalletEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(UserWalletService.class);

	@Autowired
	private UserWalletRepository userWalletRepository;

	@Autowired
	private WalletStatementService walletStatementService;

	@Autowired
	private CreditLimitChangeRepository creditLimitChangeRepository;

	@Autowired
	private ClientService clientService;

	public Set<String> getRole() {
		Set<String> role = SessionUtils.getAuthUser().getUserRoles();
		Assert.notNull(role, "role could not be empty");
		return role;
	}

	public UUID getUserId() {
		UUID userId = SessionUtils.getAuthUserId();
		Assert.notNull(userId, "Not able to fetch userId from Session");
		return userId;
	}

	public String getStoreId() {
		String storeId = SessionUtils.getStoreId();
		return storeId;
	}

	public EntityDetailBean getEntityDetail() {
		EntityDetailBean entity = new EntityDetailBean();
		Set<String> role = getRole();
		if ((role.contains("FRANCHISEOWNER") || role.contains("FOSUSER")) && getStoreId() != null) {
			_LOGGER.info(String.format("Fetching storeId"));
			entity.setEntityId(getStoreId());
			entity.setEntityType(EntityType.STORE);
		} else {
			String userId = getUserId().toString();
			entity.setEntityId(userId);
			entity.setEntityType(EntityType.USER);
		}
		_LOGGER.info(String.format("Fetching Entity Detail"));
		return entity;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UserWalletEntity getUserWallet(String entityId, EntityType entityType) {
		_LOGGER.info(String.format("Fetching wallet for user %s", entityId));
		UserWalletEntity userWallet = findOrCreateByEntityId(entityId, entityType);
		return userWallet;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UserWalletEntity save(UserWalletEntity entity) {
		return userWalletRepository.save(entity);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveAll(List<UserWalletEntity> entities) {
		userWalletRepository.saveAll(entities);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public WalletStatementEntity createWalletStatement(String entityId, EntityType entityType, Double amount, Double balance, String txnMode, String txnType,
			String txnDetail, String walletType, String remarks, String key) {
		WalletStatementEntity walletStatement = new WalletStatementEntity();
		walletStatement.setEntityId(entityId);
		walletStatement.setEntityType(entityType);
		walletStatement.setAmount(amount);
		walletStatement.setBalance(balance);
		walletStatement.setTxnMode(txnMode);
		walletStatement.setTxnType(txnType);
		walletStatement.setTxnDetail(txnDetail);
		walletStatement.setWalletType(walletType);
		walletStatement.setRemarks(remarks);
		if (StringUtils.isNotEmpty(key)) {
			walletStatement.setKey(String.format("%s|%s", key, walletType));
		}
		walletStatement = walletStatementService.save(walletStatement);
		_LOGGER.info(String.format("Created wallet statement for user %s of amount %s", entityId, amount));
		return walletStatement;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UserWalletEntity addOrDeduct(WalletTxnBean txnBean, String entityId, EntityType entityType, String key) {
		UserWalletEntity userWallet = findOrCreateByEntityId(entityId, entityType);
		if (key != null && walletStatementService.keyExists(key)) {
			_LOGGER.info("By passed add or deduct due to duplicate wallet statement key");
			return userWallet;
		}
		if (!isNullOrZero(txnBean.getAmount())) {
			BigDecimal amount = BigDecimal.valueOf(txnBean.getAmount());
			if (WalletType.COINS.equals(txnBean.getWalletType())) {
				userWallet.setLoyaltyCoins(BigDecimal.valueOf(userWallet.getLoyaltyCoins()).add(amount).doubleValue());
			} else {
				userWallet.setAmount(BigDecimal.valueOf(userWallet.getAmount()).add(amount).doubleValue());
			}
		}
		if (!isNullOrZero(txnBean.getHoldAmount())) {
			BigDecimal holdAmount = BigDecimal.valueOf(txnBean.getHoldAmount());
			userWallet.setAmount(userWallet.getAmount());
			userWallet.setWalletHold(BigDecimal.valueOf(userWallet.getWalletHold()).add(holdAmount).doubleValue());
		}
		// resetCreditLimitToMin(txnBean, userWallet);
		updateWalletStatements(entityId, txnBean, userWallet, key);
		updateWalletStatusAndOutstanding(userWallet, getWalletMetadataParamBean());
		giveDefaultCL(userWallet);
		userWallet = save(userWallet);
		if (userWallet.getEntityType().equals(EntityType.STORE) && key != null) {
			clientService.refreshCartInternal(userWallet);
		}
		return userWallet;
	}

	public void giveDefaultCL(UserWalletEntity userWallet) {
		BigDecimal minAmountForCL = BigDecimal.valueOf(ParamsUtils.getIntegerParam("MIN_AMOUNT_FOR_CREDIT_LIMIT", 100));
		Double defaultCL = Double.valueOf(ParamsUtils.getIntegerParam("DEFAULT_CREDIT_LIMIT", 2000));
		if (BigDecimal.valueOf(userWallet.getAmount()).subtract(BigDecimal.valueOf(userWallet.getWalletHold())).compareTo(minAmountForCL) >= 0
				&& userWallet.getCreditLimit().compareTo(defaultCL) < 0) {
			userWallet.setCreditLimit(defaultCL);
		}
	}

	private void resetCreditLimitToMin(WalletTxnBean txnBean, UserWalletEntity userWallet) {
		if (txnBean.getTxnType().equals(PaymentConstants.CO_TXN_TYPE)) {
			Double minAmountForCL = Double.valueOf(ParamsUtils.getIntegerParam("MIN_CREDIT_LIMIT", 0));
			userWallet.setCreditLimit(minAmountForCL);
		}
	}

	public List<UserWalletEntity> getUserWalletsByUserIds(List<String> userIds) {
		int limit = 5000;
		Map<String, Object> filters = new HashMap<>();
		filters.put("entityId", userIds);
		filters.put("entityType", EntityType.USER);
		return findAllRecords(filters, this.defaultSortMap(), limit);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateWalletStatements(String entityId, WalletTxnBean txnBean, UserWalletEntity userWallet, String key) {
		if (txnBean.getAmount() != null && (txnBean.getAmount().compareTo(0d) != 0 || txnBean.getTxnType().equals(PaymentConstants.FO_TXN_TYPE))) {
			createWalletStatement(entityId, userWallet.getEntityType(), Math.abs(txnBean.getAmount()), userWallet.getAmount(), getTxnMode(txnBean.getAmount()),
					txnBean.getTxnType(), txnBean.getTxnDetail(), fetchCoinsOrDefaultWalletType(txnBean).toString(), txnBean.getRemarks(), key);
		}
		if (!isNullOrZero(txnBean.getHoldAmount())) {
			createWalletStatement(entityId, userWallet.getEntityType(), Math.abs(txnBean.getHoldAmount()), userWallet.getWalletHold(),
					getTxnMode(txnBean.getHoldAmount()), txnBean.getTxnType(), txnBean.getTxnDetail(), WalletType.HOLD.toString(), txnBean.getRemarks(), key);
		}
	}

	private boolean isNullOrZero(Double amount) {
		return amount == null || amount.compareTo(0d) == 0;
	}

	private WalletType fetchCoinsOrDefaultWalletType(WalletTxnBean txnBean) {
		if (WalletType.COINS.equals(txnBean.getWalletType())) {
			return WalletType.COINS;
		}
		return WalletType.WALLET;
	}

	private String getTxnMode(Double amount) {
		String txnMode = null;
		if (amount.compareTo(0d) <= 0) {
			txnMode = WalletTxnMode.DEBIT.toString();
		} else {
			txnMode = WalletTxnMode.CREDIT.toString();
		}
		return txnMode;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UserWalletEntity findOrCreateByEntityId(String entityId, EntityType entityType) {
		UserWalletEntity userWallet = userWalletRepository.findByEntityId(entityId);
		if (userWallet == null) {
			if (entityType.equals(EntityType.STORE) && !clientService.getStoreDetails(Collections.singleton(entityId), null).stream().findFirst().isPresent()) {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Store does not exist"));
			}
			_LOGGER.info(String.format("findOrCreateByEntityId:: user wallet not found %s creating ", entityId));
			userWallet = UserWalletEntity.buildUserWalletEntity(entityId, entityType);
			userWallet = save(userWallet);
		}
		return userWallet;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UserWalletEntity updateWalletCreditLimit(UserWalletEntity userWallet, Double creditLimit) {
		_LOGGER.info(String.format("Updating credit limit for user %s with amount %.2f", userWallet.getEntityId(), creditLimit));
		userWallet.setCreditLimit(creditLimit);
		return save(userWallet);
	}

	@Override
	public Class<UserWalletEntity> getEntity() {
		return UserWalletEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return userWalletRepository;
	}

	public List<String> getStoresWithNegativeWalletBalance(EntityType entityType) {
		List<UserWalletEntity> entityList = userWalletRepository.findEntitiesWithBalanceLessThanAmount(entityType, 0d);
		if (CollectionUtils.isNotEmpty(entityList)) {
			return entityList.stream().map(UserWalletEntity::getEntityId).collect(Collectors.toList());
		}
		return null;
	}

	public List<String> getUsersBalanceLessThanAmount(EntityType entityType, Double amount) {
		List<UserWalletEntity> entityList = userWalletRepository.findEntitiesWithBalanceLessThanAmount(entityType, amount);
		if (CollectionUtils.isNotEmpty(entityList)) {
			return entityList.stream().map(UserWalletEntity::getEntityId).collect(Collectors.toList());
		}
		return null;
	}

	public CreditLimitChangeEntity getCreditLimitChange(String entityId) {
		return creditLimitChangeRepository.findByStoreIdAndDate(entityId, DateUtils.convertDateUtcToIst(new Date()));
	}

	public List<CreditLimitChangeEntity> getCreditLimitChangeEntities(List<String> entityIds, Date date) {
		return creditLimitChangeRepository.findByStoreIdsAndDate(entityIds, date);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveAllCreditLimitChanges(List<CreditLimitChangeEntity> creditLimitChangeEntities) {
		creditLimitChangeRepository.saveAll(creditLimitChangeEntities);
	}

	public void bulkUpdateCreditLimit(List<CreditLimitUploadBean> creditLimitUploadBeanList) {
		List<UserWalletEntity> userWallets = getUserWalletsByStoreIds(
				creditLimitUploadBeanList.stream().map(CreditLimitUploadBean::getStoreId).collect(Collectors.toList()));
		Map<String, Double> storeIdToCreditLimitMap = creditLimitUploadBeanList.stream()
				.collect(Collectors.toMap(CreditLimitUploadBean::getStoreId, CreditLimitUploadBean::getCreditLimit));
		List<String> storeIds = creditLimitUploadBeanList.stream().map(CreditLimitUploadBean::getStoreId).collect(Collectors.toList());
		Date date = creditLimitUploadBeanList.get(0).getDate();
		for (UserWalletEntity userWallet : userWallets) {
			userWallet.setCreditLimit(storeIdToCreditLimitMap.get(userWallet.getEntityId()));
		}
		buildAndSaveAllCreditLimitChangeEntity(getCreditLimitChangesMap(storeIds, date), creditLimitUploadBeanList);
		saveAll(userWallets);
	}

	public List<UserWalletEntity> getUserWalletsByStoreIds(List<String> storeIds) {
		int limit = 5000;
		Map<String, Object> filters = new HashMap<>();
		filters.put("entityId", storeIds);
		filters.put("entityType", EntityType.STORE);
		return findAllRecords(filters, this.defaultSortMap(), limit);
	}

	public void validateCreditLimitsOnUpload(CreditLimitUploadBean bean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {
			if (CollectionUtils.isNotEmpty(bean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

	public List<CreditLimitUploadBean> preProcessCreditLimitUpload(List<CreditLimitUploadBean> rawBeans) {
		List<UserWalletEntity> userWallets = getUserWalletsByStoreIds(rawBeans.stream().map(CreditLimitUploadBean::getStoreId).collect(Collectors.toList()));
		Map<String, UserWalletEntity> userWalletEntityMap = userWallets.stream().collect(Collectors.toMap(UserWalletEntity::getEntityId, Function.identity()));
		List<String> storeIds = rawBeans.stream().map(CreditLimitUploadBean::getStoreId).collect(Collectors.toList());
		Date date = DateUtils.convertDateUtcToIst(new Date());
		Map<String, CreditLimitChangeEntity> creditLimitChangeEntityMap = getCreditLimitChangesMap(storeIds, date);
		Set<String> storeIdSet = new HashSet<>();
		List<StoreOrderCount> storeOrders = clientService.getStoreOrderCount(storeIds);
		Double maxClAmount = Double.valueOf(ParamsUtils.getParam("MAX_CL_AMOUNT_FOR_STORE", "13000"));
		Set<String> exclusionSet = Arrays.asList(ParamsUtils.getParam("WALLET_CREDIT_LIMIT_UPDATE_EXCLUSION_LIST", "").split(",")).stream()
				.collect(Collectors.toSet());
		for (CreditLimitUploadBean creditLimitUploadBean : rawBeans) {
			if (creditLimitUploadBean.getStoreId() != null && !userWalletEntityMap.containsKey(creditLimitUploadBean.getStoreId())) {
				creditLimitUploadBean.getErrors()
						.add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Wallet for Store Id %s not found", creditLimitUploadBean.getStoreId()),
								"storeId"));
			} else if (storeIdSet.contains(creditLimitUploadBean.getStoreId())) {
				creditLimitUploadBean.getErrors()
						.add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Store Id %s is duplicated", creditLimitUploadBean.getStoreId()),
								"storeId"));
			} else {
				BigDecimal creditLimitChangeLimit = getCreditChangeLimitForUser();
				creditLimitUploadBean.setDate(date);
				if (validateAndUpdateCreditLimitChange(creditLimitChangeEntityMap.get(creditLimitUploadBean.getStoreId()), creditLimitUploadBean,
						userWalletEntityMap.get(creditLimitUploadBean.getStoreId()), creditLimitChangeLimit) && (SessionUtils.getAuthUserRoles()
						.contains("ADMIN") || validateWalletStatus(userWalletEntityMap.get(creditLimitUploadBean.getStoreId()),
						creditLimitUploadBean.getErrors(), exclusionSet))) {
					storeIdSet.add(creditLimitUploadBean.getStoreId());
				}
				if (validateOrderCountAndCL(storeOrders, creditLimitUploadBean.getStoreId(), creditLimitUploadBean,
						maxClAmount) && !SessionUtils.getAuthUserRoles().contains("ADMIN")) {
					creditLimitUploadBean.getErrors().add(ErrorBean.withError(Errors.INVALID_REQUEST,
							String.format("Store Id %s has less than 4 orders so can not update credit limit more than %s", creditLimitUploadBean.getStoreId(),
									maxClAmount), "storeId"));
				}
			}
		}
		return rawBeans;
	}

	public boolean validateOrderCountAndCL(List<StoreOrderCount> storeOrders, String storeId, CreditLimitUploadBean creditLimitUploadBean, Double maxClAmount) {
		Set<String> exclusionSet = Arrays.asList(ParamsUtils.getParam("WALLET_STATUS_UPDATE_EXCLUSION_LIST", "").split(",")).stream()
				.collect(Collectors.toSet());
		if (!exclusionSet.contains(storeId)) {
			Map<String, Long> storeOrderCountMap = storeOrders.stream().collect(Collectors.toMap(StoreOrderCount::getStoreId, StoreOrderCount::getCount));
			if ((!storeOrderCountMap.containsKey(storeId) || storeOrderCountMap.get(storeId) <= 3) && creditLimitUploadBean.getCreditLimit()
					.compareTo(maxClAmount) > 0) {
				return true;
			}
		}
		return false;
	}

	public Boolean validateAndUpdateCreditLimitChange(CreditLimitChangeEntity creditLimitChangeEntity, CreditLimitUploadBean creditLimitUploadBean,
			UserWalletEntity userWallet, BigDecimal creditLimitChangeLimit) {
		BigDecimal creditLimitDiff = BigDecimal.valueOf(creditLimitUploadBean.getCreditLimit()).subtract(BigDecimal.valueOf(userWallet.getCreditLimit()));
		if (creditLimitChangeEntity == null) {
			if (creditLimitDiff.compareTo(creditLimitChangeLimit) > 0) {
				creditLimitUploadBean.getErrors().add(new ErrorBean(Errors.AUTHORIZATION_EXCEPTION,
						String.format("User can only set credit limit upto %s for store id %s", creditLimitChangeLimit, userWallet.getEntityId())));
				return false;
			}
			creditLimitUploadBean.setChangeAmount(creditLimitDiff);
		} else {
			BigDecimal newChangeAmount = creditLimitDiff.add(creditLimitChangeEntity.getChangeAmount());
			if (newChangeAmount.compareTo(creditLimitChangeLimit) > 0) {
				creditLimitUploadBean.getErrors().add(new ErrorBean(Errors.AUTHORIZATION_EXCEPTION,
						String.format("Credit Limit already increased by %s for store id %s. Please contact your manager to increase it further",
								creditLimitChangeEntity.getChangeAmount(), userWallet.getEntityId())));
				return false;
			}
			creditLimitUploadBean.setChangeAmount(newChangeAmount);
		}
		return true;
	}

	public void buildAndSaveAllCreditLimitChangeEntity(Map<String, CreditLimitChangeEntity> existingCreditLimitChangeEntityMap,
			List<CreditLimitUploadBean> creditLimitUploadBeans) {
		List<CreditLimitChangeEntity> creditLimitChangeEntities = new ArrayList<>();
		for (CreditLimitUploadBean creditLimitUploadBean : creditLimitUploadBeans) {
			CreditLimitChangeEntity creditLimitChangeEntity = null;
			if (existingCreditLimitChangeEntityMap.containsKey(creditLimitUploadBean.getStoreId())) {
				creditLimitChangeEntity = existingCreditLimitChangeEntityMap.get(creditLimitUploadBean.getStoreId());
				creditLimitChangeEntity.setChangeAmount(creditLimitUploadBean.getChangeAmount());
			} else {
				creditLimitChangeEntity = buildCreditLimitChangeEntity(creditLimitUploadBean);
			}
			creditLimitChangeEntities.add(creditLimitChangeEntity);
		}
		saveAllCreditLimitChanges(creditLimitChangeEntities);
	}

	private CreditLimitChangeEntity buildCreditLimitChangeEntity(CreditLimitUploadBean creditLimitUploadBean) {
		CreditLimitChangeEntity creditLimitChangeEntity = CreditLimitChangeEntity.newInstance();
		creditLimitChangeEntity.setStoreId(creditLimitUploadBean.getStoreId());
		creditLimitChangeEntity.setDate(creditLimitUploadBean.getDate());
		creditLimitChangeEntity.setChangeAmount(creditLimitUploadBean.getChangeAmount());
		return creditLimitChangeEntity;
	}

	private Map<String, BigDecimal> getCreditLimitChangeRoleMap() {
		String deliveryChargesMapParam = ParamsUtils.getParam("CREDIT_LIMIT_CHANGE_ROLE_MAP");
		return Arrays.stream(deliveryChargesMapParam.split(",")).map(pair -> pair.split(":"))
				.collect(Collectors.toMap(pair -> pair[0], pair -> new BigDecimal(pair[1])));
	}

	private BigDecimal getCreditChangeLimit(Map<String, BigDecimal> creditLimitChangeRoleMap) {
		Set<String> userRoles = SessionUtils.getAuthUserRoles();
		String creditLimitRole = userRoles.stream().filter(creditLimitChangeRoleMap::containsKey).findFirst().orElse(null);
		if (creditLimitRole == null) {
			throw new ValidationException(new ErrorBean(Errors.AUTHORIZATION_EXCEPTION, "User not authorized to change Credit Limit."));
		}
		return creditLimitChangeRoleMap.get(creditLimitRole);
	}

	public BigDecimal getCreditChangeLimitForUser() {
		Map<String, BigDecimal> creditLimitChangeRoleMap = getCreditLimitChangeRoleMap();
		return getCreditChangeLimit(creditLimitChangeRoleMap);
	}

	public Map<String, CreditLimitChangeEntity> getCreditLimitChangesMap(List<String> storeIds, Date date) {
		List<CreditLimitChangeEntity> creditLimitChangeEntities = getCreditLimitChangeEntities(storeIds, date);
		return creditLimitChangeEntities.stream().collect(Collectors.toMap(CreditLimitChangeEntity::getStoreId, Function.identity()));
	}

	public Boolean validateWalletStatus(UserWalletEntity userWallet, List<ErrorBean> errorBeans, Set<String> exclusionSet) {
		if (exclusionSet.contains(userWallet.getEntityId())) {
			errorBeans.add(new ErrorBean(Errors.INVALID_REQUEST,
					String.format("Manual credit limit change for store id %s is not allowed", userWallet.getEntityId())));
			return false;
		}
		if (userWallet.getStatus().equals(WalletStatus.INACTIVE)) {
			errorBeans.add(new ErrorBean(Errors.INVALID_REQUEST, String.format("Unpaid minimum outstanding is %s for store id %s",
					userWallet.getMetadata() != null ? userWallet.getMetadata().getMinOutstanding() : null, userWallet.getEntityId())));
			return false;
		}
		return true;
	}

	private void updateWalletStatusAndOutstanding(UserWalletEntity userWallet, WalletMetadataParamsBean metadataParams) {
		try {
			if (userWallet.getEntityType().equals(EntityType.STORE)) {
				if (!metadataParams.getWalletUpdateExclusionSet().contains(userWallet.getEntityId())) {
					walletStatementService.updateUserWalletMetadataVariablesV2(userWallet, metadataParams);
					WalletStatus oldWalletStatus = userWallet.getStatus();
					userWallet.setStatus(userWallet.getMetadata().getMinOutstanding().compareTo(0d) > 0 ? WalletStatus.INACTIVE : WalletStatus.ACTIVE);
					if (oldWalletStatus == WalletStatus.INACTIVE && userWallet.getStatus() == WalletStatus.ACTIVE) {
						clientService.markStoreEligibleForTargetCashback(userWallet.getEntityId());
					}
				}
			}
		} catch (Exception e) {
			_LOGGER.info(String.format("Something went wrong while updateWalletStatusAndOutstanding : %s", e));
			throw new ServerException(ErrorBean.withError(Errors.SERVER_EXCEPTION, "Something went wrong while updating wallet status and outstanding", null));
		}
	}

	private WalletMetadataParamsBean getWalletMetadataParamBean() {
		return WalletMetadataParamsBean.builder().walletOutstandingTolerance(Double.valueOf(ParamsUtils.getParam("WALLET_OUTSTANDING_TOLERANCE", "0")))
				//				.walletOutstandingWindowDate(
				//						DateUtils.getDate(DateUtils.DATE_FMT_WITH_TIME, ParamsUtils.getParam("WALLET_OUTSTANDING_WINDOW_DATE", "2023-08-31 18:30:00")))
				.walletUpdateExclusionSet(
						Arrays.asList(ParamsUtils.getParam("WALLET_STATUS_UPDATE_EXCLUSION_LIST", "").split(",")).stream().collect(Collectors.toSet()))
				//				.walletOutstandingMinPayable(Double.valueOf(ParamsUtils.getParam("WALLET_OUTSTANDING_MIN_PAYABLE", "0")))
				.build();
	}

	public void redoAllWalletStatusAndMetadataCalculations() {
		List<UserWalletEntity> allActiveStoreWallets = userWalletRepository.findAllByActiveAndEntityType(1, EntityType.STORE);
		WalletMetadataParamsBean metadataParams = getWalletMetadataParamBean();
		for (UserWalletEntity userWallet : allActiveStoreWallets) {
			_LOGGER.debug("running updateWalletStatusAndOutstanding for " + userWallet.getEntityId());
			updateWalletStatusAndOutstanding(userWallet, metadataParams);
		}
		saveAll(allActiveStoreWallets);
	}

	public void resetCreditLimit() {
		List<UserWalletEntity> allActiveStoreWallets = userWalletRepository.findAllByActiveAndEntityType(1, EntityType.STORE);
		Double walletOutstandingCreditLimitTolerance = Double.valueOf(ParamsUtils.getParam("WALLET_OUTSTANDING_CREDIT_LIMIT_TOLERANCE", "500"));
		for (UserWalletEntity userWallet : allActiveStoreWallets) {
			Double userWalletOutstanding = Double.max(BigDecimal.valueOf(userWallet.getAmount()).subtract(BigDecimal.valueOf(userWallet.getWalletHold())).multiply(BigDecimal.valueOf(-1d)).doubleValue(), 0d);
			userWallet.setCreditLimit(walletOutstandingCreditLimitTolerance.compareTo(userWalletOutstanding) >= 0 ? 0d : userWalletOutstanding);
		}
		saveAll(allActiveStoreWallets);
	}

	public EntityDetailBean getEntityDetailV2() {
		EntityDetailBean entityDetail = new EntityDetailBean();
		entityDetail.setEntityId(SessionUtils.getAuthUserId().toString());
		String appType = SessionUtils.getAppId();
		Set<String> roles = SessionUtils.getAuthUserRoles();
		if (appType != null) {
			if (appType.equals(PaymentConstants.AppType.FOS.getValue()) && roles.contains(PaymentConstants.FOS_USER_ROLE)) {
				entityDetail.setEntityType(EntityType.AM);
			} else if (appType.equals(PaymentConstants.AppType.PARTNER.getValue()) && roles.contains(
					PaymentConstants.PARTNER_APP_USER_ROLE) && getStoreId() != null) {
				entityDetail.setEntityType(EntityType.STORE);
			} else if (appType.equals(PaymentConstants.AppType.CONSUMER.getValue())) {
				entityDetail.setEntityType(EntityType.USER);
			} else {
				throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Invalid app", null));
			}
		} else {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Invalid app", null));
		}
		return entityDetail;
	}

	public List<BulkAddOrDeductSheetBean> preProcessBulkAddOrDeductUpload(List<BulkAddOrDeductSheetBean> rawBeans) {
		return sanitizeBulkAddOrDeductUpload(rawBeans);
	}

	private List<BulkAddOrDeductSheetBean> sanitizeBulkAddOrDeductUpload(List<BulkAddOrDeductSheetBean> rawBeans) {
		if (CollectionUtils.isEmpty(rawBeans)) {
			return rawBeans;
		}
		Set<String> customerIds = new HashSet<>();
		boolean hasErrors = false;
		for (BulkAddOrDeductSheetBean bean : rawBeans) {
			String customerId = bean.getCustomerId();
			if (customerId == null || customerId.isEmpty()) {
				bean.getErrors().add(ErrorBean.withError("INVALID_ID", "Invalid customer id", "customer_id"));
				hasErrors = true;
			}
			if (!customerIds.add(customerId)) {
				bean.getErrors().add(ErrorBean.withError("DUPLICATE_CUSTOMER_ID", "Duplicate Customer Id", "customerId"));
				hasErrors = true;
			}
			if (bean.getAmount() == null || bean.getAmount().isNaN() || Objects.equals(bean.getAmount().toString(), "") || bean.getAmount() <= 0) {
				bean.getErrors().add(ErrorBean.withError("INVALID_AMOUNT", "Invalid amount", "amount"));
				hasErrors = true;
			}
		}
		if (hasErrors) {
			return rawBeans;
		}
		final int CHUNK_SIZE = 500;
		List<String> customerIdList = new ArrayList<>(customerIds);
		List<UserDetailsResponse> userDetails = new ArrayList<>();
		for (int i = 0; i < customerIdList.size(); i += CHUNK_SIZE) {
			int endIndex = Math.min(i + CHUNK_SIZE, customerIdList.size());
			List<String> chunk = customerIdList.subList(i, endIndex);
			try {
				List<UserDetailsResponse> chunkResponse = clientService.getUserDetailsByIds(chunk);
				if (chunkResponse != null) {
					userDetails.addAll(chunkResponse);
				}
			} catch (Exception e) {
				_LOGGER.error(String.format("Error processing chunk from index %d to %d: %s", i, endIndex, e.getMessage()), e);
				throw new ValidationException(
						ErrorBean.withError("PROCESSING_ERROR", "Error processing users. Our team is looking into it. Please retry.", null));
			}
		}
		if (CollectionUtils.isEmpty(userDetails)) {
			throw new ValidationException(ErrorBean.withError("UPLOAD_ERROR", "Data not found for users present in the sheet", null));
		}
		Map<String, UserDetailsResponse> userDetailsMap = userDetails.stream()
				.collect(Collectors.toMap(UserDetailsResponse::getId, Function.identity(), (first, second) -> first));
		rawBeans.stream().filter(bean -> !userDetailsMap.containsKey(bean.getCustomerId()))
				.forEach(bean -> bean.getErrors().add(ErrorBean.withError("CUSTOMER_NOT_FOUND", "Customer not found", "customerId")));
		return rawBeans;
	}

	public void validateBulkAddOrDeductDataOnUpload(BulkAddOrDeductUploadBean uploadBean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {
			if (Objects.isNull(uploadBean.getCustomerId())) {
				uploadBean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Customer Id not found", "customerId"));
			}
			if (Objects.isNull(uploadBean.getAmount())) {
				uploadBean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Amount not found", "amount"));
			}
			if (CollectionUtils.isNotEmpty(uploadBean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}
}
