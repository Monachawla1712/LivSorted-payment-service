package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = PaymentConstants.CREDIT_LIMIT_CHANGE_TABLE_NAME)
@DynamicUpdate
@Data
public class CreditLimitChangeEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column(nullable = false)
	private String storeId;

	@Column(nullable = false)
	private Date date;

	@Column(nullable = false)
	private BigDecimal changeAmount;

	public static CreditLimitChangeEntity newInstance() {
		CreditLimitChangeEntity creditLimitChangeEntity = new CreditLimitChangeEntity();
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30);
		creditLimitChangeEntity.setDate(localDate.toDate());
		return creditLimitChangeEntity;
	}
}
