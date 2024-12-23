package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants.WalletStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserWalletBean implements Serializable {

	private static final long serialVersionUID = -5571723327178720853L;

	private String entityId;

	private String entityType;

	private Double amount;

	private Double loyaltyCoins;

	private Double walletHold;

	private Double creditLimit;

	private WalletStatus status;

	private UserWalletMetadata metadata;

	public static UserWalletBean newInstance() {
		return new UserWalletBean();
	}
}
