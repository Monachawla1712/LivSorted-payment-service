package com.sorted.rest.services.payment.beans;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class NotificationServiceSmsRequest {
	Map<String, String> fillers;

	private UUID userId;

	private String templateName;
}

