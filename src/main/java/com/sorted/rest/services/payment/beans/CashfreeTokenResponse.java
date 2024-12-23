package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class CashfreeTokenResponse {

	private String status;

	private String message;

	private String cftoken;

}
