package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.beans.PrMetadata;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import com.sorted.rest.services.payment.constants.PaymentConstants.PaymentRequestStatus;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;

@Entity
@Table(name = PaymentConstants.PAYMENT_REQUESTS_TABLE_NAME)
@DynamicUpdate
@Data
@Where(clause = "active = 1")
public class PaymentRequestEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EntityType entityType;

	@Column(nullable = false)
	private String entityId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PaymentRequestStatus status;

	@Column(nullable = false)
	private String txnMode;

	@Column(nullable = false)
	private String txnType;

	@Column(nullable = false)
	private String txnDetail;

	@Column(nullable = false)
	private String walletType;

	@Column(nullable = false)
	private Double Amount;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private PrMetadata metadata = new PrMetadata();

	@Column
	private String remarks;

	public static PaymentRequestEntity newInstance() {
		PaymentRequestEntity entity = new PaymentRequestEntity();
		return entity;
	}
}