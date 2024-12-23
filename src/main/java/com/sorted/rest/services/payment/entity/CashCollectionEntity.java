package com.sorted.rest.services.payment.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.payment.beans.CcMetadata;
import com.sorted.rest.services.payment.constants.PaymentConstants;
import com.sorted.rest.services.payment.constants.PaymentConstants.CashCollectionStatus;
import com.sorted.rest.services.payment.constants.PaymentConstants.EntityType;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name = PaymentConstants.CASH_COLLECTIONS_TABLE_NAME)
@DynamicUpdate
@Data
@Where(clause = "active = 1")
public class CashCollectionEntity extends BaseEntity {

	private static final long serialVersionUID = -7538803140039235801L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Long id;

	@Column
	private Date date;

	@Column
	private String slot;

	@Column(nullable = false)
	private String entityId;

	@Column
	@Enumerated(EnumType.STRING)
	private EntityType entityType = EntityType.STORE;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private CashCollectionStatus status;

	@Column
	private Double requestedAmount;

	@Column
	private Double collectedAmount;

	@Column
	private Double receivedAmount;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private CcMetadata metadata = new CcMetadata();

	@Column
	private String referenceId;

	@Column
	private String key;

	public static CashCollectionEntity newInstance() {
		CashCollectionEntity entity = new CashCollectionEntity();
		return entity;
	}
}