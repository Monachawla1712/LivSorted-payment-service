package com.sorted.rest.services.payment.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.payment.beans.StoreDataResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "store", url = "${client.store.url}", configuration = { FeignCustomConfiguration.class })
public interface StoreClient {

	@GetMapping(value = "/store-app/internal/store")
	List<StoreDataResponse> getStoreDataFromIds(@RequestParam(name = "store_id") String storeId);
}