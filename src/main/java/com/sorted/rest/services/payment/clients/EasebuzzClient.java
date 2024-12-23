package com.sorted.rest.services.payment.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "easebuzzClient", url = "${client.easebuzz.url}")
public interface EasebuzzClient {

	@PostMapping(value = "/payment/initiateLink", consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE })
	Object initiatePayment(@RequestBody MultiValueMap<String, String> body);

}