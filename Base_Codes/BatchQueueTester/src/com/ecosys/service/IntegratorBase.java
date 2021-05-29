package com.ecosys.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.NewCookie;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;

import com.ecosys.beans.RecordStatus;
import com.ecosys.exception.SystemException;
import com.ecosys.getmetcfile.MSPGetMppFileResultType;
import com.ecosys.getmetcfile.MSPGetMppFileType;
import com.ecosys.getmprjfile.MSPGetMppFileStructureResultType;
import com.ecosys.getmprjfile.MSPGetMppFileStructureType;
import com.ecosys.properties.BatchQueueConstants;
import com.ecosys.properties.GlobalConstants;
import com.ecosys.rest.BatchQueueRead.BatchQueueReadType;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;


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
	
	protected XMLGregorianCalendar dateToXMLGregorianCalendar(Date dateToConvert) {
		
		XMLGregorianCalendar xmlDate = null;
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(dateToConvert);
		
		try {
			xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return xmlDate;
		
	}
	
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
	
	// get MPP file for retrieve ETC Data
	protected String getMPPFile (Client client, String projectID, String minorPeriodID) throws SystemException {
		String mppFilePath = null;
		
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", projectID);
		parameterMap.put("ProjectPeriod", minorPeriodID);

		ClientResponse response = this.epcRestMgr.getAsApplicationXml(client, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_GETMETCFILE, null, parameterMap, true);
		this.logger.debug("HTTP status code: " + response.getStatus());
		NewCookie sessionCookie = this.epcRestMgr.getSessionCookie(response);
		if(sessionCookie != null)
			this.epcRestMgr.logout(client, GlobalConstants.EPC_REST_Uri, sessionCookie);
		
		MSPGetMppFileResultType result = this.epcRestMgr.responseToObject(response, MSPGetMppFileResultType.class);
		
		if(!result.isSuccessFlag()) {
			this.logger.debug(this.epcRestMgr.responseToString(response, true));
			throw new SystemException("Error reading " + GlobalConstants.EPC_API_GETMETCFILE);
		}
		
		List<MSPGetMppFileType> lstMSPType = result.getMSPGetMppFile();

		com.ecosys.getmetcfile.DocumentValueType document = lstMSPType.get(0).getAttachment();
		
		if (document.getTitle() != null) {
			
			String strHRefDoc = document.getLink().getHref();
			try {
				URI uri = new URI(strHRefDoc.replace(" ", "%20")+ "&_username=" + GlobalConstants.EPC_REST_USERNAME + "&_password=" + GlobalConstants.EPC_REST_PASSWORD);
				mppFilePath = uri.toString();
				
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			logError("Could not find the mpp file to create ETC data for Project");
		}
		
		return mppFilePath;
		
	}
	
	// get MPP file for retrieve Project Structure Data
	protected String getMPPFile (Client client, String projectID) throws SystemException {
		String mppFilePath = null;
		
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", projectID);

		ClientResponse response = this.epcRestMgr.getAsApplicationXml(client, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_GETMPRJFILE, null, parameterMap, true);
		this.logger.debug("HTTP status code: " + response.getStatus());
		NewCookie sessionCookie = this.epcRestMgr.getSessionCookie(response);
		if(sessionCookie != null)
			this.epcRestMgr.logout(client, GlobalConstants.EPC_REST_Uri, sessionCookie);
		
		MSPGetMppFileStructureResultType result = this.epcRestMgr.responseToObject(response, MSPGetMppFileStructureResultType.class);
		
		if(!result.isSuccessFlag()) {
			this.logger.debug(this.epcRestMgr.responseToString(response, true));
			throw new SystemException("Error reading " + GlobalConstants.EPC_API_GETMPRJFILE);
		}
		
		List<MSPGetMppFileStructureType> lstMSPType = result.getMSPGetMppFileStructure();

		com.ecosys.getmprjfile.DocumentValueType document = lstMSPType.get(0).getAttachment();
		
		if (document.getTitle() != null) {
			
			String strHRefDoc = document.getLink().getHref();
			try {
				URI uri = new URI(strHRefDoc.replace(" ", "%20")+ "&_username=" + GlobalConstants.EPC_REST_USERNAME + "&_password=" + GlobalConstants.EPC_REST_PASSWORD);
				mppFilePath = uri.toString();
				
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			logError("Could not find the mpp file to create Project Structure for Project");
			
		}
		
		return mppFilePath;
	}


}
