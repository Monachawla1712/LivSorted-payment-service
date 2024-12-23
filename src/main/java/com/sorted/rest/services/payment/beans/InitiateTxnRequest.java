package com.sorted.rest.services.payment.beans;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class InitiateTxnRequest {

	@NotNull
	private UUID orderId;

}
