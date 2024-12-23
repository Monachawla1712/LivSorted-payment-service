package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.io.Serializable;
import java.sql.Date;

@Data
public class OrderDetailResponse implements Serializable {

	private static final long serialVersionUID = -2067398928213224730L;

	private String displayOrderId;

	private Date deliveryDate;

}
