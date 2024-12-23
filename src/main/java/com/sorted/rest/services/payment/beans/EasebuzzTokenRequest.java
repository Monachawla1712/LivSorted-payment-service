package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class EasebuzzTokenRequest {

	private String key;

	private String txnid;

	private Double amount;

	private String surl;

	private String furl;

	private String productinfo = "NA";

	private String firstname = "NA";

	private Integer phone = 1000000001;

	private String email = "NA@dummy.email";

	private String hash;
}
