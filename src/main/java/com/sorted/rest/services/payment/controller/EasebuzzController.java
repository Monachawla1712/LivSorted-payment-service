package com.sorted.rest.services.payment.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.payment.beans.CreateEasebuzzVAInternalRequest;
import com.sorted.rest.services.payment.beans.CreateEasebuzzVAResponse;
import com.sorted.rest.services.payment.beans.EasebuzzCreateVABean;
import com.sorted.rest.services.payment.clients.ClientService;
import com.sorted.rest.services.payment.entity.EasebuzzVirtualAccountEntity;
import com.sorted.rest.services.payment.services.EasebuzzService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@Api(tags = "Easebuzz Services", description = "Easebuzz related services.")
public class EasebuzzController implements BaseController {

	AppLogger _LOGGER = LoggingManager.getLogger(EasebuzzController.class);

	@Autowired
	private EasebuzzService easebuzzService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private BaseMapper<?, ?> mapper;

	@ApiOperation(value = "Create Easebuzz Virtual Account", nickname = "createEasebuzzVirtualAccount")
	@PostMapping(path = "/payments/easebuzz/virtual-account")
	@ResponseStatus(HttpStatus.OK)
	public CreateEasebuzzVAResponse createEasebuzzVirtualAccount(@RequestBody @Valid CreateEasebuzzVAInternalRequest request) {
		CreateEasebuzzVAResponse response = CreateEasebuzzVAResponse.newInstance();
		Optional<EasebuzzVirtualAccountEntity> existingVA = easebuzzService.findByEntityDetails(request.getEntityId(), request.getEntityType());
		if (existingVA.isPresent()) {
			response.setId(existingVA.get().getId());
			response.setQrCodeUrl(existingVA.get().getQrCodePng());
			return response;
		}
		Object easebuzzResponse = clientService.createEasebuzzVA(request);
		EasebuzzCreateVABean easebuzzResponseBean = getMapper().convertValue(easebuzzResponse, EasebuzzCreateVABean.class);
		if (easebuzzResponseBean == null || easebuzzResponseBean.getSuccess() != true) {
			_LOGGER.error("Error while creating Easebuzz Virtual Account");
			throw new ServerException(
					new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while creating Easebuzz Virtual Account. Kindly try again."));
		}
		EasebuzzVirtualAccountEntity entity = buildEasebuzzVirtualAccountEntity(easebuzzResponseBean, request, easebuzzResponse);
		entity = easebuzzService.save(entity);
		response.setId(entity.getId());
		response.setQrCodeUrl(entity.getQrCodePng());
		return response;
	}

	public EasebuzzVirtualAccountEntity buildEasebuzzVirtualAccountEntity(EasebuzzCreateVABean payload, CreateEasebuzzVAInternalRequest request,
			Object easebuzzResponse) {
		EasebuzzVirtualAccountEntity entity = EasebuzzVirtualAccountEntity.newInstance();
		entity.setEntityId(request.getEntityId());
		entity.setEntityType(request.getEntityType());
		entity.setLabel(payload.getData().getVa().getLabel());
		entity.setVirtualAccountId(payload.getData().getVa().getId());
		entity.setVirtualAccountNumber(payload.getData().getVa().getVirtualAccountNumber());
		entity.setVirtualIfscCode(payload.getData().getVa().getVirtualIfscCode());
		entity.setVirtualUpiHandle(payload.getData().getVa().getVirtualUpiHandle());
		entity.setQrCodePng(payload.getData().getVa().getUpiQrcodeRemoteFileLocation());
		entity.setQrCodePdf(payload.getData().getVa().getUpiQrcodeScannerRemoteFileLocation());
		entity.getMetadata().setPaymentGatewayResponse(easebuzzResponse);
		return entity;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}
}
