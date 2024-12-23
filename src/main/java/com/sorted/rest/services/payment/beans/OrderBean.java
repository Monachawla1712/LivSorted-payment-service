package com.sorted.rest.services.payment.beans;

import java.io.Serializable;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import com.sorted.rest.common.websupport.base.BaseBean;
import com.sorted.rest.services.payment.constants.PaymentConstants.OrderStatus;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiModelProperty.AccessMode;
import lombok.Data;

/**
 * Bean to be returned with Contains Orders
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Response Bean extending the List Bean")
@Data
public class OrderBean extends BaseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = " Order Id.", accessMode = AccessMode.READ_ONLY)
	@Null
	private UUID id;

	@ApiModelProperty(value = "Customer Id who placed order", allowEmptyValue = false)
	@NotNull
	private UUID customerId;

	@ApiModelProperty(value = "Display Order ID", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "Store Id to which order is placed", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "final Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double finalBillAmount;

	@ApiModelProperty(value = "estimated Bill Amount", allowEmptyValue = true)
	@NotNull
	private Double estimatedBillAmount;

	@ApiModelProperty(value = "amountReceived", allowEmptyValue = true)
	@NotNull
	private Double amountReceived;

	@NotNull
	private OrderStatus status;

	@NotNull
	private PaymentDetail paymentDetail;

	public static OrderBean newInstance() {
		return new OrderBean();
	}
}
