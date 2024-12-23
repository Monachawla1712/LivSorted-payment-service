package com.sorted.rest.services.payment.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class InitiateTxnRequestV2 {

	@NotNull
	private double amount;

	private List<Long> ccIds;

}
