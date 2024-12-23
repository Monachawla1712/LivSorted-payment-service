package com.sorted.rest.services.payment.utils;

import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ExceptionHandlerUtils {

	private AppLogger _LOGGER = LoggingManager.getLogger(ExceptionHandlerUtils.class);

	public Boolean isHandledWsException(Exception e) {
		if (e != null) {
			if (e instanceof DataIntegrityViolationException) {
				DataIntegrityViolationException dataIntegrityViolationException = (DataIntegrityViolationException) e;
				Throwable cause = dataIntegrityViolationException.getRootCause();
				if (cause instanceof PSQLException) {
					PSQLException psqlException = (PSQLException) cause;
					if (PaymentConstants.PSQL_CONSTRAINT_VIOLATION_SQL_STATE.equals(psqlException.getSQLState()) && !StringUtils.isEmpty(
							psqlException.getMessage()) && psqlException.getMessage().contains(PaymentConstants.WS_KEY_CONSTRAINT)) {
						_LOGGER.info("Handled error occurred while inserting wallet statement with message: " + psqlException.getMessage());
						return Boolean.TRUE;
					}
				}
			}
		}
		return Boolean.FALSE;
	}
}
