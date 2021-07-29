package org.egov.waterconnection.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.model.Property;
import org.egov.waterconnection.model.WaterConnectionRequest;
import org.egov.waterconnection.model.workflow.ProcessInstance;
import org.egov.waterconnection.model.workflow.ProcessInstanceRequest;
import org.egov.waterconnection.model.workflow.ProcessInstanceResponse;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WorkflowIntegrator {

	private static final String MODULENAMEVALUE = "WS";

	@Autowired
	private WSConfiguration config;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RestTemplate rest;

	@Autowired
	private WaterServicesUtil wsUtil;

	/**
	 * Method to integrate with workflow
	 *
	 * takes the water connection request as parameter constructs the work-flow
	 * request
	 *
	 * and sets the resultant status from wf-response back to water-connection
	 * request object
	 *
	 * @param waterConnectionRequest
	 */
	public void callWorkFlow(WaterConnectionRequest waterConnectionRequest, Property property) {

		String activityType = waterConnectionRequest.getWaterConnection().getActivityType();
		String businessService = getBusinessService(activityType);
		if (waterConnectionRequest.getWaterConnection().getProcessInstance().getAction()
				.equalsIgnoreCase(WCConstants.ACTION_PAY)) {
			JSONObject obj = new JSONObject();
			if (activityType.equalsIgnoreCase(WCConstants.WS_CONVERSION)) {
				obj.put("role", "WS_SUPERINTENDENT_" + waterConnectionRequest.getWaterConnection().getSubdiv());
			} else {
				obj.put("role", "WS_JE_" + waterConnectionRequest.getWaterConnection().getSubdiv());
			}
			waterConnectionRequest.getWaterConnection().getProcessInstance().setAdditionalDetails(obj);
		}
		ProcessInstance processInstance = ProcessInstance.builder()
				.businessId(waterConnectionRequest.getWaterConnection().getApplicationNo())
				.tenantId(property.getTenantId()).businessService(businessService).moduleName(MODULENAMEVALUE)
				.additionalDetails(
						waterConnectionRequest.getWaterConnection().getProcessInstance().getAdditionalDetails())
				.action(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction()).build();

		if (!StringUtils.isEmpty(waterConnectionRequest.getWaterConnection().getProcessInstance())) {
			if ((waterConnectionRequest.getWaterConnection().getProcessInstance().getAssignee()) != null) {
				processInstance
						.setAssignee(waterConnectionRequest.getWaterConnection().getProcessInstance().getAssignee());
			}
			if (!CollectionUtils
					.isEmpty(waterConnectionRequest.getWaterConnection().getProcessInstance().getDocuments())) {
				processInstance
						.setDocuments(waterConnectionRequest.getWaterConnection().getProcessInstance().getDocuments());
			}
			if (!StringUtils.isEmpty(waterConnectionRequest.getWaterConnection().getProcessInstance().getComment())) {
				processInstance
						.setComment(waterConnectionRequest.getWaterConnection().getProcessInstance().getComment());
			}

		}
		try {
			log.info("Process Instance request is : " + mapper.writeValueAsString(processInstance));
		} catch (JsonProcessingException e1) {
			log.error("Failed to log ProcessInstance : ", e1);
		}
		List<ProcessInstance> processInstances = new ArrayList<>();
		processInstances.add(processInstance);
		ProcessInstanceResponse processInstanceResponse = null;
		try {
			processInstanceResponse = mapper.convertValue(
					rest.postForObject(config.getWfHost().concat(config.getWfTransitionPath()),
							ProcessInstanceRequest.builder().requestInfo(waterConnectionRequest.getRequestInfo())
									.processInstances(processInstances).build(),
							Map.class),
					ProcessInstanceResponse.class);

		} catch (HttpClientErrorException e) {
			/*
			 * extracting message from client error exception
			 */
			DocumentContext responseContext = JsonPath.parse(e.getResponseBodyAsString());
			List<Object> errros = null;
			try {
				errros = responseContext.read("$.Errors");
			} catch (PathNotFoundException pnfe) {
				StringBuilder builder = new StringBuilder();
				builder.append(" Unable to read the json path in error object : ").append(pnfe.getMessage());
				log.error("EG_WS_WF_ERROR_KEY_NOT_FOUND", builder.toString());
				throw new CustomException("EG_WS_WF_ERROR_KEY_NOT_FOUND", builder.toString());
			}
			throw new CustomException("EG_WF_ERROR", errros.toString());
		} catch (Exception e) {
			throw new CustomException("EG_WF_ERROR",
					" Exception occured while integrating with workflow : " + e.getMessage());
		}

		/*
		 * on success result from work-flow read the data and set the status back to WS
		 * object
		 */
		processInstanceResponse.getProcessInstances().forEach(pInstance -> {
			if (waterConnectionRequest.getWaterConnection().getApplicationNo().equals(pInstance.getBusinessId())) {
				waterConnectionRequest.getWaterConnection()
						.setApplicationStatus(pInstance.getState().getApplicationStatus());
			}
		});
	}

	public String getBusinessService(String activityType) {

		switch (activityType) {
		case WCConstants.WS_NEWCONNECTION:
		case WCConstants.WS_APPLY_FOR_REGULAR_CON:
			return config.getBusinessServiceValue();
		case WCConstants.WS_APPLY_FOR_TEMPORARY_CON:
			return config.getBusinessServiceTemporaryValue();
		case WCConstants.WS_APPLY_FOR_TEMPORARY_CON_BILLING:
			return config.getBusinessServiceTempBillingValue();
		case WCConstants.WS_APPLY_FOR_TEMP_TEMP_CON:
			return config.getBusinessServiceTempToTempValue();
		case WCConstants.WS_APPLY_FOR_TEMP_REGULAR_CON:
			return config.getBusinessServiceTempToRegularValue();
		case WCConstants.WS_PERMANENT_DISCONNECTION:
			return config.getBusinessServiceDisconnectionValue();
		case WCConstants.WS_TEMPORARY_DISCONNECTION:
			return config.getBusinessServiceTempdisconnectValue();
		case WCConstants.WS_REACTIVATE:
			return config.getBusinessServicereactivateValue();
		case WCConstants.WS_CHANGE_OWNER_INFO:
			return config.getBusinessServiceRenameValue();
		case WCConstants.WS_CONVERSION:
			return config.getBusinessServiceConversionValue();
		case WCConstants.WS_NEW_TUBEWELL_CONNECTION:
			return config.getBusinessServiceTubewellValue();
		case WCConstants.WS_UPDATE_METER_INFO:
			return config.getBusinessServiceupdateMeterValue();
		}
		return "";
	}
}
