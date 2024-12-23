package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class JuspayOrderPaidRequest implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	private String id;

	@JsonProperty("date_created")
	private String dateCreated;

	@JsonProperty("event_name")
	private String eventName;

	private Content content;

	@Data
	public static class Content implements Serializable {

		private static final long serialVersionUID = 8453503875733187507L;

		private JuspayOrderBean order;
	}
}