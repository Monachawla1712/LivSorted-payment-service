package com.sorted.rest.services.payment.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest.SortDirection;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.payment.beans.WalletMetadataParamsBean;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletTxnMode;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.entity.UserWalletEntity;
import com.sorted.rest.services.payment.entity.WalletStatementEntity;
import com.sorted.rest.services.payment.repository.WalletStatementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WalletStatementService implements BaseService<WalletStatementEntity> {

	AppLogger _LOGGER = LoggingManager.getLogger(WalletStatementService.class);

	@Autowired
	private WalletStatementRepository walletStatementRepository;

	@Transactional(propagation = Propagation.REQUIRED)
	public WalletStatementEntity save(WalletStatementEntity entity) {
		return walletStatementRepository.save(entity);
	}

	public PageAndSortResult<WalletStatementEntity> getWalletStatement(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, PageAndSortRequest.SortDirection> sort) {
		PageAndSortResult<WalletStatementEntity> walletStatementList = null;
		try {
			walletStatementList = findPagedRecords(filters, sort, pageSize, pageNo);
		} catch (Exception e) {
			_LOGGER.error(e);
			throw new ValidationException(ErrorBean.withError("FETCH_ERROR", e.getMessage(), null));
		}
		return walletStatementList;
	}

	public List<WalletStatementEntity> findByTxnDetail(String txnDetail) {
		_LOGGER.info(String.format("Fetching wallet statement for txnDetail : %s", txnDetail));
		Map<String, Object> filters = new HashMap<>();
		filters.put("txnDetail", txnDetail);
		filters.put("walletType", PaymentConstants.WalletType.WALLET.toString());
		List<WalletStatementEntity> walletStatement = findAllRecords(filters);
		return walletStatement;
	}

	@Override
	public Class<WalletStatementEntity> getEntity() {
		return WalletStatementEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return walletStatementRepository;
	}

	public PageAndSortResult<WalletStatementEntity> getWalletStatementV2(Integer pageSize, Integer pageNo, Map<String, Object> filters,
			Map<String, SortDirection> sort) {
		PageAndSortResult<WalletStatementEntity> walletStatement = findPagedRecords(filters, sort, pageNo * pageSize, 1);
		Set<String> currPageTxnDetail = null;
		int prevPageSize = (pageNo - 1) * pageSize;
		if (pageNo > 1) {
			if (walletStatement != null && walletStatement.getData() != null && walletStatement.getData().size() > prevPageSize) {
				List<WalletStatementEntity> l1 = walletStatement.getData().subList(0, prevPageSize);
				Set<String> prevPageTxnDetail = l1.stream().map(l -> l.getTxnDetail()).collect(Collectors.toSet());
				currPageTxnDetail = walletStatement.getData().subList(prevPageSize, walletStatement.getData().size()).stream()
						.filter(l -> !prevPageTxnDetail.contains(l.getTxnDetail())).map(l -> l.getTxnDetail()).collect(Collectors.toSet());
			} else {
				walletStatement.setData(null);
			}
			walletStatement.setPageNo(pageNo);
			walletStatement.setPageSize(pageSize);
			walletStatement.setPages(pageSize > 0 ?
					walletStatement.getTotal() % pageSize > 0 ? walletStatement.getTotal() / pageSize + 1 : walletStatement.getTotal() / pageSize :
					0);
		} else {
			currPageTxnDetail = walletStatement.getData().stream().map(l -> l.getTxnDetail()).collect(Collectors.toSet());
		}
		if (currPageTxnDetail != null && !currPageTxnDetail.isEmpty()) {
			filters.put("txnDetail", currPageTxnDetail);
			List<WalletStatementEntity> currePageStatement = findAllRecords(filters, sort);
			walletStatement.setData(currePageStatement);
		}
		return walletStatement;
	}

	public List<WalletStatementEntity> getWalletStatementsForLedger(String storeId, Date from, Date to) {
		_LOGGER.info(String.format("getWalletStatementsForLedger :- storeId : %s, from : %s, to : %s", storeId, from, DateUtils.addDays(to, 1)));
		return walletStatementRepository.findByWalletStatement(storeId, from, DateUtils.addDays(to, 1));
	}

	public Boolean keyExists(String key) {
		return walletStatementRepository.keyExists(key);
	}

	/*public void updateUserWalletMetadataVariables(UserWalletEntity userWallet, WalletMetadataParamsBean metadataParams) {
		List<WalletStatementEntity> lastOrderTransactions = walletStatementRepository.getLastOrderTransactions(userWallet.getEntityId()).stream()
				.sorted(Comparator.comparing(WalletStatementEntity::getId)).collect(Collectors.toList());
		userWallet.getMetadata().setAfterOrderTopup(getAfterLastOrderTopup(lastOrderTransactions));
		userWallet.getMetadata().setLastOrderCost(getLastOrderCost(lastOrderTransactions));
		userWallet.getMetadata().setLastOrderRefund(getLastOrderRefund(lastOrderTransactions));
		userWallet.getMetadata().setWindowOutstanding(
				Double.max(getBeforeTransactionWindowOutstanding(lastOrderTransactions, metadataParams.getWalletOutstandingWindowDate()), 0d));
		// last order outstanding as shown on FE
		BigDecimal lastOrderOutstanding = BigDecimal.valueOf(userWallet.getMetadata().getLastOrderCost())
				.subtract(BigDecimal.valueOf(userWallet.getMetadata().getLastOrderRefund()))
				.subtract(BigDecimal.valueOf(userWallet.getMetadata().getAfterOrderTopup()));
		// total outstanding as shown on FE
		Double userWalletOutstanding = Double.max(BigDecimal.valueOf(userWallet.getAmount()).multiply(BigDecimal.valueOf(-1d))
				.subtract(BigDecimal.valueOf(metadataParams.getWalletOutstandingTolerance())).doubleValue(), 0d);
		// adding window outstanding to clear more parts of wallet outstanding
		BigDecimal lastOrderPayable = BigDecimal.valueOf(userWallet.getMetadata().getWindowOutstanding()).add(lastOrderOutstanding);
		// adding wallet outstanding min payable to clear more parts of wallet outstanding
		lastOrderPayable = lastOrderPayable.add(BigDecimal.valueOf(metadataParams.getWalletOutstandingMinPayable()));
		// min outstanding as shown on FE
		Double minOutstanding = lastOrderPayable.compareTo(BigDecimal.ZERO) > 0 ? Double.min(lastOrderPayable.doubleValue(), userWalletOutstanding) : 0d;
		userWallet.getMetadata().setMinOutstanding(minOutstanding);
		// wallet adjustment as shown on FE
		userWallet.getMetadata().setWalletAdjustment(
				lastOrderOutstanding.subtract(BigDecimal.valueOf(minOutstanding)).add(BigDecimal.valueOf(userWallet.getMetadata().getWindowOutstanding()))
						.doubleValue());
	}*/

	//
	public void updateUserWalletMetadataVariablesV2(UserWalletEntity userWallet, WalletMetadataParamsBean metadataParams) {
		List<WalletStatementEntity> lastOrderTransactions = walletStatementRepository.getLastOrderTransactions(userWallet.getEntityId()).stream()
				.sorted(Comparator.comparing(WalletStatementEntity::getId)).collect(Collectors.toList());
		userWallet.getMetadata().setAfterOrderTopup(getAfterLastOrderTopup(lastOrderTransactions));
		userWallet.getMetadata().setLastOrderCost(getLastOrderCost(lastOrderTransactions));
		userWallet.getMetadata().setLastOrderRefund(getLastOrderRefund(lastOrderTransactions));
		// last order outstanding as shown on FE
		BigDecimal lastOrderOutstanding = BigDecimal.valueOf(userWallet.getMetadata().getLastOrderCost())
				.subtract(BigDecimal.valueOf(userWallet.getMetadata().getLastOrderRefund()))
				.subtract(BigDecimal.valueOf(userWallet.getMetadata().getAfterOrderTopup()));
		// total outstanding as shown on FE
		Double userWalletOutstanding = Double.max(BigDecimal.valueOf(userWallet.getAmount()).subtract(BigDecimal.valueOf(userWallet.getWalletHold())).multiply(BigDecimal.valueOf(-1d))
				.subtract(BigDecimal.valueOf(metadataParams.getWalletOutstandingTolerance())).doubleValue(), 0d);
		userWallet.getMetadata().setMinOutstanding(userWalletOutstanding);
		// wallet adjustment as shown on FE
		userWallet.getMetadata().setWalletAdjustment(lastOrderOutstanding.subtract(BigDecimal.valueOf(userWalletOutstanding)).doubleValue());
	}

	private Double getLastOrderCost(List<WalletStatementEntity> lastOrderTransactions) {
		return lastOrderTransactions.stream().filter(e -> !e.getTxnType().equals("FO-ITEM-REFUND") && !e.getTxnType().equals("FO-FULL-ORDER-REFUND"))
				.map(e -> e.getWalletType().equals(WalletType.WALLET.toString()) ^ e.getTxnMode().equals(WalletTxnMode.DEBIT.toString()) ?
						BigDecimal.valueOf(e.getAmount()).multiply(BigDecimal.valueOf(-1d)) :
						BigDecimal.valueOf(e.getAmount())).reduce(BigDecimal::add).orElse(BigDecimal.valueOf(0d)).doubleValue();
	}

	private Double getLastOrderRefund(List<WalletStatementEntity> lastOrderTransactions) {
		return lastOrderTransactions.stream()
				.filter(e -> e.getWalletType().equals(WalletType.WALLET.toString()) && (e.getTxnType().equals("FO-ITEM-REFUND") || e.getTxnType()
						.equals("FO-FULL-ORDER-REFUND"))).map(e -> e.getTxnMode().equals(WalletTxnMode.DEBIT.toString()) ?
						BigDecimal.valueOf(e.getAmount()).multiply(BigDecimal.valueOf(-1d)) :
						BigDecimal.valueOf(e.getAmount())).reduce(BigDecimal::add).orElse(BigDecimal.valueOf(0d)).doubleValue();
	}

	private Double getAfterLastOrderTopup(List<WalletStatementEntity> lastOrderTransactions) {
		return !lastOrderTransactions.isEmpty() ?
				walletStatementRepository.getAfterTransactionTopup(lastOrderTransactions.get(0).getEntityId(), lastOrderTransactions.get(0).getId()).stream()
						.map(e -> e.getTxnMode().equals(WalletTxnMode.DEBIT.toString()) ?
								BigDecimal.valueOf(e.getAmount()).multiply(BigDecimal.valueOf(-1d)) :
								BigDecimal.valueOf(e.getAmount())).reduce(BigDecimal::add).orElse(BigDecimal.valueOf(0d)).doubleValue() :
				0d;
	}

	private Double getBeforeTransactionWindowOutstanding(List<WalletStatementEntity> lastOrderTransactions, Date walletOutstandingWindowDate) {
		return !lastOrderTransactions.isEmpty() ?
				walletStatementRepository.getBeforeTransactionWindowOutstanding(lastOrderTransactions.get(0).getEntityId(),
						lastOrderTransactions.get(0).getId(), walletOutstandingWindowDate) :
				0d;
	}
}
