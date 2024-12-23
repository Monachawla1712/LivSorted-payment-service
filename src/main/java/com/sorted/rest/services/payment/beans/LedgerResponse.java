package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class LedgerResponse implements Serializable {

	private String ledgerUrl;
}
