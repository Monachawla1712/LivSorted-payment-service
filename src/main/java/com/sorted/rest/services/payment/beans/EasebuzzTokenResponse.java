package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class EasebuzzTokenResponse {

	private Integer status;

	private String error_desc;

	private String data;
}
