package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class WalletStatementBean implements Serializable {

	private static final long serialVersionUID = 5875998682647758301L;

	private Double amount;

	private Double balance;

	private String txnMode;

	private String txnType;

	private String txnDetail;

	private Date createdAt;

	private String remarks;

	private List<WalletStatementBean> adjustments;

	public static WalletStatementBean newInstance() {
		return new WalletStatementBean();
	}
}
