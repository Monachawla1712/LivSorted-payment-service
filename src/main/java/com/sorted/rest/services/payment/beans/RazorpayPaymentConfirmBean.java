package com.sorted.rest.services.payment.beans;

import lombok.Data;

@Data
public class RazorpayPaymentConfirmBean {

	private String razorpayPaymentId;

	private String razorpayOrderId;

	private String razorpaySignature;

	public static RazorpayPaymentConfirmBean newInstance() {
		return new RazorpayPaymentConfirmBean();
	}
}