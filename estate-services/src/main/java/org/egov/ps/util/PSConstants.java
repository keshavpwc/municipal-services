package org.egov.ps.util;

public class PSConstants {

	public static final String TENANT_ID = "ch.chandigarh";

	public static final String PM_DRAFTED = "ES_DRAFTED";

	public static final String PM_APPROVED = "ES_APPROVED";

	public static final String ES_APPROVED = "ES_PM_APPROVED";

	public static final String ES_PM_MM_APPROVED = "ES_PM_MM_APPROVED";

	public static final String ES_PM_EB_APPROVED = "ES_PM_EB_APPROVED";

	public static final String ES_PM_LEASEHOLD = "PROPERTY_TYPE.LEASEHOLD";

	public static final String ES_WF_DOCS = "WF_DOCS";

	public static final String ES_REFUNDED = "ES_REFUNDED";

	public static final String ESTATE_BRANCH = "ESTATE_BRANCH";

	public static final String BUILDING_BRANCH = "BUILDING_BRANCH";

	public static final String MANI_MAJRA = "MANI_MAJRA";

	public static final String APPLICATION_ESTATE_BRANCH = "EstateBranch";

	public static final String APPLICATION_BUILDING_BRANCH = "BuildingBranch";

	public static final String APPLICATION_MANI_MAJRA = "ManiMajra";

	public static final String MM_JANTA_READY_MARKET = "PROPERTY_TYPE.JANTA_READY_MARKET";

	public static final String MM_PUNJAB_AGRO_JUICE = "PROPERTY_TYPE.PUNJAB_AGRO_JUICE";

	public static final String JSONPATH_PROPERTY_SERVICE = "$.MdmsRes.OwnershipTransferSaleDeed";

	public static final String MDMS_PS_MODULE_NAME = "EstateServices";

	public static final String ES_MODULE_PREFIX = "ES_";

	public static final String MDMS_PS_FIELDS = "EstateBranch_OwnershipTransfer_SaleDeed";

	public static final String MDMS_PS_CODE_FILTER = "$.*.code";

	public static final String MDMS_PS_FIELD_FILTER = "[0].fields";

	public static final String MDMS_PS_FEES_FILTER = "[0].fees";

	public static final String MDMS_PS_MM_JANATHA_READY_MONTHLY_FILTER = "[0].janathaReadyMarketManiMajraMonthly";

	public static final String MDMS_PS_MM_JANATHA_READY_ANNUALLY_FILTER = "[0].janathaReadyMarketManiMajraAnnually";

	public static final String MDMS_PS_MM_PUNJAB_JUICE_SITE_FILTER = "[0].punjabAgroJuiceSite";

	public static final String MDMS_PS_FEE_GST_FILTER = "[0].feePercentGST";

	public static final String MDMS_PS_NOTIFICATIONS_FILTER = "[0].notifications";

	public static final String MDMS_DOCUMENT_FIELD_FILTER = ".*.documentList.*";

	public static final String EB_OT_SD = "EstateBranch_OwnershipTransfer_SaleDeed";

	public static final String EB_OT_RW = "EstateBranch_OwnershipTransfer_RegisteredWill";

	public static final String EB_OC_LH_FH = "EstateBranch_OtherCitizen_LeaseholdToFreehold";

	public static final String EB_OC_SCF_SCO = "EstateBranch_OtherCitizen_ScfToSco";

	public static final String EB_IS_AOS = "EstateBranch_InternalServices_AllotmentOfSite";

	public static final String MDMS_PS_MORTGAGE_FILTER = ".*.documentList.*";

	public static final String EM_ACTION_APPROVE = "Approve";

	public static final String EM_STATE_PENDING_DA_FEE = "PENDING_DA_FEE";

	public static final String TAX_HEAD_CODE_APPLICATION_CHARGE = "APPLICATION";
	
	public static final String TAX_HEAD_CODE_TRANSFER = "TRANSFER";
	
	public static final String TAX_HEAD_CODE_GST = "GST";
	
	public static final String TAX_HEAD_CODE_INSPECTION = "INSPECTION";
	
	public static final String TAX_HEAD_CODE_SECURITY = "SECURITY";
	
	public static final String TAX_HEAD_CODE_EXTENSION = "EXTENSION";
	
	public static final String TAX_HEAD_CODE_DOCUMENTCOPYING = "DOCUMENTCOPYING";
	
	public static final String TAX_HEAD_CODE_ALLOTMENT = "ALLOTMENT";
	
	public static final String TAX_HEAD_CODE_CONVERSION = "CONVERSION";
	
	public static final String TAX_HEAD_CODE_PROPERTYTRANSFER = "PROPERTYTRANSFER";
	

	public static final String ESTATE_SERVICE = "ESTATE_SERVICE";

	public static final String ES_DRAFT = "DRAFT";

	public static final String ES_APPROVE = "APPROVE";

	public static final String ROLE_EMPLOYEE = "EMPLOYEE";

	public static final String ROLE_CITIZEN = "CITIZEN";

	public static final String ACTION_PAY = "PAY";

	public static final String PROPERTY_MASTER = "PROPERTY_MASTER";

	public static final String MONTHLY_DEMAND = "Monthly";

	public static final String EB_ADD_COURT_CASES = "ADD_COURT_CASES";

	public static final String EB_ALLOCATION_TYPE_AUCTION = "ALLOCATION_TYPE.AUCTION";

	public static final String MM_PROPERTY_RENT_MASTER_NAME = "ManiMajra_Property_Rent_Config";

