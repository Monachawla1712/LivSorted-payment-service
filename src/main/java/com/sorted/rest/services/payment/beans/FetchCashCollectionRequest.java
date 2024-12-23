package com.sorted.rest.services.payment.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
public class FetchCashCollectionRequest implements Serializable {

	@NotEmpty
	private String customerId;

	@NotEmpty
	private String collectorMobileNumber;

	@NotEmpty
	private String date;

}
