package org.egov.waterconnection.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WCConstants {

	private WCConstants() {

	}
	
	public static final String JSONPATH_ROOT = "$.MdmsRes.ws-services-masters";
	
	public static final String TAX_JSONPATH_ROOT = "$.MdmsRes.ws-services-calculation";
	
	public static final String JSONPATH_CODE_CONNECTION_CATEGORY = "connectionCategory.code";
	
	public static final String JSONPATH_CODE_CONNECTION_TYPE = "connectionType.code";
	
	public static final String JSONPATH_CODE_WATER_SOURCE= "waterSource.code";

	public static final String MDMS_WC_MOD_NAME = "ws-services-masters";
	
	public static final String WS_TAX_MODULE = "ws-services-calculation";

	public static final String MDMS_WC_Connection_Category = "connectionCategory";

	public static final String MDMS_WC_Connection_Type = "connectionType";

	public static final String MDMS_WC_Water_Source = "waterSource";

	public static final String INVALID_CONNECTION_CATEGORY = "Invalid Connection Category";

	public static final String INVALID_CONNECTION_TYPE = "Invalid Connection Type";
	
	public static final String METERED_CONNECTION = "Metered";
	
	  // WS actions

    public static final String ACTION_INITIATE = "INITIATE";

    public static final String ACTION_APPLY  = "APPLY";

    public static final String ACTIVATE_CONNECTION  = "ACTIVATE_REGULAR_CONNECTION";

    public static final String ACTION_REJECT  = "REJECT";

    public static final String ACTION_CANCEL  = "CANCEL";

    public static final String ACTION_PAY  = "PAY";
    
    public static final String ACTION_PAY_FOR_REGULAR_CONNECTION  = "PAY_FOR_REGULAR_CONNECTION";
    
    public static final String ACTION_PAY_FOR_TEMPORARY_CONNECTION  = "PAY_FOR_TEMPORARY_CONNECTION";

    public static final String ACTION_ACTIVATE_TEMP_CONNECTION  = "ACTIVATE_TEMPORARY_CONNECTION";


    public static final String STATUS_INITIATED = "INITIATED";

    public static final String STATUS_APPLIED  = "APPLIED";

    public static final String STATUS_APPROVED  = "CONNECTION_ACTIVATED";

    public static final String STATUS_REJECTED  = "REJECTED";

    public static final String STATUS_FIELDINSPECTION  = "FIELDINSPECTION";

    public static final String STATUS_CANCELLED  = "CANCELLED";

    public static final String STATUS_PAID  = "PAID";
    
    public static final String STATUS_PENDING_FOR_PAYMENT = "PENDING_FOR_PAYMENT";
    
    public static final String NOTIFICATION_LOCALE = "en_IN";

	public static final String MODULE = "rainmaker-ws";

	public static final String SMS_RECIEVER_MASTER = "SMSReceiver";
	
	public static final String SERVICE_FIELD_VALUE_WS = "WS";
	
	public static final String SERVICE_FIELD_VALUE_NOTIFICATION = "Water";
	
	
	//Application Status For Notification
	public static final String INITIATE_INITIATED = "SUBMIT_APPLICATION_PENDING_FOR_DOCUMENT_VERIFICATION";

	public static final String REJECT_REJECTED = "REJECT_REJECTED";

	public static final String SEND_BACK_TO_CITIZEN_PENDING_FOR_CITIZEN_ACTION = "SEND_BACK_TO_CITIZEN_PENDING_FOR_CITIZEN_ACTION";

	public static final String SEND_BACK_FOR_DO_PENDING_FOR_DOCUMENT_VERIFICATION = "SEND_BACK_FOR_DOCUMENT_VERIFICATION_PENDING_FOR_DOCUMENT_VERIFICATION";

	public static final String SEND_BACK_PENDING_FOR_FIELD_INSPECTION = "SEND_BACK_FOR_FIELD_INSPECTION_PENDING_FOR_FIELD_INSPECTION";

	public static final String VERIFY_AND_FORWORD_PENDING_FOR_FIELD_INSPECTION = "VERIFY_AND_FORWARD_PENDING_FOR_FIELD_INSPECTION";

	public static final String VERIFY_AND_FORWARD_PENDING_APPROVAL_FOR_CONNECTION = "VERIFY_AND_FORWARD_PENDING_APPROVAL_FOR_CONNECTION";

	public static final String APPROVE_FOR_CONNECTION_PENDING_FOR_PAYMENT = "APPROVE_FOR_CONNECTION_PENDING_FOR_PAYMENT";

	public static final String PAY_PENDING_FOR_CONNECTION_ACTIVATION = "PAY_PENDING_FOR_CONNECTION_ACTIVATION";
	
	public static final String ACTIVATE_CONNECTION_CONNECTION_ACTIVATED = "ACTIVATE_CONNECTION_CONNECTION_ACTIVATED";
	
	public static final String EDIT_PENDING_FOR_DOCUMENT_VERIFICATION = "EDIT_PENDING_FOR_DOCUMENT_VERIFICATION";
	
	public static final String EDIT_PENDING_FOR_FIELD_INSPECTION = "EDIT_PENDING_FOR_FIELD_INSPECTION";
	
	public static final String ACTION_APPROVE_CONNECTION_CONST = "APPROVE_FOR_CONNECTION";
	
	public static final String ACTIVATE_CONNECTION_CONST = "ACTIVATE_CONNECTION";
	
	public static final String ACTION_APPLY_SECURITY_DEPOSIT = "APPLY_SECURITY_DEPOSIT";
	
	public static final String ACTION_APPROVE_FOR_CONNECTION_CONVERSION = "APPROVE_FOR_CONNECTION_CONVERSION";
	
	public static final String ACTION_APPROVE_FOR_CONNECTION_RENAME = "APPROVE_FOR_CONNECTION_RENAME";
	
	public static final String ACTION_APPROVE_FOR_CONNECTION_DISCONNECTION = "APPROVE_FOR_CONNECTION_DISCONNECTION";
	
	public static final String ACTION_SEND_BACK_FOR_ADDON_PAYMENT = "SEND_BACK_FOR_ADDON_PAYMENT";
	
	public static final String ACTION_APPROVE_ACTIVATE_CONNECTION = "APPROVE_ACTIVATE_CONNECTION";
	
	
	public static final List<String> NOTIFICATION_ENABLE_FOR_STATUS = Collections
			.unmodifiableList(Arrays.asList(INITIATE_INITIATED, REJECT_REJECTED,
					SEND_BACK_TO_CITIZEN_PENDING_FOR_CITIZEN_ACTION, SEND_BACK_FOR_DO_PENDING_FOR_DOCUMENT_VERIFICATION,
					SEND_BACK_PENDING_FOR_FIELD_INSPECTION, VERIFY_AND_FORWORD_PENDING_FOR_FIELD_INSPECTION,
					VERIFY_AND_FORWARD_PENDING_APPROVAL_FOR_CONNECTION, APPROVE_FOR_CONNECTION_PENDING_FOR_PAYMENT,
					PAY_PENDING_FOR_CONNECTION_ACTIVATION, ACTIVATE_CONNECTION_CONNECTION_ACTIVATED,
					EDIT_PENDING_FOR_DOCUMENT_VERIFICATION, EDIT_PENDING_FOR_FIELD_INSPECTION));
	
	public static final String  USREVENTS_EVENT_TYPE = "SYSTEMGENERATED";
	
	public static final String  USREVENTS_EVENT_NAME = "WATER CONNECTION";
	
	public static final String  USREVENTS_EVENT_POSTEDBY = "SYSTEM-WS";
	
	public static final String VARIABLE_WFDOCUMENTS = "documents";

    public static final String VARIABLE_PLUMBER = "plumberInfo";
    
    public static final String WC_ROADTYPE_MASTER = "RoadType";
	
	public static final List<String> FIELDS_TO_CHECK = Collections
			.unmodifiableList(Arrays.asList("rainWaterHarvesting", "waterSource", "meterId", "meterInstallationDate",
					"proposedPipeSize", "proposedTaps", "pipeSize", "noOfTaps", "oldConnectionNo", "roadType",
					"roadCuttingArea", "connectionExecutionDate", "connectionCategory", "connectionType",
					"documentType", "fileStoreId", "licenseNo"));

	public static final String WS_EDIT_SMS = "WS_EDIT_SMS_MESSAGE";
	
	public static final String WS_EDIT_IN_APP = "WS_EDIT_IN_APP_MESSAGE";
	
	public static final List<String> DOC_CHECK_APP_STATUS_NOTIFY = Collections
	.unmodifiableList(Arrays.asList("PENDING_FOR_TUBEWELL_CONNECTION_ACTIVATION", "PENDING_FOR_CONNECTION_TARIFF_CHANGE", "PENDING_FOR_CONNECTION_REACTIVATION", "PENDING_FOR_CONNECTION_HOLDER_CHANGE",
			"PENDING_FOR_TEMPORARY_CONNECTION_CLOSE", "PENDING_FOR_CONNECTION_CLOSE", "PENDING_FOR_CONNECTION_EXTENSION_REGULAR", "PENDING_FOR_CONNECTION_ACTIVATION", "PENDING_FOR_CONNECTION_ACTIVATION"));

	public static final List<String> SUBMIT_ACTION_FROM_CITIZEN_MESSAGE = Collections
	.unmodifiableList(Arrays.asList("SUBMIT_APPLICATION", "RESUBMIT_APPLICATION", "SUBMIT_ROADCUT_NOC"));

	public static final List<String> DOCUMENT_PENDING_FROM_CITIZEN_MESSAGE = Collections
	.unmodifiableList(Arrays.asList("SEND_BACK_TO_CITIZEN_FOR_ROADCUT_NOC", "SEND_BACK_TO_CITIZEN"));
	
	public static final String WS_PAYMENT_MESSAGE_NOTIFICATION ="Dear <Owner Name> , Your payment for application <Application number> has been been succesfully recorded. Please submit previously filled physical copies of document at concerned Public Health Division Office,Chandigarh  <Action Button>Download Application</Action Button>";// Download your receipt using this link <receipt download link>.
	public static final String WS_REJECT_MESSAGE_NOTIFICATION ="Dear <Owner Name> , Your request for application <Application number> has been been rejected.  <Action Button>Download Application</Action Button>";
	public static final String WS_RESUBMIT_MESSAGE_NOTIFICATION ="Dear <Owner Name> , Your request for application <Application number> is incomplete.Please re-submit the documents.  <Action Button>Download Application</Action Button>";
	public static final String WS_SUBMIT_MESSAGE_NOTIFICATION ="Dear <Owner Name> , Your request for application <Application number> has been been succesfully submitted.  <Action Button>Download Application</Action Button>";
    public static final String DEFAULT_OBJECT_MODIFIED_SMS_MSG = "Dear <Owner Name>, Your Application <Application number>  for a New <Service> Connection has been edited. For more details, please log in to <mseva URL> or download <mseva app link>.";

    public static final String DEFAULT_OBJECT_MODIFIED_APP_MSG = "Dear <Owner Name>, Your Application <Application number>  for a New <Service> Connection has been edited. Click here for more details <View History Link>.";
   
    public static final String IDGEN_ERROR_CONST = "IDGEN ERROR";
    
    public static final String ADHOC_PENALTY = "adhocPenalty";
    
    public static final String ADHOC_REBATE = "adhocRebate";

	public static final String ADHOC_PENALTY_REASON = "adhocPenaltyReason";

	public static final String ADHOC_PENALTY_COMMENT = "adhocPenaltyComment";

	public static final String ADHOC_REBATE_REASON = "adhocRebateReason";

	public static final String ADHOC_REBATE_COMMENT = "adhocRebateComment";
	
	public static final String INITIAL_METER_READING_CONST = "initialMeterReading";
	
	public static final String LAST_METER_READING_CONST = "lastMeterReading";
	
	public static final String SUBMIT_APPLICATION_CONST = "SUBMIT_APPLICATION";
	
	public static final String DETAILS_PROVIDED_BY = "detailsProvidedBy";
	
	public static final String APP_CREATED_DATE = "appCreatedDate";
	
	public static final String ESTIMATION_FILESTORE_ID = "estimationFileStoreId";
	
	public static final String SANCTION_LETTER_FILESTORE_ID = "sanctionFileStoreId";
	
	public static final String ESTIMATION_DATE_CONST = "estimationLetterDate";
	
	public static final List<String> ADDITIONAL_OBJ_CONSTANT = Collections
			.unmodifiableList(Arrays.asList(ADHOC_PENALTY, ADHOC_REBATE, ADHOC_PENALTY_REASON, ADHOC_PENALTY_COMMENT,
					ADHOC_REBATE_REASON, ADHOC_REBATE_COMMENT, INITIAL_METER_READING_CONST,LAST_METER_READING_CONST, DETAILS_PROVIDED_BY,
					APP_CREATED_DATE, ESTIMATION_FILESTORE_ID, SANCTION_LETTER_FILESTORE_ID, ESTIMATION_DATE_CONST));

	public static final List<String> EDIT_NOTIFICATION_STATE = Collections.unmodifiableList(Arrays.asList(ACTION_INITIATE, SUBMIT_APPLICATION_CONST, ACTION_PAY));
	
	public static final List<String> IGNORE_CLASS_ADDED = Collections.unmodifiableList(Arrays.asList("PlumberInfo"));

	public static final List<String> ACTIVITY_TYPE_81 = Collections.unmodifiableList(Arrays.asList("CONNECTION_CONVERSION","UPDATE_CONNECTION_HOLDER_INFO","APPLY_FOR_TEMPORARY_REGULAR_CONNECTION"));

	public static final List<String> ACTIVITY_TYPE_82 = Collections.unmodifiableList(Arrays.asList("UPDATE_METER_INFO","REACTIVATE_CONNECTION"));

	public static final List<String> ACTIVITY_TYPE_NEW_CONN = Collections.unmodifiableList(Arrays.asList("NEW_WS_CONNECTION"));
	
	public static final String SELF = "SELF";
	
	public static final String PDF_APPLICATION_KEY = "ws-applicationwater";
	
	public static final String PDF_ESTIMATION_KEY = "ws-estimationnotice";
	
	public static final String PDF_SANCTION_KEY = "ws-sanctionletter";
	
	public static final String PENDING_FOR_CONNECTION_ACTIVATION = "PENDING_FOR_CONNECTION_ACTIVATION";
	
	public static final long DAYS_CONST= 86400000l;
	
	public static final String BILLING_PERIOD = "billingPeriod";
	
	public static final String JSONPATH_ROOT_FOR_BILLING = "$.MdmsRes.ws-services-masters.billingPeriod";
	
	public static final String  BILLING_PERIOD_MASTER = "Billing_Period_Master";
	
	public static final String QUARTERLY_BILLING_CONST = "quarterly";

	public static final String MONTHLY_BILLING_CONST = "monthly";

	public static final String BILLING_CYCLE_STRING = "billingCycle";
	
	public static final String APPROVE_CONNECTION = "APPROVE_CONNECTION";
	
	// Used to differentiate the type of request which is processing
	public static final int CREATE_APPLICATION = 0;
	public static final int UPDATE_APPLICATION = 1;
	public static final int MODIFY_CONNECTION =  2;

	public static final String WATER_SERVICE_BUSINESS_ID = "WS";

	public static final String NEW_WATER_CONNECTION ="NEW_WATER_CONNECTION";
	
	public static final String MODIFY_WATER_CONNECTION = "MODIFY_WATER_CONNECTION";

	public static final String PAYMENT_NOTIFICATION_APP = "WS_PAYMENT_NOTIFICATION_APP";

	public static final String PAYMENT_NOTIFICATION_SMS = "WS_PAYMENT_NOTIFICATION_SMS";
	
	public static final String APPLICATION_TYPE_TEMPORARY = "TEMPORARY";
	
	public static final String APPLICATION_TYPE_REGULAR = "REGULAR";
	
	public static final String APPLICATION_PROPERTY_TYPE_DOMESTIC = "DOMESTIC";
	public static final String APPLICATION_PROPERTY_TYPE_COMMERCIAL = "COMMERCIAL";

	//Activity TYpe
	public static final String WS_NEWCONNECTION = "NEW_WS_CONNECTION";

	public static final String WS_PERMANENT_DISCONNECTION = "PERMANENT_DISCONNECTION";
	
	public static final String WS_TEMPORARY_DISCONNECTION = "TEMPORARY_DISCONNECTION";
	
	public static final String WS_REACTIVATE = "REACTIVATE_CONNECTION";

	public static final String WS_CHANGE_OWNER_INFO = "UPDATE_CONNECTION_HOLDER_INFO";

	public static final String WS_CONVERSION = "CONNECTION_CONVERSION";

	public static final String WS_APPLY_FOR_REGULAR_CON = "APPLY_FOR_REGULAR_INFO";
	
	public static final String WS_NEW_TUBEWELL_CONNECTION = "NEW_TUBEWELL_CONNECTION";

	public static final String WS_UPDATE_METER_INFO = "UPDATE_METER_INFO";
	
	public static final String STATUS_PENDING_FOR_REGULAR = "PENDING_FOR_REGULAR_CONNECTION";
	
	public static final String WS_ACTION_REACTIVATION = "APPLY_CONNECTION_REACTIVATION";

	public static final String ACTION_APPLY_FOR_REGULAR_CONNECTION = "APPLY_FOR_REGULAR_CONNECTION";

	public static final String WS_BILLING_FILENAME = "billingData.txt";

	public static final String WS_CONNECTION_FILENAME = "connectionData.txt";

	public static final String CONNECTION_EXCHANGE = "CONNECTION";
	
	public static final String ADVICE_EXCHANGE = "ADVICE";
	
	public static final String WS_ADVICE_FILENAME = "adviceData.txt";
	
	public static final String WS_APPLY_FOR_TEMPORARY_CON_BILLING = "APPLY_FOR_TEMPORARY_CONNECTION_BILLING";
	
	public static final String WS_APPLY_FOR_TEMPORARY_CON = "APPLY_FOR_TEMPORARY_CONNECTION";

	public static final String WS_APPLY_FOR_TEMP_TEMP_CON = "APPLY_FOR_TEMPORARY_TEMPORARY_CONNECTION";

	public static final String WS_APPLY_FOR_TEMP_REGULAR_CON = "APPLY_FOR_TEMPORARY_REGULAR_CONNECTION";

	public static final String ACTION_VERIFY_AND_FORWARD_PAYMENT = "VERIFY_AND_FORWARD_FOR_PAYMENT";

	public static final Set<String> APPROVED_ACTIONS =  Stream.of("TUBEWELL_CONNECTION_ACTIVATED", "CONNECTION_TARIFF_CHANGED","CONNECTION_REACTIVATED","CONNECTION_UPDATED","TEMPORARY_CONNECTION_CLOSED","CONNECTION_CLOSED","CONNECTION_EXTENDED","CONNECTION_TYPE_CHANGED","CONNECTION_ACTIVATED").collect(Collectors.toSet());
	public static final String WS_METER_TESTING="WS_METER_TESTING";
}
