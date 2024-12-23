package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserWalletMetadata implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	private Double lastOrderCost = 0d;

	private Double lastOrderRefund = 0d;

	private Double afterOrderTopup = 0d;

	private Double walletAdjustment = 0d;

	private Double minOutstanding = 0d;

	private Double windowOutstanding = 0d;

	public static UserWalletMetadata newInstance() {
		return new UserWalletMetadata();
	}
}
