package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletMetadataParamsBean implements Serializable {

	private static final long serialVersionUID = -5571723327178720853L;

	private Double walletOutstandingTolerance;

	private Date walletOutstandingWindowDate;

	private Double walletOutstandingMinPayable;

	@JsonIgnore
	private Set<String> walletUpdateExclusionSet;
}
