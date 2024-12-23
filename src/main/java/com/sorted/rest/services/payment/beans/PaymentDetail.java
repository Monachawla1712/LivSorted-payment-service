package com.sorted.rest.services.payment.beans;

import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentStatus;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

@Data
public class PaymentDetail implements Serializable {

	private static final long serialVersionUID = 2989920741806143595L;

	private PaymentStatus paymentStatus = PaymentStatus.PENDING;

	private String paymentGateway;

	private Set<String> transactions;
}
