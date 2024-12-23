package com.sorted.rest.services.payment.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderCountRequest {

	private List<String> storeIds;
}
