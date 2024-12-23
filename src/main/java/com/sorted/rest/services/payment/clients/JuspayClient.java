package com.sorted.rest.services.payment.clients;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.sorted.rest.services.payment.beans.JuspayCustomerRequest;
import com.sorted.rest.services.payment.beans.JuspayCustomerResponse;
import com.sorted.rest.services.payment.beans.JuspaySessionRequest;

@FeignClient(value = "juspayClient", url = "${client.juspay.url}")
public interface JuspayClient {

	@PostMapping(value = "/session")
	Object createSession(@RequestHeader Map<String, Object> headers, @RequestBody JuspaySessionRequest body);

	@GetMapping(value = "/orders/{orderId}")
	Object fetchOrderStatus(@RequestHeader Map<String, Object> headers, @PathVariable String orderId);

	@PostMapping(value = "/customers")
	JuspayCustomerResponse createCustomer(@RequestHeader Map<String, Object> headers, @RequestBody JuspayCustomerRequest body);

}