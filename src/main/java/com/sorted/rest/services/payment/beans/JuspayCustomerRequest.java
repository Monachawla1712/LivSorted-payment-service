package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class JuspayCustomerRequest implements Serializable {

	private static final long serialVersionUID = -5135090458754245196L;

	@NotNull
	@JsonProperty("object_reference_id")
	private String id;

	@NotNull
	@JsonProperty("mobile_number")
	private String phone;

	@JsonProperty("first_name")
	private String firstName;

	@JsonProperty("last_name")
	private String lastName;
}