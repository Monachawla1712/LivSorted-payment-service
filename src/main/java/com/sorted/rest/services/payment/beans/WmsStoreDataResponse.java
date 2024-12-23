package com.sorted.rest.services.payment.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * StoreBean related information
 *
 * @author mohit
 */
@ApiModel(description = "StoreBean")
@Data
public class WmsStoreDataResponse implements Serializable {

	private static final long serialVersionUID = 493517626137997750L;

	private Integer id;

	private String extStoreId;

	private String name;

	private String address;

	private String active;

	private String storeType;

	private Integer isSrpStore;

	private Integer prevId;

	private Integer whId;

	public static WmsStoreDataResponse newInstance() {
		WmsStoreDataResponse entity = new WmsStoreDataResponse();
		return entity;
	}
}