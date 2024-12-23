package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants.WalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTxnBean implements Serializable {

	private static final long serialVersionUID = 7274431348615296955L;

	private Double amount;

	@NotEmpty
	private String txnType;

	@NotEmpty
	private String txnDetail;

	private WalletType walletType;

	private String remarks;

	private Double holdAmount;
}
