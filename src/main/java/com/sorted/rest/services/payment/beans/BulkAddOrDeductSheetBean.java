package com.sorted.rest.services.payment.beans;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkAddOrDeductSheetBean implements Serializable, CSVMapping {

	private static final long serialVersionUID = 7400716746876340944L;

	private String customerId;

	private Double amount;

	public static BulkAddOrDeductSheetBean newInstance() {
		return new BulkAddOrDeductSheetBean();
	}

	@Override
	public BulkAddOrDeductSheetBean newBean() {
		return new BulkAddOrDeductSheetBean();
	}

	@Override
	public String getHeaderMapping() {
		return "customerId:customer id,amount:amount";
	}

	private List<ErrorBean> errors = new ArrayList<>();

}
