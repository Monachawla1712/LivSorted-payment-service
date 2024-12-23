package com.sorted.rest.services.payment.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.BeanValidationUtils;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.common.upload.csv.CSVBulkRequest;
import com.sorted.rest.services.common.upload.csv.CsvUploadResult;
import com.sorted.rest.services.common.upload.csv.CsvUtils;
import com.sorted.rest.services.payment.beans.PaymentRequestUploadBean;
import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.entity.PaymentRequestEntity;
import com.sorted.rest.services.payment.services.PaymentRequestUploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@Api(tags = "Payment Request Upload Services", description = "Manage Payment Request Upload related services.")
public class PaymentRequestUploadController implements BaseController {

	static AppLogger _LOGGER = LoggingManager.getLogger(BaseController.class);

	@Autowired
	PaymentRequestUploadService paymentRequestUploadService;

	@Autowired
	private BaseMapper<?, ?> baseMapper;

	@Override
	public BaseMapper<?, ?> getMapper() {
		return baseMapper;
	}

	@ApiOperation(value = "upload Projected Sr Inventory ", nickname = "uploadProjectedSr")
	@PostMapping(path = "/payments/requests/store/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CsvUploadResult<PaymentRequestUploadBean> uploadPaymentRequest(
			@RequestParam(name = "walletType") WalletType walletType,
			@RequestParam("file") MultipartFile file
	) {
		final int maxAllowedRows = 1000;
		final String module = "payment-request";
		List<PaymentRequestUploadBean> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, PaymentRequestUploadBean.newInstance());
		List<PaymentRequestUploadBean> response = paymentRequestUploadService.preProcessPaymentRequestsUpload(rawBeans, EntityType.STORE, walletType);
		CsvUploadResult<PaymentRequestUploadBean> result = validatePaymentRequestUpload(response);
		result.setHeaderMapping(response.get(0).getHeaderMapping());
		CsvUtils.saveBulkRequestData(SessionUtils.getAuthUserId(), module, result);
		return result;
	}

	private CsvUploadResult<PaymentRequestUploadBean> validatePaymentRequestUpload(List<PaymentRequestUploadBean> beans) {
		final CsvUploadResult<PaymentRequestUploadBean> result = new CsvUploadResult<>();

		if (CollectionUtils.isNotEmpty(beans)) {
			beans.stream().forEach(bean -> {
				try {
					Errors errors = getSpringErrors(bean);
					paymentRequestUploadService.validatePaymentRequestOnUpload(bean, errors);
					checkError(errors);
					result.addSuccessRow(bean);

				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						_LOGGER.error(e.getMessage(), e);
						final List<ErrorBean> errors = e instanceof ValidationException
								? BeanValidationUtils.prepareValidationResponse((ValidationException) e).getErrors()
								: Arrays.asList(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
						_LOGGER.info("Payment Request Uploaded data is having error =>" + errors.toString());
					}
					result.addFailedRow(bean);
				}
			});
		}
		return result;
	}

	@ApiOperation(value = "Save Payment Request Csv", nickname = "savePaymentRequestCsv")
	@PostMapping("/payments/requests/store/upload/save")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void savePaymentRequest(@RequestParam(name = "key") String key, @RequestParam(name = "cancel", required = false) Integer cancel) {
		final boolean cleanup = cancel != null;
		if (cleanup) {
			cancelUpload(key);
		} else {
			savePaymentRequestBulkData(key);
		}
	}

	public void cancelUpload(String key) {
		final int deleteCount = CsvUtils.cancelUpload(key);
		_LOGGER.info(String.format("Upload Cancel called with Key = %s and delete count is = %s", key, deleteCount));
		if (deleteCount <= 0) {
			throw new ValidationException(ErrorBean.withError("UPLOAD_CANCEL_ERROR", "Unable to cancel bulk upload request.", null));
		}
	}

	private void savePaymentRequestBulkData(String key) {
		final CSVBulkRequest<PaymentRequestUploadBean> uploadedData = CsvUtils.getBulkRequestData(key, PaymentRequestUploadBean.class);
		if (uploadedData != null && CollectionUtils.isNotEmpty(uploadedData.getData())) {
			List<PaymentRequestEntity> entityList = getMapper().mapAsList(uploadedData.getData(), PaymentRequestEntity.class);
			paymentRequestUploadService.bulkSave(entityList);
			CsvUtils.markUploadProcessed(key);
		} else {
			throw new ValidationException(ErrorBean.withError("UPLOAD_ERROR", "Uploaded data not found or it is expired.", null));
		}
	}
}
