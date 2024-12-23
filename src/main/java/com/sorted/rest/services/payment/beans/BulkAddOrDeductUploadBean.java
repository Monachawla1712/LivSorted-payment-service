package com.sorted.rest.services.payment.beans;

import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkAddOrDeductUploadBean extends BulkAddOrDeductSheetBean implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	private String txnType;

	private String txnDetail;

}