package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class CreateEasebuzzVAInternalRequest implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	@NotNull
	private String entityId;

	@NotNull
	private PaymentConstants.EntityType entityType;

	@NotNull
	private String label;

	public static CreateEasebuzzVAInternalRequest newInstance() {
		return new CreateEasebuzzVAInternalRequest();
	}
}
