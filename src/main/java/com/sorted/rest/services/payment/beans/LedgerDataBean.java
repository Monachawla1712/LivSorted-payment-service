package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LedgerDataBean implements Serializable {

	private static final long serialVersionUID = -1217133370533767490L;

	private String storeId;

	private String storeName;

	private Double openingBalance;

	private String fileName;

	private String fromDate;

	private String toDate;

	private String ledgerUrl;

	private List<LedgerTxnBean> txns;
}
