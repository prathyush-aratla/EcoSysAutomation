package com.ecosys.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ecosys.beans.RecordStatus;
import com.ecosys.exception.SystemException;
import com.ecosys.properties.BatchQueueConstants;
import com.ecosys.rest.BatchQueueRead.BatchQueueReadType;
import com.sun.jersey.api.client.Client;


public abstract class IntegratorBase {
	
	protected Logger logger = Logger.getLogger(IntegratorBase.class);
	protected List<RecordStatus> loggerList = new ArrayList<RecordStatus>();
	
	protected EpcRestMgr epcRestMgr;

	public EpcRestMgr getEpcRestMgr() {
		return epcRestMgr;
	}

	public void setEpcRestMgr(EpcRestMgr epcRestMgr) {
		this.epcRestMgr = epcRestMgr;
	}
	
	protected BatchQueueMgr batchQueueMgr;

	public BatchQueueMgr getBatchQueueMgr() {
		return batchQueueMgr;
	}

	public void setBatchQueueMgr(BatchQueueMgr batchQueueMgr) {
		this.batchQueueMgr = batchQueueMgr;
	}
	
	protected BatchQueueReadType bqrt;

	public BatchQueueReadType getBqrt() {
		return bqrt;
	}

	public void setBqrt(BatchQueueReadType bqrt) {
		this.bqrt = bqrt;
	}
	
	protected Client client = null;

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}
	
	
	
	protected void logError(Exception e) {
		this.logger.error(e);
		if (bqrt != null) {
			this.loggerList.add(new RecordStatus("", bqrt.getTaskInternalID(), bqrt.getCostObjectHierarchyPathID(), e.getMessage(), BatchQueueConstants.BATCHQUEUE_LOG_ERROR));
		}
	}
	
	protected void logError(String msg) {
		this.logger.error(msg);
		if (bqrt != null) {
			this.loggerList.add(new RecordStatus("", bqrt.getTaskInternalID(), bqrt.getCostObjectHierarchyPathID(), msg, BatchQueueConstants.BATCHQUEUE_LOG_ERROR));
		}
	}

	protected void logError(String externalKey, String msg) {
		this.logger.error(msg);
		if (bqrt != null) {
			this.loggerList.add(new RecordStatus(externalKey, bqrt.getTaskInternalID(), bqrt.getCostObjectHierarchyPathID(), msg, BatchQueueConstants.BATCHQUEUE_LOG_ERROR));
		}
	}

	protected void logWarn(String msg) {
		this.logger.warn(msg);
		if (bqrt != null) {
			this.loggerList.add(new RecordStatus("", bqrt.getTaskInternalID(), bqrt.getCostObjectHierarchyPathID(), msg, BatchQueueConstants.BATCHQUEUE_LOG_WARN));
		}
	}

	protected void logInfo(String msg) {
		this.logger.info(msg);
		if (bqrt != null) {
			this.loggerList.add(new RecordStatus("", bqrt.getTaskInternalID(), bqrt.getCostObjectHierarchyPathID(), msg, BatchQueueConstants.BATCHQUEUE_LOG_INFO));
		}
	}

	protected void logDebug(String msg) {
		this.logger.debug(msg);
		//TODO log to web service, may skip this for debug messages, need to check
		//if log.webservice==true then log to web service
	}
	
	
	//Custom Implementations for client starts Here
	
	protected String[] getArguments(String taskInternalID) throws SystemException {
		String arguments[] = batchQueueMgr.readTask(client,taskInternalID).getBatchQueueParam1ID().split("\\|");
		
		for ( int i=0; i < arguments.length; i++) {
			
			this.logger.info("Argument-" + (i+1) + " : " +  arguments[i].toString() );
			
		}
		
		return arguments;
	}
	
	protected String getIntegrationType(String taskInternalID) throws SystemException {
		
		String integrationType = batchQueueMgr.readTask(client,taskInternalID).getBatchQueueIntegrationTypeID();
		
		this.logger.info(integrationType);
		
		return integrationType;
	}


}
