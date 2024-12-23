package com.sorted.rest.services.payment.clients;

import com.sorted.rest.services.payment.beans.WmsStoreDataResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(value = "wms", url = "${client.wms.url}")
public interface WmsClient {

	@GetMapping(value = "/api/v1/stores")
	List<WmsStoreDataResponse> getStoreDetails(@RequestHeader Map<String, Object> headers, @RequestParam(name = "ids") Set<Integer> ids,
			@RequestParam(name = "storeType") String storeType);
}