package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@ApiModel(description = "Backoffice Credit Limit Upload Bean")
public class CreditLimitUploadBean implements Serializable, CSVMapping {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	private String storeId;

	@NotNull
	private Double creditLimit;

	private Date date;

	private BigDecimal changeAmount;

	private List<ErrorBean> errors = new ArrayList<>();

	public static CreditLimitUploadBean newInstance() {
		return new CreditLimitUploadBean();
	}

	@Override
	public CreditLimitUploadBean newBean() {
		return newInstance();
	}

	@Override
	public List<ErrorBean> getErrors() {
		return errors;
	}

	@Override
	@JsonIgnore
	public String getHeaderMapping() {
		return "storeId:Store Id,creditLimit:Credit Limit";
	}

}