	public static final String BUSINESS_SERVICE_EB_RENT = "ESTATE_SERVICE_ESTATE_BRANCH.PROPERTY_MASTER";
	public static final String BUSINESS_SERVICE_BB_RENT = "ESTATE_SERVICE_BUILDING_BRANCH.PROPERTY_MASTER";
	public static final String BUSINESS_SERVICE_MB_RENT = "ESTATE_SERVICE_MANI_MAJRA.PROPERTY_MASTER";

	public static final String BUSINESS_SERVICE_EB_PENALTY = "ESTATE_SERVICE_ESTATE_BRANCH.PROPERTY_VIOLATION";
	public static final String BUSINESS_SERVICE_BB_PENALTY = "ESTATE_SERVICE_BUILDING_BRANCH.PROPERTY_VIOLATION";
	public static final String BUSINESS_SERVICE_MB_PENALTY = "ESTATE_SERVICE_MANI_MAJRA.PROPERTY_VIOLATION";

	public static final String BUSINESS_SERVICE_EB_EXTENSION_FEE = "ESTATE_SERVICE_ESTATE_BRANCH.EXTENSION_FEE";
	public static final String BUSINESS_SERVICE_BB_EXTENSION_FEE = "ESTATE_SERVICE_BUILDING_BRANCH.EXTENSION_FEE";
	public static final String BUSINESS_SERVICE_MB_EXTENSION_FEE = "ESTATE_SERVICE_MANI_MAJRA.EXTENSION_FEE";

	public static final String BUSINESS_SERVICE_EB_SECURITY_DEPOSIT = "ESTATE_SERVICE_ESTATE_BRANCH.SECURITY_DEPOSIT";
	public static final String BUSINESS_SERVICE_BB_SECURITY_DEPOSIT = "ESTATE_SERVICE_BUILDING_BRANCH.SECURITY_DEPOSIT";
	public static final String BUSINESS_SERVICE_MB_SECURITY_DEPOSIT = "ESTATE_SERVICE_MANI_MAJRA.SECURITY_DEPOSIT";

	public static final String LOCALIZATION_MSGS_JSONPATH = "$.messages";
	public static final String LOCALIZATION_MODULE = "rainmaker-es";
	public static final String LOCALIZATION_LOCALE = "en_IN";

	public static final String PROPERTY_VIOLATION = "PROPERTY_VIOLATION";

	public static final String EXTENSION_FEE = "EXTENSION_FEE";

	public static final String SECURITY_DEPOSIT = "SECURITY_DEPOSIT";

	// User event
	public static final String USREVENTS_EVENT_TYPE = "SYSTEMGENERATED";
	public static final String USREVENTS_EVENT_NAME = "Estate Module";
	public static final String USREVENTS_EVENT_POSTEDBY = "SYSTEM-EST";

	public static final String APPLICATION_TYPE_NDC = "NDC";
	public static final String PENDING_SO_APPROVAL = "ES_PENDING_SO_APPROVAL";

	public static final String ES_EB_PM_PENDING_APPROVAL = "ES_EB_PM_PENDING_APPROVAL";

	public static final String PENDING_PM_MM_APPROVAL = "ES_PM_MM_PENDING_APPROVAL";
	public static final String PENDING_MM_SO_APPROVAL = "ES_MM_PENDING_SO_APPROVAL";

	public static final String MONTHLY = "Monthly";
	public static final String ANNUALLY = "Annually";

	public static final String MODE_GENERATED = "Generated";

	public static final String RELATION_OPD = "offline";

	public static final String PROPERTY_RENT = "PROPERTY_RENT";
	public static final String PENALTY = "PENALTY";
	public static final String PROPERTY_PAYMENT_PAYER = "PROPERTY_PAYMENT_PAYER";
	public static final String PROPERTY_PAYMENT_OWNER = "PROPERTY_PAYMENT_OWNER";
	public static final String PROPERTY_RENT_MDMS_MODULE = "Property_Rent_Fee";

	public static final String PAYMENT_MODE_ONLINE = "ONLINE";
	public static final String PAYMENT_MODE_OFFLINE = "CASH";

	public static final String PAYMENT_TYPE_RENT = "rent";
	public static final String PAYMENT_TYPE_EF = "extention fee";
	public static final String PAYMENT_TYPE_SD = "security deposit";
	public static final String PAYMENT_TYPE_PENALTY = "penalty";

	public static final String OWNERSHIP_TRANSFER = "OwnershipTransfer";
	public static final String MODE_OF_TRANSFER = "MODE_OF_TRANSFER";

	public static final double GST_INTEREST_RATE = 18.0;

	public static final String RELATION_BIDDER = "bidder";

	public static final String ACTION_SUBMIT = "SUBMIT";

	public static final String NOC = "NOC";

	public static final String BB_NOC_PENDING_JE_VERIFICATION = "PENDING_JE_VERIFICATION";

	public static final String BB_NOC_PENDING_DRAFSMAN_CALCULATION = "PENDING_DRAFSMAN_CALCULATION";

	public static final String BB_NOC_PENDING_AC_APPROVAL = "PENDING_AC_APPROVAL";

	public static final String BB_NOC_DUMMY_FILENO = "BBNOC-1";
	
	public static final String MM_PROPERTY_TYPE_OTHER = "PROPERTY_TYPE.OTHERS";
	
	public static final String MAIL_SIGNATURE = "<br/><br/><b>Regards,<br/>Municipal Corporation of Chandigarh<b>";

}
