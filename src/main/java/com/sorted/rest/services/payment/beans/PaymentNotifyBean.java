package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PaymentNotifyBean implements Serializable {

	private static final long serialVersionUID = -8129150877695828452L;

	private String id;

	private String paymentMode;

	private String orderId;

	private String txTime;

	private String referenceId;

	private String type;

	private String txMsg;

	private String signature;

	private String orderAmount;

	private String txStatus;

	private List<Object> paymentGatewayResponse;

	private Long ccId;

	public static PaymentNotifyBean newInstance() {
		return new PaymentNotifyBean();
	}
}
