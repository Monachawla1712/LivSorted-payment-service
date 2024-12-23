package com.sorted.rest.services.payment.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.*;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.common.upload.csv.CSVBulkRequest;
import com.sorted.rest.services.common.upload.csv.CsvUploadResult;
import com.sorted.rest.services.common.upload.csv.CsvUtils;
import com.sorted.rest.services.params.entity.ParamEntity;
import com.sorted.rest.services.params.service.ParamService;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.entity.CreditLimitChangeEntity;
import com.sorted.rest.services.payment.entity.UserWalletEntity;
import com.sorted.rest.services.payment.services.UserWalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Api(value = "User Wallet Service", description = "Manage User wallet details")
public class UserWalletController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(UserWalletController.class);

	@Autowired
	private BaseMapper<?, ?> userWalletMapper;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private ParamService paramService;

	@ApiOperation(value = "Get user wallet detail from Portal.", nickname = "getUserWallet")
	@GetMapping("/payments/wallet/{entityType}/{entityId}")
	public ResponseEntity<UserWalletBean> getUserWallet(@PathVariable String entityId, @PathVariable EntityType entityType) {
		UserWalletEntity entity = userWalletService.getUserWallet(entityId, entityType);
		UserWalletBean walletBean = getMapper().mapSrcToDest(entity, UserWalletBean.newInstance());
		deductHoldAmount(walletBean);
		return ResponseEntity.ok(walletBean);
	}

	private void deductHoldAmount(UserWalletBean entity) {
		BigDecimal finalAmount = BigDecimal.valueOf(entity.getAmount()).subtract(BigDecimal.valueOf(entity.getWalletHold()));
		entity.setAmount(finalAmount.doubleValue());
	}

	@ApiOperation(value = "Get user wallet detail from consumer App", nickname = "getUserWallet")
	@GetMapping("/payments/wallet")
	public ResponseEntity<UserWalletBean> getUserWallet() {
		EntityDetailBean entityDetail = userWalletService.getEntityDetail();
		UserWalletEntity entity = userWalletService.getUserWallet(entityDetail.getEntityId(), entityDetail.getEntityType());
		UserWalletBean walletBean = getMapper().mapSrcToDest(entity, UserWalletBean.newInstance());
		deductHoldAmount(walletBean);
		return ResponseEntity.ok(walletBean);
	}

	@ApiOperation(value = "Add or Deduct money from wallet", nickname = "addOrDeductMoney")
	@PostMapping("payments/wallet/addOrDeduct/{entityType}/{entityId}")
	public ResponseEntity<UserWalletBean> addOrDeduct(@PathVariable String entityId, @PathVariable EntityType entityType, @RequestBody WalletTxnBean request,
			@RequestParam(required = false) String key) {
		validateAmount(request);
		UserWalletEntity entity = userWalletService.addOrDeduct(request, entityId, entityType, key);
		UserWalletBean walletBean = getMapper().mapSrcToDest(entity, UserWalletBean.newInstance());
		deductHoldAmount(walletBean);
		return ResponseEntity.ok(walletBean);
	}

	private void validateAmount(WalletTxnBean request) {
		if ((request.getAmount() != null && (request.getAmount().compareTo(0d) != 0 || request.getTxnType()
				.equals(PaymentConstants.FO_TXN_TYPE))) || (request.getHoldAmount() != null && request.getHoldAmount().compareTo(0d) != 0)) {
			return;
		}
		throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "The transaction of amount zero is not allowed", "amount"));
	}

	@ApiOperation(value = "Set credit limit in wallet", nickname = "creditLimit")
	@PatchMapping("/payments/wallet/creditLimit/{entityType}/{entityId}")
	@Transactional(propagation = Propagation.REQUIRED)
	public ResponseEntity<UserWalletBean> updateCreditLimit(@PathVariable String entityId, @PathVariable EntityType entityType,
			@RequestBody @Valid UserWalletRequestBean request) {
		if (entityId == null) {
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "EntityId can't be null"));
		}
		UserWalletEntity wallet = userWalletService.getUserWallet(entityId, entityType);
		if (Objects.equals(entityType, EntityType.STORE)) {
			Date date = DateUtils.convertDateUtcToIst(new Date());
			BigDecimal creditLimitChangeLimit = userWalletService.getCreditChangeLimitForUser();
			CreditLimitUploadBean creditLimitUploadBean = CreditLimitUploadBean.newInstance();
			creditLimitUploadBean.setStoreId(entityId);
			creditLimitUploadBean.setCreditLimit(request.getCreditLimit());
			creditLimitUploadBean.setDate(date);
			Map<String, CreditLimitChangeEntity> existingCreditLimitChangeEntityMap = userWalletService.getCreditLimitChangesMap(
					Arrays.asList(creditLimitUploadBean.getStoreId()), date);
			Set<String> exclusionSet = Arrays.asList(ParamsUtils.getParam("WALLET_CREDIT_LIMIT_UPDATE_EXCLUSION_LIST", "").split(",")).stream()
					.collect(Collectors.toSet());
			if (!userWalletService.validateAndUpdateCreditLimitChange(existingCreditLimitChangeEntityMap.get(entityId), creditLimitUploadBean, wallet,
					creditLimitChangeLimit) || !(SessionUtils.getAuthUserRoles().contains("ADMIN") || userWalletService.validateWalletStatus(wallet,
					creditLimitUploadBean.getErrors(), exclusionSet))) {
				throw new ValidationException(creditLimitUploadBean.getErrors().get(0));
			}
			List<StoreOrderCount> storeOrders = clientService.getStoreOrderCount(Collections.singletonList(entityId));
			Double maxClAmount = Double.valueOf(ParamsUtils.getParam("MAX_CL_AMOUNT_FOR_STORE", "13000"));
			if (userWalletService.validateOrderCountAndCL(storeOrders, entityId, creditLimitUploadBean, maxClAmount) && !SessionUtils.getAuthUserRoles()
					.contains("ADMIN")) {
				throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST,
						String.format("Store Id %s has less than 4 orders so can not update credit limit more than %s", creditLimitUploadBean.getStoreId(),
								maxClAmount), "storeId"));
			}
			userWalletService.buildAndSaveAllCreditLimitChangeEntity(existingCreditLimitChangeEntityMap, Arrays.asList(creditLimitUploadBean));
			wallet = userWalletService.updateWalletCreditLimit(wallet, request.getCreditLimit());
		} else if (Objects.equals(entityType, EntityType.USER)) {
			wallet = userWalletService.updateWalletCreditLimit(wallet, request.getCreditLimit());
		}
		UserWalletBean walletBean = getMapper().mapSrcToDest(wallet, UserWalletBean.newInstance());
		deductHoldAmount(walletBean);
		return ResponseEntity.ok(walletBean);
	}

	@GetMapping("/payments/wallet/negative/stores")
	public List<String> getStoresWithNegativeWalletBalance() {
		return userWalletService.getStoresWithNegativeWalletBalance(EntityType.STORE);
	}

	@ApiOperation(value = "Credit Limit Upload", nickname = "creditLimitUpload")
	@PostMapping("/payments/wallet/creditLimit/backoffice/upload")
	public CsvUploadResult<CreditLimitUploadBean> uploadCreditLimitSheet(@RequestParam MultipartFile file) {
		final int maxAllowedRows = 5000;
		final String module = "credit-limit";
		List<CreditLimitUploadBean> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, CreditLimitUploadBean.newInstance());
		List<CreditLimitUploadBean> response = userWalletService.preProcessCreditLimitUpload(rawBeans);
		CsvUploadResult<CreditLimitUploadBean> csvUploadResult = validateCreditLimitUpload(response);
		csvUploadResult.setHeaderMapping(response.get(0).getHeaderMapping());
		CsvUtils.saveBulkRequestData(userWalletService.getUserId(), module, csvUploadResult);
		return csvUploadResult;
	}

	private CsvUploadResult<CreditLimitUploadBean> validateCreditLimitUpload(List<CreditLimitUploadBean> beans) {
		final CsvUploadResult<CreditLimitUploadBean> result = new CsvUploadResult<>();
		if (CollectionUtils.isNotEmpty(beans)) {
			for (CreditLimitUploadBean bean : beans) {
				try {
					org.springframework.validation.Errors errors = getSpringErrors(bean);
					userWalletService.validateCreditLimitsOnUpload(bean, errors);
					checkError(errors);
					result.addSuccessRow(bean);
				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						final List<ErrorBean> errors = e instanceof ValidationException ?
								BeanValidationUtils.prepareValidationResponse((ValidationException) e).getErrors() :
								Collections.singletonList(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
					}
					result.addFailedRow(bean);
				}
			}
		}
		return result;
	}

	@ApiOperation(value = "save credit limit sheet upload", nickname = "saveCreditLimitUpload")
	@PostMapping(path = "/payments/wallet/creditLimit/backoffice/upload/save")
	@ResponseStatus(HttpStatus.OK)
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveCreditLimitSheetUpload(@RequestParam String key, @RequestParam(required = false) Integer cancel) {
		final boolean cleanup = cancel != null;
		if (cleanup) {
			cancelUpload(key);
		} else {
			saveBulkCreditLimitUpload(key);
		}
	}

	public void cancelUpload(String key) {
		final int deleteCount = CsvUtils.cancelUpload(key);
		if (deleteCount <= 0) {
			throw new ValidationException(ErrorBean.withError("UPLOAD_CANCEL_ERROR", "Unable to cancel bulk upload request.", null));
		}
	}

	private void saveBulkCreditLimitUpload(String key) {
		final CSVBulkRequest<CreditLimitUploadBean> uploadedData = CsvUtils.getBulkRequestData(key, CreditLimitUploadBean.class);
		if (uploadedData != null && CollectionUtils.isNotEmpty(uploadedData.getData())) {
			List<CreditLimitUploadBean> creditLimitUploadBeanList = uploadedData.getData();
			userWalletService.bulkUpdateCreditLimit(creditLimitUploadBeanList);
			CsvUtils.markUploadProcessed(key);
		} else {
			throw new ValidationException(ErrorBean.withError("UPLOAD_ERROR", "Uploaded data not found or it is expired.", null));
		}
	}

	@ApiOperation(value = "fetch internal bulk user wallet detail.", nickname = "fetchBulkUserWallets")
	@PostMapping("/payments/wallet/internal")
	public ResponseEntity<List<UserWalletBean>> fetchWallets(@RequestBody @Valid BulkFetchWalletRequest request) {
		List<UserWalletEntity> userWallets = userWalletService.getUserWalletsByStoreIds(request.getStoreIds());
		List<UserWalletBean> walletBeans = getMapper().mapAsList(userWallets, UserWalletBean.class);
		for (UserWalletBean userWallet : walletBeans) {
			deductHoldAmount(userWallet);
		}
		return ResponseEntity.ok(walletBeans);
	}

	@ApiOperation(value = "update wallet metadata params", nickname = "updateWalletMetadataParams")
	@PostMapping("payments/wallet/metadata-params")
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateWalletMetadataParams(@RequestBody @Valid WalletMetadataParamsBean request) {
		Boolean isUpdated = false;
		if (request.getWalletOutstandingTolerance() != null) {
			ParamEntity walletOutstandingToleranceParam = paramService.findByParamKey("WALLET_OUTSTANDING_TOLERANCE");
			if (walletOutstandingToleranceParam != null && !request.getWalletOutstandingTolerance().toString()
					.equals(walletOutstandingToleranceParam.getParamValue())) {
				isUpdated = true;
				walletOutstandingToleranceParam.setParamValue(request.getWalletOutstandingTolerance().toString());
				paramService.save(walletOutstandingToleranceParam);
			}
		}
		if (request.getWalletOutstandingWindowDate() != null) {
			String walletOutstandingWindowDate = DateUtils.toString(DateUtils.DATE_FMT_WITH_TIME,
					DateUtils.addMinutes(request.getWalletOutstandingWindowDate(), -330));
			ParamEntity walletOutstandingWindowDateParam = paramService.findByParamKey("WALLET_OUTSTANDING_WINDOW_DATE");
			if (walletOutstandingWindowDateParam != null && !walletOutstandingWindowDate.equals(walletOutstandingWindowDateParam.getParamValue())) {
				isUpdated = true;
				walletOutstandingWindowDateParam.setParamValue(walletOutstandingWindowDate);
				paramService.save(walletOutstandingWindowDateParam);
			}
		}
		if (request.getWalletOutstandingMinPayable() != null) {
			ParamEntity walletOutstandingMinPayable = paramService.findByParamKey("WALLET_OUTSTANDING_MIN_PAYABLE");
			if (walletOutstandingMinPayable != null && !request.getWalletOutstandingMinPayable().toString()
					.equals(walletOutstandingMinPayable.getParamValue())) {
				isUpdated = true;
				walletOutstandingMinPayable.setParamValue(request.getWalletOutstandingMinPayable().toString());
				paramService.save(walletOutstandingMinPayable);
			}
		}
		if (isUpdated) {
			userWalletService.redoAllWalletStatusAndMetadataCalculations();
		}
	}

	@ApiOperation(value = "reset credit limit", nickname = "resetCreditLimit")
	@PostMapping("payments/wallet/creditLimit/reset")
	@Transactional(propagation = Propagation.REQUIRED)
	public void resetCreditLimit() {
		_LOGGER.debug("running resetCreditLimit");
		userWalletService.resetCreditLimit();
	}

	@ApiOperation(value = "fetch internal bulk user wallet detail.", nickname = "fetchBulkUserWallets")
	@PostMapping("/payments/wallet/consumer/internal")
	public ResponseEntity<List<UserWalletBean>> fetchConsumerWalletsInternal(@RequestBody @Valid BulkConsumerWalletRequest request) {
		List<UserWalletEntity> userWallets = userWalletService.getUserWalletsByUserIds(request.getUserIds());
		List<UserWalletBean> walletBeans = getMapper().mapAsList(userWallets, UserWalletBean.class);
		for (UserWalletBean userWallet : walletBeans) {
			deductHoldAmount(userWallet);
		}
		return ResponseEntity.ok(walletBeans);
	}

	@ApiOperation(value = "Get user wallet detail v2", nickname = "getUserWallet")
	@GetMapping("/payments/wallet/v2")
	public ResponseEntity<UserWalletBean> getConsumerWallet() {
		EntityDetailBean entityDetail = userWalletService.getEntityDetailV2();
		UserWalletEntity entity = userWalletService.getUserWallet(entityDetail.getEntityId(), entityDetail.getEntityType());
		UserWalletBean walletBean = getMapper().mapSrcToDest(entity, UserWalletBean.newInstance());
		deductHoldAmount(walletBean);
		return ResponseEntity.ok(walletBean);
	}

	@PostMapping(value = "/payments/wallet/bulk-add-or-deduct/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CsvUploadResult<BulkAddOrDeductUploadBean> uploadBulkAddOrDeduct(@RequestParam MultipartFile file, @RequestParam String txnType,
			@RequestParam String txnDetail) {
		final int maxAllowedRows = 5000;
		final String module = "bulk-add-or-deduct";
		if (txnType == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "txnType cannot be null", "txnType"));
		}
		if (txnDetail == null) {
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "txnDetail cannot be null", "txnDetail"));
		}
		List<BulkAddOrDeductSheetBean> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, BulkAddOrDeductSheetBean.newInstance());
		List<BulkAddOrDeductSheetBean> preProcessedBeans = userWalletService.preProcessBulkAddOrDeductUpload(rawBeans);
		_LOGGER.info("Validating bulk add or deduct upload data");
		CsvUploadResult<BulkAddOrDeductUploadBean> result = validateBulkAddOrDeductUpload(preProcessedBeans, txnType, txnDetail);
		result.setHeaderMapping(preProcessedBeans.get(0).getHeaderMapping());
		CsvUtils.saveBulkRequestData(SessionUtils.getAuthUserId(), module, result);
		_LOGGER.info("saving bulk add or deduct upload data");
		return result;
	}

	@PostMapping("/payments/wallet/bulk-add-or-deduct/upload/save")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void saveBulkAddOrDeduct(@RequestParam String key, @RequestParam(required = false) Integer cancel) {
		final boolean cleanup = cancel != null;
		if (cleanup) {
			cancelUpload(key);
		} else {
			_LOGGER.info(String.format("Processing bulk add or deduct for key::%s", key));
			saveBulkAddOrDeductData(key);
		}
	}

	private void saveBulkAddOrDeductData(String key) {
		try {
			final CSVBulkRequest<BulkAddOrDeductUploadBean> uploadedData = CsvUtils.getBulkRequestData(key, BulkAddOrDeductUploadBean.class);
			if (uploadedData == null || CollectionUtils.isEmpty(uploadedData.getData())) {
				throw new ValidationException(ErrorBean.withError("UPLOAD_ERROR", "Uploaded data not found or it is expired.", null));
			}
			List<BulkAddOrDeductUploadBean> data = uploadedData.getData();
			for (BulkAddOrDeductUploadBean d : data) {
				WalletTxnBean bean = prepareWalletTxnBean(d);
				String walletKey = String.format("%s|%s", d.getCustomerId(), key);
				userWalletService.addOrDeduct(bean, d.getCustomerId(), EntityType.USER, walletKey);
			}
			CsvUtils.markUploadProcessed(key);
		} catch (Exception e) {
			_LOGGER.error("Something went wrong while processing bulk add/deduct data.", e);
			throw new ServerException(ErrorBean.withError("ERROR", "Something went wrong while processing bulk data. Our team is on it. Please retry.", null));
		}
	}

	private WalletTxnBean prepareWalletTxnBean(BulkAddOrDeductUploadBean d) {
		WalletTxnBean bean = new WalletTxnBean();
		bean.setAmount(d.getAmount());
		bean.setTxnType(d.getTxnType());
		bean.setTxnDetail(d.getTxnDetail());
		bean.setWalletType(PaymentConstants.WalletType.WALLET);
		bean.setHoldAmount(null);
		return bean;
	}

	private CsvUploadResult<BulkAddOrDeductUploadBean> validateBulkAddOrDeductUpload(List<BulkAddOrDeductSheetBean> preProcessedBeans, String txnType,
			String txnDetail) {
		CsvUploadResult<BulkAddOrDeductUploadBean> result = new CsvUploadResult<>();
		if (CollectionUtils.isNotEmpty(preProcessedBeans)) {
			for (BulkAddOrDeductSheetBean bean : preProcessedBeans) {
				BulkAddOrDeductUploadBean uploadBean = new BulkAddOrDeductUploadBean(txnType, txnDetail);
				uploadBean.setCustomerId(bean.getCustomerId());
				uploadBean.setAmount(bean.getAmount());
				uploadBean.setErrors(bean.getErrors());
				try {
					org.springframework.validation.Errors errors = getSpringErrors(uploadBean);
					userWalletService.validateBulkAddOrDeductDataOnUpload(uploadBean, errors);
					checkError(errors);
					result.addSuccessRow(uploadBean);
				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						_LOGGER.error(e.getMessage(), e);
						final List<ErrorBean> errors = e instanceof ValidationException ? BeanValidationUtils.prepareValidationResponse((ValidationException) e)
								.getErrors() : List.of(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
						_LOGGER.info("Bulk Add or Deduct uploaded data is having error =>" + errors.toString());
					}
					result.addFailedRow(uploadBean);
				}
			}
		}
		return result;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return userWalletMapper;
	}
}
