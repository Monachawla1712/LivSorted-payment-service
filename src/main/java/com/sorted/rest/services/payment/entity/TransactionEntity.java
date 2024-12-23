package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.beans.TxnMetadata;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * @author mohit
 */
@Entity
@Table(name = PaymentConstants.TRANSACTIONS_TABLE_NAME)
@DynamicUpdate
@Data
public class TransactionEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(generator = "shortenedUUID")
	@GenericGenerator(name = "shortenedUUID", strategy = "com.sorted.rest.services.payment.utils.ShortenedUUIDGenerator")
	private String id;

	@Column(nullable = false)
	private UUID customerId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EntityType entityType;

	@Column
	private String entityId;

	@Column
	private UUID orderId;

	@Column
	private String storeId;

	@Column(nullable = false)
	private Double amount;

	@Column
	private String paymentMode;

	@Column
	private String paymentGateway;

	@Column
	private String medium;

	@Column
	private String referenceId;

	@Column
	private String error;

	@Column(nullable = false)
	private String status;

	@Column
	private Date processedAt;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private TxnMetadata metadata = new TxnMetadata();

}