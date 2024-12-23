package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateEasebuzzVARequest implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	private String key;

	private String label;

	public static CreateEasebuzzVARequest newInstance() {
		return new CreateEasebuzzVARequest();
	}
}
