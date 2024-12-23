package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sorted.rest.common.websupport.base.BaseBean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashCollectionResponse extends BaseBean implements Serializable {

	private static final long serialVersionUID = 192389943235679096L;

	private Long id;

	private Date date;

	private String slot;

	private String storeId;

	private String customerId;

	private String status;

	private Double requestedAmount;

	private Double collectedAmount;

	private Double receivedAmount;

	private CcMetadata metadata;

	private String storeName;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private java.util.Date creationTime;

	public static CashCollectionResponse newInstance() {
		return new CashCollectionResponse();
	}
}
