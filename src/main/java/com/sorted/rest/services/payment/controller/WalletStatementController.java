package com.sorted.rest.services.payment.controller;

import com.sorted.rest.common.dbsupport.constants.Operation;
import com.sorted.rest.common.dbsupport.pagination.FilterCriteria;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.payment.beans.*;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.entity.WalletStatementEntity;
import com.sorted.rest.services.payment.services.LedgerService;
import com.sorted.rest.services.payment.services.UserWalletService;
import com.sorted.rest.services.payment.services.WalletStatementService;
import com.sorted.rest.services.payment.utils.LedgerPDFGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Api(value = "Wallet statement service", description = "Manage wallet statement details")
public class WalletStatementController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(WalletStatementController.class);

	@Autowired
	private BaseMapper<?, ?> walletStatementMapper;

	@Autowired
	private WalletStatementService walletStatementService;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private LedgerService ledgerService;

	@Autowired
	private LedgerPDFGenerator ledgerPDFGenerator;

	@Autowired
	private ClientService clientService;

	@ApiOperation(value = "wallet statement", nickname = "getWalletStatement")
	@GetMapping("/payments/walletStatement")
	public PageAndSortResult<WalletStatementBean> getWalletStatement(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "20") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		EntityDetailBean entityDetail = userWalletService.getEntityDetail();
		final Map<String, Object> params = new HashMap<>();
		params.put("entityId", entityDetail.getEntityId());
		params.put("walletType", WalletType.WALLET.toString());
		PageAndSortResult<WalletStatementEntity> walletStatementList = walletStatementService.getWalletStatement(pageSize, pageNo, params, sort);
		PageAndSortResult<WalletStatementBean> response = new PageAndSortResult<WalletStatementBean>();
		if (walletStatementList != null && walletStatementList.getData() != null) {
			response = prepareResponsePageData(walletStatementList, WalletStatementBean.class);
			addLabels(response.getData());
		}
		return response;
	}

	@ApiOperation(value = "wallet statement v2", nickname = "getWalletStatementV2")
	@GetMapping("/payments/walletStatement/v2")
	public PageAndSortResult<WalletStatementBean> getWalletStatementV2(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "20") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		EntityDetailBean entityDetail = userWalletService.getEntityDetail();
		final Map<String, Object> filters = new HashMap<>();
		filters.put("entityId", entityDetail.getEntityId());
		filters.put("walletType", WalletType.WALLET.toString());
		PageAndSortResult<WalletStatementEntity> walletStatementRes = walletStatementService.getWalletStatement(pageSize, pageNo, filters, sort);
		PageAndSortResult<WalletStatementBean> response = new PageAndSortResult<WalletStatementBean>();
		if (walletStatementRes != null && walletStatementRes.getData() != null) {
			response = prepareResponsePageData(walletStatementRes, WalletStatementBean.class);
			addLabels(response.getData());
		}
		return response;
	}

	private void addLabels(List<WalletStatementBean> walletStatement) {
		String txnTypeLabels = ParamsUtils.getParam("TXN_TYPE_LABELS");
		if (txnTypeLabels != null) {
			Map<String, String> txnTypeLabelMap = Arrays.asList(txnTypeLabels.split(",")).stream().map(s -> s.split(":"))
					.collect(Collectors.toMap(e -> e[0], e -> e[1]));
			updateLabels(walletStatement, txnTypeLabelMap);
		}
	}

	private void updateLabels(List<WalletStatementBean> walletStatement, Map<String, String> txnTypeLabelMap) {
		for (WalletStatementBean ws : walletStatement) {
			if (txnTypeLabelMap.containsKey(ws.getTxnType())) {
				ws.setTxnType(txnTypeLabelMap.get(ws.getTxnType()));
			}
			if (ws.getAdjustments() != null) {
				updateLabels(ws.getAdjustments(), txnTypeLabelMap);
			}
		}
	}

	@ApiOperation(value = "coin statement", nickname = "getCoinStatement")
	@GetMapping("/payments/coinStatement")
	public PageAndSortResult<WalletStatementBean> getCoinStatement(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "20") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		EntityDetailBean entityDetail = userWalletService.getEntityDetail();
		final Map<String, Object> params = new HashMap<>();
		params.put("entityId", entityDetail.getEntityId());
		params.put("walletType", "COINS");
		PageAndSortResult<WalletStatementEntity> walletStatementList = walletStatementService.getWalletStatement(pageSize, pageNo, params, sort);
		PageAndSortResult<WalletStatementBean> response = new PageAndSortResult<WalletStatementBean>();
		if (walletStatementList != null && walletStatementList.getData() != null) {
			response = prepareResponsePageData(walletStatementList, WalletStatementBean.class);
		}
		return response;
	}

	@ApiOperation(value = "wallet statement", nickname = "getWalletStatement")
	@GetMapping("/payments/admin/walletStatement/{entityType}/{entityId}")
	public PageAndSortResult<WalletStatementBean> getWalletStatementAdmin(@PathVariable EntityType entityType, @PathVariable String entityId,
			@RequestParam(defaultValue = "1") Integer pageNo, @RequestParam(defaultValue = "20") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);

		EntityDetailBean entityDetail = new EntityDetailBean();
		entityDetail.setEntityId(entityId);
		if (Objects.equals(entityType, EntityType.STORE)) {
			entityDetail.setEntityType(PaymentConstants.EntityType.STORE);
		} else {
			entityDetail.setEntityType(PaymentConstants.EntityType.USER);
		}
		final Map<String, Object> params = new HashMap<>();
		params.put("entityId", entityDetail.getEntityId());
		params.put("walletType", WalletType.WALLET.toString());
		PageAndSortResult<WalletStatementEntity> walletStatementList = walletStatementService.getWalletStatement(pageSize, pageNo, params, sort);
		PageAndSortResult<WalletStatementBean> response = new PageAndSortResult<WalletStatementBean>();
		if (walletStatementList != null && walletStatementList.getData() != null) {
			response = prepareResponsePageData(walletStatementList, WalletStatementBean.class);
		}
		return response;
	}

	@ApiOperation(value = "wallet statement based on txn detail", nickname = "getWalletStatementByTxnDetail")
	@GetMapping("/payments/walletStatement/{txnDetail}")
	public ResponseEntity<List<WalletStatementBean>> getWalletStatementByTxnDetail(@PathVariable String txnDetail) {
		List<WalletStatementEntity> walletStatement = walletStatementService.findByTxnDetail(txnDetail);
		if (CollectionUtils.isEmpty(walletStatement)) {
			_LOGGER.info(String.format("getWalletStatementByTxnDetail:: No Data Found for txnDetail: %s", txnDetail));
			return ResponseEntity.ok(new ArrayList<>());
		}
		List<WalletStatementBean> response = getMapper().mapAsList(walletStatement, WalletStatementBean.class);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "wallet statement details ", nickname = "getLedgerForBackoffice")
	@GetMapping("/payments/walletStatement/{entityType}/{entityId}/ledger")
	public ResponseEntity<LedgerResponse> downloadLedger(@PathVariable String entityId, @RequestParam java.sql.Date from, @RequestParam java.sql.Date to) {
		_LOGGER.info(String.format("downloadLedger : entityId = %s", entityId));
		LedgerResponse response = generateLedger(entityId, from, to);
		return ResponseEntity.ok(response);
	}

	private LedgerResponse generateLedger(String entityId, Date from, Date to) {
		List<WalletStatementEntity> walletStatement = walletStatementService.getWalletStatementsForLedger(entityId, DateUtils.addMinutes(from, -330),
				DateUtils.addMinutes(to, -330));
		Set<String> displayOrderIds = walletStatement.stream().filter(w -> w.getTxnDetail().startsWith("OF-")).map(WalletStatementEntity::getTxnDetail)
				.collect(Collectors.toSet());
		StoreDataResponse storeDetails = getStoreDetails(entityId);
		Map<String, OrderDetailResponse> orderMap = getOrderDetails(displayOrderIds);
		List<LedgerTxnBean> txns = getLedgerTxnBean(walletStatement);
		LedgerDataBean ledgerDataBean = ledgerService.buildLedgerData(txns, storeDetails, from, to, orderMap);
		ledgerPDFGenerator.generatePdfReport(ledgerDataBean);
		ledgerService.uploadLedger(ledgerDataBean);
		LedgerResponse response = new LedgerResponse();
		response.setLedgerUrl(ledgerDataBean.getLedgerUrl());
		return response;
	}

	private List<LedgerTxnBean> getLedgerTxnBean(List<WalletStatementEntity> walletStatement) {
		List<WalletStatementBean> walletStatementBeans = getMapper().mapAsList(walletStatement, WalletStatementBean.class);
		addLabels(walletStatementBeans);
		List<LedgerTxnBean> txns = getMapper().mapAsList(walletStatementBeans, LedgerTxnBean.class);
		return txns;
	}

	@ApiOperation(value = "wallet statement details", nickname = "getLedgerFromSession")
	@GetMapping("/payments/walletStatement/ledger")
	public ResponseEntity<LedgerResponse> downloadLedgerApp(@RequestParam java.sql.Date from, @RequestParam java.sql.Date to) {
		String storeId = SessionUtils.getStoreId();
		_LOGGER.info(String.format("GET STORE ORDERS : storeId = %s", storeId));
		LedgerResponse response = generateLedger(storeId, from, to);
		return ResponseEntity.ok(response);
	}

	public StoreDataResponse getStoreDetails(String storeId) {
		return clientService.getStoreDataFromId(storeId);
	}

	public Map<String, OrderDetailResponse> getOrderDetails(Set<String> displayOrderIds) {
		List<OrderDetailResponse> response = clientService.getOrderDetails(displayOrderIds);
		Map<String, OrderDetailResponse> orderMap = response.stream().collect(Collectors.toMap(OrderDetailResponse::getDisplayOrderId, Function.identity()));
		return orderMap;
	}

	@ApiOperation(value = "wallet statement v3", nickname = "getWalletStatementV3")
	@GetMapping("/payments/walletStatement/v3")
	public PageAndSortResult<WalletStatementBean> getWalletStatementV3(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize) {
		Map<String, PageAndSortRequest.SortDirection> sort = new LinkedHashMap<>();
		sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		EntityDetailBean entityDetail = userWalletService.getEntityDetailV2();
		final Map<String, Object> params = new HashMap<>();
		params.put("entityId", entityDetail.getEntityId());
		params.put("entityType", entityDetail.getEntityType());
		params.put("walletType", WalletType.WALLET.toString());
		String filterDate = ParamsUtils.getParam("WALLET_STATEMENT_FILTER_DATE", "2024-03-01");
		params.put("createdAt", new FilterCriteria("createdAt", DateUtils.getDate(DateUtils.SHORT_DATE_FMT, filterDate), Operation.GTE));
		PageAndSortResult<WalletStatementEntity> walletStatementList = walletStatementService.getWalletStatement(pageSize, pageNo, params, sort);
		PageAndSortResult<WalletStatementBean> response = new PageAndSortResult<WalletStatementBean>();
		if (walletStatementList != null && walletStatementList.getData() != null) {
			response = prepareResponsePageData(walletStatementList, WalletStatementBean.class);
		}
		return response;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return walletStatementMapper;
	}
}
