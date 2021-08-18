package org.egov.cpt.models;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.egov.cpt.models.calculation.Calculation;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A Object holds the basic data for a Property
 */
@ApiModel(description = "A Object holds the basic data for a Property")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2018-09-18T17:06:11.263+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Property {

	@JsonProperty("id")
	@Size(max = 256, message = "Id must be between 0 and 256 characters in length")
	private String id;

	@JsonProperty("transitNumber")
//	@Size(max = 4, message = "Transit number must be between 0 and 4 characters in length")
	private String transitNumber;

	@JsonProperty("tenantId")
	@Size(max = 256, message = "Tenant id must be between 0 and 256 characters in length")
	private String tenantId;

	@JsonProperty("colony")
	@Size(max = 256, message = "Colony must be between 0 and 256 characters in length")
	private String colony;

	@JsonProperty("masterDataState")
	@Size(max = 256, message = "Master data state must be between 0 and 256 characters in length")
	private String masterDataState;

	@JsonProperty("masterDataAction")
	@Size(max = 256, message = "Master data action must be between 0 and 256 characters in length")
	private String masterDataAction;

	@JsonProperty("assignee")
	@Builder.Default
	private List<String> assignee = null;

	@Size(max = 128, message = "Comment must be between 0 and 128 characters in length")
	@JsonProperty("comment")
	private String comment;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails;

	@Valid
	@JsonProperty("propertyDetails")
	private PropertyDetails propertyDetails;

	@Valid
	@JsonProperty("owners")
	private List<Owner> owners;

	@Valid
	@JsonProperty
	private List<DuplicateCopy> duplicateCopys;

	@Valid
	@JsonProperty
	private List<PropertyImages> propertyImages;

	@Valid
	@JsonProperty
	private List<NoticeGeneration> notices;

	@Valid
	@JsonProperty
	private List<MortgageApprovedGrantDetails> grantDetails;

	@Valid
	@JsonProperty
	private List<RentDemand> demands;

	@Valid
	@JsonProperty
	private List<RentDemand> inActiveDemands;

	@Valid
	@JsonProperty
	private List<RentPayment> payments;

	@Valid
	@JsonProperty
	private List<RentPayment> inActivePayments;

	@Valid
	@JsonProperty
	private List<RentCollection> rentCollections;

	@Valid
	@JsonProperty
	private RentAccount rentAccount;

	@Valid
	@JsonProperty
	private RentSummary rentSummary;

	@Valid
	@JsonProperty
	private Calculation calculation;

	public Property addDocumentItem(DuplicateCopy newDuplicateCopyItem) {
		if (this.duplicateCopys == null) {
			this.duplicateCopys = new ArrayList<>();
		}
		for (DuplicateCopy duplicateCopy : duplicateCopys) {
			if (duplicateCopy.getId().equalsIgnoreCase(newDuplicateCopyItem.getId())) {
				return this;
			}
		}
		this.duplicateCopys.add(newDuplicateCopyItem);
		return this;
	}

	public Property addOwnerItem(Owner newOwnerItem) {
		if (this.owners == null) {
			this.owners = new ArrayList<>();
		}
		for (Owner owner : owners) {
			if (owner.getId().equalsIgnoreCase(newOwnerItem.getId())) {
				return this;
			}
		}
		this.owners.add(newOwnerItem);
		return this;
	}

	public Property addPropertyImagesItem(PropertyImages newPropertyImagesItem) {
		if (this.propertyImages == null) {
			this.propertyImages = new ArrayList<>();
		}
		for (PropertyImages propertyImage : propertyImages) {
			if (propertyImage.getId().equalsIgnoreCase(newPropertyImagesItem.getId())) {
				return this;
			}
		}
		this.propertyImages.add(newPropertyImagesItem);
		return this;
	}

	public Property addNoticeItem(NoticeGeneration newNoticeItem) {
		if (this.notices == null) {
			this.notices = new ArrayList<>();
		}
		for (NoticeGeneration notice : notices) {
			if (notice.getId().equalsIgnoreCase(newNoticeItem.getId())) {
				return this;
			}
		}
		this.notices.add(newNoticeItem);
		return this;
	}

	public Property addGrantDetailItem(MortgageApprovedGrantDetails newGrantDetailItem) {
		if (this.grantDetails == null) {
			this.grantDetails = new ArrayList<>();
		}
		for (MortgageApprovedGrantDetails grantDetail : grantDetails) {
			if (grantDetail.getId().equalsIgnoreCase(newGrantDetailItem.getId())) {
				return this;
			}
		}
		this.grantDetails.add(newGrantDetailItem);
		return this;
	}

	public Property addDemandItem(RentDemand newDemandItem) {
		if (this.demands == null) {
			this.demands = new ArrayList<>();
		}
		for (RentDemand grantDetail : demands) {
			if (grantDetail.getId().equalsIgnoreCase(newDemandItem.getId())) {
				return this;
			}
		}
		this.demands.add(newDemandItem);
		return this;
	}

	public Property addPaymentItem(RentPayment newPaymentItem) {
		if (this.payments == null) {
			this.payments = new ArrayList<>();
		}
		for (RentPayment grantDetail : payments) {
			if (grantDetail.getId().equalsIgnoreCase(newPaymentItem.getId())) {
				return this;
			}
		}
		this.payments.add(newPaymentItem);
		return this;
	}

	public Property addCollectionItem(RentCollection newCollectionItem) {
		if (this.rentCollections == null) {
			this.rentCollections = new ArrayList<>();
		}
		for (RentCollection rentCollection : rentCollections) {
			if (rentCollection.getId().equalsIgnoreCase(newCollectionItem.getId())) {
				return this;
			}
		}
		this.rentCollections.add(newCollectionItem);
		return this;
	}

	@JsonProperty("pincode")
	@Size(min = 6, max = 6, message = "Pincode must be 6 digit in length")
	private String pincode;

	@JsonProperty("area")
	@Size(max = 256, message = "Area must be between 0 and 256 characters in length")
	private String area;

	/**
	 * Amount to be paid
	 */
	@JsonProperty("paymentAmount")
	private Double paymentAmount;

	/**
	 * Pending consumer code. This needs to be saved in the database for online
	 * payments.
	 */
	@JsonProperty("rentPaymentConsumerCode")
	@Size(max = 256, message = "Rent payment consumer code must be between 0 and 256 characters in length")
	private String rentPaymentConsumerCode;

	@JsonProperty("transactionId")
	@Size(max = 256, message = "Transaction id must be between 0 and 256 characters in length")
	private String transactionId;

	@JsonProperty("bankName")
	@Size(max = 256, message = "Bank name must be between 0 and 256 characters in length")
	private String bankName;
	
	@JsonProperty("paymentMode")
	private String paymentMode;
	
	@JsonProperty("transactionDate")
	private Long transactionDate;

	@JsonProperty("billingBusinessService")
	@Size(max = 256, message = "Billing business service must be between 0 and 256 characters in length")
	private String billingBusinessService;

	/**
	 * RENTED_PROPERTIES_COLONY_VIKAS_NAGAR.RENT
	 */
	public String getBillingBusinessService() {
		return String.format("RENTED_PROPERTIES_%s.RENT", this.getColony());
	}

	@Valid
	@JsonProperty
	private List<OfflinePaymentDetails> offlinePaymentDetails;

}