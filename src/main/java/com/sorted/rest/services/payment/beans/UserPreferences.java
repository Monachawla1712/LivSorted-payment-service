package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class UserPreferences {
	private Integer slot;
	private String paymentMethod;
	private String paymentPreference;
}
