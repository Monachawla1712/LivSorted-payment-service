package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateEasebuzzVAResponse implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	private Integer id;

	private String qrCodeUrl;

	public static CreateEasebuzzVAResponse newInstance() {
		return new CreateEasebuzzVAResponse();
	}
}
