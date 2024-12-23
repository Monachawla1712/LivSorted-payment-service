package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class JuspayCustomerResponse implements Serializable {

	private static final long serialVersionUID = -426438889276188345L;

	private String id;

	private String object;

	@JsonProperty("object_reference_id")
	private String objectReferenceId;

	@JsonProperty("mobile_country_code")
	private String mobileCountryCode;

	@JsonProperty("mobile_number")
	private String mobileNumber;

	@JsonProperty("email_address")
	private String emailAddress;

	@JsonProperty("first_name")
	private String firstName;

	@JsonProperty("last_name")
	private String lastName;

	@JsonProperty("date_created")
	private String dateCreated;

	@JsonProperty("last_updated")
	private String lastUpdated;

}