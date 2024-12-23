package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class CashfreeTokenRequest {

	private String orderId;

	private Double orderAmount;

	private String orderCurrency;

}