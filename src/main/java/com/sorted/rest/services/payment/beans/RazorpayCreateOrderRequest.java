package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class RazorpayCreateOrderRequest {

	private String receipt;

	private Integer amount;

	private String currency;

}