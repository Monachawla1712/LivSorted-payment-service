package com.sorted.rest.services.payment.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.services.common.upload.AWSUploadService;
import com.sorted.rest.services.payment.beans.LedgerDataBean;
import com.sorted.rest.services.payment.beans.LedgerTxnBean;
import com.sorted.rest.services.payment.beans.OrderDetailResponse;
import com.sorted.rest.services.payment.beans.StoreDataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class LedgerService {

	private AppLogger _LOGGER = LoggingManager.getLogger(LedgerService.class);

	@Autowired
	private AWSUploadService awsUploadService;

	public LedgerDataBean buildLedgerData(List<LedgerTxnBean> txns, StoreDataResponse store, Date fromDate, Date toDate,
			Map<String, OrderDetailResponse> orderMap) {
		LedgerDataBean ledgerDataBean = new LedgerDataBean();
		ledgerDataBean.setStoreId(store == null || store.getStoreId() == null ? "" : store.getStoreId());
		ledgerDataBean.setStoreName(store == null || store.getName() == null ? "" : store.getName());
		ledgerDataBean.setFromDate(DateUtils.toString(DateUtils.DATE_MM_FMT, fromDate));
		ledgerDataBean.setToDate(DateUtils.toString(DateUtils.DATE_MM_FMT, toDate));
		setTxnsData(ledgerDataBean, txns, orderMap);
		ledgerDataBean.setFileName(getPdfNameWithDate(ledgerDataBean));
		setOpeningBalance(ledgerDataBean, txns);
		return ledgerDataBean;
	}

	private void setOpeningBalance(LedgerDataBean ledgerDataBean, List<LedgerTxnBean> txns) {
		LedgerTxnBean firstTxn = txns.get(0);
		if (firstTxn.getTxnMode().equals("CREDIT")) {
			ledgerDataBean.setOpeningBalance(BigDecimal.valueOf(firstTxn.getBalance()).subtract(BigDecimal.valueOf(firstTxn.getAmount())).doubleValue());
		}
		if (firstTxn.getTxnMode().equals("DEBIT")) {
			ledgerDataBean.setOpeningBalance(BigDecimal.valueOf(firstTxn.getBalance()).add(BigDecimal.valueOf(firstTxn.getAmount())).doubleValue());
		}
	}

	private void setTxnsData(LedgerDataBean ledgerDataBean, List<LedgerTxnBean> txns, Map<String, OrderDetailResponse> orderMap) {
		for (LedgerTxnBean txn : txns) {
			if (orderMap != null && orderMap.containsKey(txn.getTxnDetail()) && orderMap.get(txn.getTxnDetail()).getDeliveryDate() != null) {
				txn.setOrderDate(DateUtils.toString(DateUtils.DATE_MM_FMT, orderMap.get(txn.getTxnDetail()).getDeliveryDate()));
			}
			txn.setCreatedDate(DateUtils.toString(DateUtils.DATE_MM_TIME_FMT, DateUtils.convertDateUtcToIst(txn.getCreatedAt())));
		}
		ledgerDataBean.setTxns(txns);
	}

	private String getPdfNameWithDate(LedgerDataBean ledgerDataBean) {
		String filename = ledgerDataBean.getStoreId().concat("_").concat(ledgerDataBean.getToDate()).concat("_").concat(ledgerDataBean.getFromDate())
				.concat("_").concat(String.valueOf(Instant.now().toEpochMilli())).concat(".pdf");
		return filename;
	}

	public void uploadLedger(LedgerDataBean ledgerDataBean) {
		String bucketName = ParamsUtils.getParam("SORTED_FILES_BUCKET_NAME");
		String subDirectory = ParamsUtils.getParam("LEDGER_FILES_SUBDIRECTORY");
		File directoryPath = new File(System.getProperty("user.dir"));
		File files[] = directoryPath.listFiles();
		try {
			for (File file : files) {
				if (file.getName().endsWith(".pdf")) {
					_LOGGER.info(String.format("Uploading Ledger - %s to s3", ledgerDataBean.getFileName()));
					byte[] fileBytes = Files.readAllBytes(Path.of(directoryPath.getPath().concat("/").concat(file.getName())));
					Object response = awsUploadService.uploadFile(bucketName, subDirectory, fileBytes, ledgerDataBean.getFileName());
					ledgerDataBean.setLedgerUrl((ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(response.toString())));
					file.delete();
				}
			}
		} catch (IOException err) {
			_LOGGER.error("ledger Upload error", err);
			throw new ServerException(new ErrorBean(Errors.UPDATE_FAILED, "Error while uploading Ledger", "ledger"));
		}
	}
}
