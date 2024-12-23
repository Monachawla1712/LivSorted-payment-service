package com.sorted.rest.services.payment.clients;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sorted.rest.services.payment.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;

@FeignClient(value = "order", url = "${client.order.url}", configuration = { FeignCustomConfiguration.class })
public interface OrderClient {

	@GetMapping(value = "/orders/{id}/internal")
	OrderBean getOrderById(@PathVariable UUID id);

	@PostMapping(value = "/orders/internal/payment-update")
	void sendOrderPaymentUpdate(@RequestBody OrderBean order);

	@GetMapping(value = "/orders/franchise/display-ids")
	List<OrderDetailResponse> getOrderDetailsByDisplayOrderIds(@RequestParam() Set<String> ids);

	@PostMapping(value = "/orders/franchise/orders")
	List<StoreOrderCount> getFranchiseOrdersForStores(@RequestBody StoreOrderCountRequest storeIds);

	@PutMapping(value = "/orders/franchise/cart/refresh")
	void refreshCart(@RequestBody UpdateFranchiseCartRequest updateFranchiseCartRequest);
}