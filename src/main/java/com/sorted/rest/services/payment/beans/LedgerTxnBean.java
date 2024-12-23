package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class LedgerTxnBean extends WalletStatementBean {

	private static final long serialVersionUID = 1207615262502187118L;

	private String orderDate;

	private String createdDate;

}
