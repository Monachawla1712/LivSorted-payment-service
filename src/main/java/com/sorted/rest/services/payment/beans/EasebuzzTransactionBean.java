package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class EasebuzzTransactionBean implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	@JsonProperty("status")
	private Boolean status;

	@JsonProperty("msg")
	private EasebuzzTransactionPayload payload;
}
