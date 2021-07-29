package org.egov.waterconnection.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.waterconnection.config.WSConfiguration;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.model.BillGeneration;
import org.egov.waterconnection.model.BillGenerationRequest;
import org.egov.waterconnection.model.SearchCriteria;
import org.egov.waterconnection.model.WaterConnection;
import org.egov.waterconnection.model.WaterConnectionRequest;
import org.egov.waterconnection.model.collection.PaymentRequest;
import org.egov.waterconnection.producer.WaterConnectionProducer;
import org.egov.waterconnection.repository.builder.WsQueryBuilder;
import org.egov.waterconnection.repository.rowmapper.WaterRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class WaterDaoImpl implements WaterDao {

	@Autowired
	private WaterConnectionProducer waterConnectionProducer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private WsQueryBuilder wsQueryBuilder;

	@Autowired
	private WaterRowMapper waterRowMapper;
	
	@Autowired
	private WSConfiguration wsConfiguration;

	@Value("${egov.waterservice.createwaterconnection}")
	private String createWaterConnection;

	@Value("${egov.waterservice.updatewaterconnection}")
	private String updateWaterConnection;
	
	@Value("${egov.waterservice.createwatersubactivity}")
	private String createWaterSubActivity;
	
	@Override
	public void saveWaterConnection(WaterConnectionRequest waterConnectionRequest) {
		waterConnectionProducer.push(createWaterConnection, waterConnectionRequest);
	}

	@Override
	public List<WaterConnection> getWaterConnectionList(SearchCriteria criteria,
			RequestInfo requestInfo) {
		List<Object> preparedStatement = new ArrayList<>();
		String query = wsQueryBuilder.getSearchQueryString(criteria, preparedStatement, requestInfo);
		if (query == null)
			return Collections.emptyList();
//		if (log.isDebugEnabled()) {
	//		StringBuilder str = new StringBuilder("Water query: ").append(query);
//			log.info(str.toString());
//		}
		List<WaterConnection> waterConnectionList = jdbcTemplate.query(query, preparedStatement.toArray(),
				waterRowMapper);
		
		if (waterConnectionList == null) {
			return Collections.emptyList();
		}

		return waterConnectionList;
	}

	@Override
	public void updateWaterConnection(WaterConnectionRequest waterConnectionRequest, boolean isStateUpdatable) {
		if(log.isInfoEnabled()) {
			log.info("UpdateWaterConnection: isStateUpdatable ? {}, WaterConnection: {}",isStateUpdatable, waterConnectionRequest.getWaterConnection());
		}
		if (isStateUpdatable) {
			waterConnectionProducer.push(updateWaterConnection, waterConnectionRequest);
		} else {
			waterConnectionProducer.push(wsConfiguration.getWorkFlowUpdateTopic(), waterConnectionRequest);
		}
	}
	
	/**
	 * push object to create meter reading
	 * 
	 * @param waterConnectionRequest
	 */
	public void postForMeterReading(WaterConnectionRequest waterConnectionRequest) {
		log.info("Posting request to kafka topic - " + wsConfiguration.getCreateMeterReading());
		waterConnectionProducer.push(wsConfiguration.getCreateMeterReading(), waterConnectionRequest);
	}

	/**
	 * push object for edit notification
	 * 
	 * @param waterConnectionRequest
	 */
	public void pushForEditNotification(WaterConnectionRequest waterConnectionRequest) {
		if (!WCConstants.EDIT_NOTIFICATION_STATE
				.contains(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			waterConnectionProducer.push(wsConfiguration.getEditNotificationTopic(), waterConnectionRequest);
		}
	}
	
	/**
	 * Enrich file store Id's
	 * 
	 * @param waterConnectionRequest
	 */
	public void enrichFileStoreIds(WaterConnectionRequest waterConnectionRequest) {
		waterConnectionProducer.push(wsConfiguration.getFileStoreIdsTopic(), waterConnectionRequest);
	}
	
	/**
	 * Save file store Id's
	 * 
	 * @param waterConnectionRequest
	 */
	public void saveFileStoreIds(WaterConnectionRequest waterConnectionRequest) {
		waterConnectionProducer.push(wsConfiguration.getSaveFileStoreIdsTopic(), waterConnectionRequest);
	}

	@Override
	public void addConnectionMapping(WaterConnectionRequest waterConnectionRequest) {
		waterConnectionProducer.push(wsConfiguration.getAddConnectionMapping(), waterConnectionRequest);
		
	}

	@Override
	public void deleteConnectionMapping(WaterConnectionRequest waterConnectionRequest) {
		waterConnectionProducer.push(wsConfiguration.getDeleteConnectionMapping(), waterConnectionRequest);		
	}

	@Override
	public void updatebillingstatus(BillGeneration billingData) {
		BillGenerationRequest billReq = BillGenerationRequest.builder().billGeneration(billingData).build();
		waterConnectionProducer.push(wsConfiguration.getUpdateBillPayment(), billReq);
		
	}
}
