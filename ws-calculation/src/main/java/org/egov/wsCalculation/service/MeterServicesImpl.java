package org.egov.wsCalculation.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.wsCalculation.model.MeterConnectionRequest;
import org.egov.wsCalculation.model.MeterReading;
import org.egov.wsCalculation.model.MeterReadingSearchCriteria;
import org.egov.wsCalculation.repository.ServiceRequestRepository;
import org.egov.wsCalculation.repository.WSCalculationDao;
import org.egov.wsCalculation.util.MeterReadingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MeterServicesImpl implements MeterService {

	@Autowired
	MeterReadingUtil meterReadingUtil;
	
	@Autowired
	WSCalculationDao wSCalculationDao;

	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	public MeterServicesImpl(ServiceRequestRepository serviceRequestRepository) {
		this.serviceRequestRepository = serviceRequestRepository;

	}


	@Override
	public List<MeterReading> createMeterReading(MeterConnectionRequest meterConnectionRequest) {
		
		
		

		List<MeterReading> meterReadingsList = new ArrayList<MeterReading>();
//		Object result = serviceRequestRepository.fetchResult(meterReadingUtil.getDemandGenerationCreateURL(),
//				meterConnectionRequest);
//		meterReadingUtil.getMeterReadingDetails(result);
		meterReadingsList.add(meterConnectionRequest.getMeterReading());

		wSCalculationDao.saveWaterConnection(meterConnectionRequest);

		return meterReadingsList;
	}


	@Override
	public List<MeterReading> searchMeterReadings(MeterReadingSearchCriteria criteria, RequestInfo requestInfo) {
		// TODO Auto-generated method stub
		return null;
	}

}