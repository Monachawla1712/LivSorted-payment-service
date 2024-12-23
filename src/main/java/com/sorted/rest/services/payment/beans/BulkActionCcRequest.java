package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionCcRequest implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	@NotNull
	private CashCollectionStatus status;

	@NotNull
	private List<CcDetails> ccDetails;

	@JsonIgnore
	private String referenceId;

	public static BulkActionCcRequest newInstance() {
		return new BulkActionCcRequest();
	}

	@Data
	public static class CcDetails {

		private Long id;

		private String txnMode;

		private String remarks;

		public static CcDetails newInstance() {
			return new CcDetails();
		}

	}
}
