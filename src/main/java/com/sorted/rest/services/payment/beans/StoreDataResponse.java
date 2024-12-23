package com.sorted.rest.services.payment.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
public class StoreDataResponse {

	@JsonProperty("id")
	private String id;

	@JsonProperty("name")
	private String name;

	@JsonProperty("store_id")
	private String storeId;

	@JsonProperty("ownerId")
	private UUID ownerId;
}
