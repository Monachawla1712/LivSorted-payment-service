package com.sorted.rest.services.payment.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.payment.beans.BulkActionPrRequest;
import com.sorted.rest.services.payment.beans.PaymentRequestResponse;
import com.sorted.rest.services.payment.beans.WalletTxnBean;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.entity.PaymentRequestEntity;
import com.sorted.rest.services.payment.services.PaymentRequestService;
import com.sorted.rest.services.payment.utils.UserUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "Payment Request Services", description = "Payment Request related services.")
public class PaymentRequestController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(PaymentRequestController.class);

	@Autowired
	private PaymentRequestService paymentRequestService;

	@Autowired
	private UserUtils userUtils;

	@Autowired
	private BaseMapper<?, ?> baseMapper;

	@ApiOperation(value = "get Payment Requests", nickname = "getPaymentRequests")
	@GetMapping(path = "/payments/requests")
	public PageAndSortResult<PaymentRequestResponse> getPaymentRequests(@RequestParam(defaultValue = "1") Integer pageNo,
																		@RequestParam(defaultValue = "25") Integer pageSize, @RequestParam(required = false) String sortBy,
																		@RequestParam(required = false) PageAndSortRequest.SortDirection sortDirection, HttpServletRequest request) {
		Map<String, PageAndSortRequest.SortDirection> sort = null;
		if (sortBy != null) {
			sort = buildSortMap(sortBy, sortDirection);
		} else {
			sort = new LinkedHashMap<>();
			sort.put("createdAt", PageAndSortRequest.SortDirection.DESC);
		}
		Map<String, Object> params = getSearchParams(request, PaymentRequestEntity.class);
        if (params.containsKey("status")) {
            params.put("status", PaymentConstants.PaymentRequestStatus.fromString(params.get("status").toString()));
        }
        PageAndSortResult<PaymentRequestEntity> prList = paymentRequestService.getAllPaginatedPaymentRequests(pageSize, pageNo, params, sort);
		PageAndSortResult<PaymentRequestResponse> response = new PageAndSortResult<>();
		if (prList != null && prList.getData() != null) {
			response = prepareResponsePageData(prList, PaymentRequestResponse.class);
		}
		return response;
	}

	@ApiOperation(value = "Approve/Reject Payment Requests", nickname = "bulkApproveOrRejectPaymentRequests")
	@PostMapping(path = "/payments/requests/approve")
	public ResponseEntity<List<ErrorBean>> bulkApproveOrRejectPaymentRequests(@RequestBody @Valid BulkActionPrRequest request) {
		return ResponseEntity.ok(paymentRequestService.bulkApproveOrRejectPaymentRequests(request));
	}

	@ApiOperation(value = "Create Payment Request from admin portal", nickname = "createPaymentRequest")
	@PostMapping("/payments/requests/{entityType}/{entityId}")
	public void createPaymentRequest(@PathVariable EntityType entityType, @PathVariable String entityId, @RequestBody WalletTxnBean request) {
		if (request.getAmount() == null || request.getAmount().compareTo(0d) == 0)
			throw new ValidationException(new ErrorBean(Errors.INVALID_REQUEST, "Payment request of amount zero is not allowed", "amount"));
		PaymentRequestEntity paymentRequest = getMapper().mapSrcToDest(request, PaymentRequestEntity.newInstance());
		paymentRequest.setEntityType(entityType);
		paymentRequest.setEntityId(entityId);
		if (paymentRequest.getAmount().compareTo(0d) == -1) {
			paymentRequest.setTxnMode(PaymentConstants.WalletTxnMode.DEBIT.toString());
			paymentRequest.setAmount(BigDecimal.valueOf(paymentRequest.getAmount()).multiply(BigDecimal.valueOf(-1d)).doubleValue());
		} else {
			paymentRequest.setTxnMode(PaymentConstants.WalletTxnMode.CREDIT.toString());
		}
		paymentRequest.setStatus(PaymentConstants.PaymentRequestStatus.REQUESTED);
		paymentRequest.getMetadata().setRequestedBy(userUtils.getUserDetail(SessionUtils.getAuthUserId()));
		paymentRequestService.save(paymentRequest);
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return baseMapper;
	}
}
