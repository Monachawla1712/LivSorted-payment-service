package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class EasebuzzTransactionResponse implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	@JsonProperty("result")
	private String status;

	@JsonProperty("payment_response")
	private EasebuzzTransactionPayload payload;
}
