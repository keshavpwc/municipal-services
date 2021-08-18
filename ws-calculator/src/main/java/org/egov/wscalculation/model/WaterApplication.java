package org.egov.wscalculation.model;

import java.util.Objects;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-01-22T12:39:45.543+05:30[Asia/Kolkata]")
public class WaterApplication {
	@JsonProperty("id")
	private String id = null;

	@JsonProperty("applicationNo")
	private String applicationNo = null;
	
	@JsonProperty("activityType")
	private String activityType = null;

	@JsonProperty("applicationStatus")
	private String applicationStatus = null;

	@JsonProperty("action")
	private String action = null;

	@JsonProperty("comments")
	private String comments = null;

	public Boolean getIsFerruleApplicable() {
		return isFerruleApplicable;
	}

	public void setIsFerruleApplicable(Boolean isFerruleApplicable) {
		this.isFerruleApplicable = isFerruleApplicable;
	}

	public Double getSecurityCharge() {
		return securityCharge;
	}

	public void setSecurityCharge(Double securityCharge) {
		this.securityCharge = securityCharge;
	}

	@JsonProperty("isFerruleApplicable")
	private Boolean isFerruleApplicable = true;

	@JsonProperty("isMeterStolen")
	private Boolean isMeterStolen = false;
	
	@JsonProperty("securityCharge")
	private Double securityCharge = null;

	@JsonProperty("additionalCharges")
	private Double additionalCharges = null;

	@JsonProperty("constructionCharges")
	private Double constructionCharges = null;

	@JsonProperty("paymentMode")
	private String paymentMode;
	
	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

	  @JsonProperty("totalAmountPaid")
	    private String totalAmountPaid;
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WaterApplication waterApplication = (WaterApplication) o;
		return Objects.equals(this.id, waterApplication.id) && Objects.equals(this.applicationNo, waterApplication.applicationNo)
				&& Objects.equals(this.applicationStatus, waterApplication.applicationStatus)
				&& Objects.equals(this.action, waterApplication.action)
				&& Objects.equals(this.comments, waterApplication.comments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, applicationNo, applicationStatus, action, comments);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class waterApplication {\n");
		sb.append("    id: ").append(toIndentedString(id)).append("\n");
		sb.append("    applicationNo: ").append(toIndentedString(applicationNo)).append("\n");
		sb.append("    applicationStatus: ").append(toIndentedString(applicationStatus)).append("\n");
		sb.append("    action: ").append(toIndentedString(action)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getApplicationNo() {
		return applicationNo;
	}

	public void setApplicationNo(String applicationNo) {
		this.applicationNo = applicationNo;
	}

	public String getActivityType() {
		return activityType;
	}

	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}

	public String getApplicationStatus() {
		return applicationStatus;
	}

	public void setApplicationStatus(String applicationStatus) {
		this.applicationStatus = applicationStatus;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public AuditDetails getAuditDetails() {
		return auditDetails;
	}

	public void setAuditDetails(AuditDetails auditDetails) {
		this.auditDetails = auditDetails;
	}

	public String getTotalAmountPaid() {
		return totalAmountPaid;
	}

	public void setTotalAmountPaid(String totalAmountPaid) {
		this.totalAmountPaid = totalAmountPaid;
	}

	public Double getAdditionalCharges() {
		return additionalCharges;
	}

	public void setAdditionalCharges(Double additionalCharges) {
		this.additionalCharges = additionalCharges;
	}

	public Double getConstructionCharges() {
		return constructionCharges;
	}

	public void setConstructionCharges(Double constructionCharges) {
		this.constructionCharges = constructionCharges;
	}

	public String getPaymentMode() {
		return paymentMode;
	}

	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}

	public Boolean getIsMeterStolen() {
		return isMeterStolen;
	}

	public void setIsMeterStolen(Boolean isMeterStolen) {
		this.isMeterStolen = isMeterStolen;
	}
}
