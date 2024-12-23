package com.sorted.rest.services.payment.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.payment.beans.NotificationServiceSmsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(value = "notification", url = "${client.notification.url}", configuration = { FeignCustomConfiguration.class })
public interface NotificationClient {

	@GetMapping(value = "/notification/sms/send")
	void sendCcVerificationCode(List<NotificationServiceSmsRequest> smsRequest);
}