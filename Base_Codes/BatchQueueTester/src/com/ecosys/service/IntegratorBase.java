package com.ecosys.service;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import com.ecosys.resources.ResourcesResultType;
import com.ecosys.resources.ResourcesType;
import com.ecosys.rest.BatchQueueRead.BatchQueueReadType;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import net.sf.mpxj.CustomFieldContainer;
import net.sf.mpxj.FieldType;
import net.sf.mpxj.FieldTypeClass;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;


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
	
	public static String padRight(String s, int n) {
	     return String.format("%-" + n + "s", s);  
	}

	public static String padLeft(String s, int n) {
	    return String.format("%" + n + "s", s);  
	}
	
	protected List<ResourcesType> epcResources;
	
	
	public List<ResourcesType> getEpcResources() {
		
		ClientResponse response = this.epcRestMgr.getAsApplicationXml(client, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_RESOURCE, null, null, true);
//		this.logger.debug("HTTP status code: " + response.getStatus());
		NewCookie sessionCookie = this.epcRestMgr.getSessionCookie(response);
		if(sessionCookie != null)
			this.epcRestMgr.logout(client, GlobalConstants.EPC_REST_Uri, sessionCookie);
		
		ResourcesResultType result = this.epcRestMgr.responseToObject(response,ResourcesResultType.class);
		
		epcResources = result.getResources();
		
		return epcResources;
	}


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
	
	
	// Method to return the Integration Type
	protected String getIntegrationType(String taskInternalID) throws SystemException {
		
		String integrationType = batchQueueMgr.readTask(client,taskInternalID).getBatchQueueIntegrationTypeID();
		
		this.logger.info(integrationType);
		
		return integrationType;
	}
	
	// Method to get MPP file - Period Specific
	protected String getMPPFile (Client client, String projectID, String minorPeriodID) throws SystemException {
		String mppFilePath = null;
		
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", projectID);
		parameterMap.put("ProjectPeriod", minorPeriodID);

		ClientResponse response = this.epcRestMgr.getAsApplicationXml(client, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_GETMETCFILE, null, parameterMap, true);
//		this.logger.debug("HTTP status code: " + response.getStatus());
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
		
//		this.logDebug("Document loaded : " + String.valueOf(document.getTitle()));
		
		if (document.getTitle() != null) {
			
			String strHRefDoc = document.getLink().getHref();
			try {
				URI uri = new URI(strHRefDoc.replace(" ", "%20")+ "&_username=" + GlobalConstants.EPC_REST_USERNAME + "&_password=" + GlobalConstants.EPC_REST_PASSWORD);
				mppFilePath = uri.toString();
				
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
		}
		else {
			bqrt.setBatchQueueStatusID(GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
			throw new SystemException("error100 : File not Found");
			
		}
		
		return mppFilePath;
		
	}
	
	// Method to get MPP file - Project wide
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
			bqrt.setBatchQueueStatusID(GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
			throw new SystemException("error100 : File not Found");
			
		}
		
		return mppFilePath;
	}

	//WBS Path builder to replace the MS Project Prefix code with EcoSys Project ID
	protected String pathIdBuilder (String costObjectID, String mppWbsPrefix, String mppWbsPath) throws SystemException{
		String wbsPathID = "";
		
		try {
			wbsPathID =costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + mppWbsPath.replace(mppWbsPrefix, "");
			
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		
		return wbsPathID;
	}
	
	//
	protected boolean validateProjectFile (String filePath) throws SystemException {
		
		boolean bValid = false;
		
		try {
			
			InputStream input = new URL(filePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			CustomFieldContainer atts = project.getCustomFields();		
			
			 FieldType resResourceAlias = atts.getFieldByAlias(FieldTypeClass.RESOURCE, "Resource Alias");
			 
			 FieldType tskWBSPathID = atts.getFieldByAlias(FieldTypeClass.TASK, "WBS Path ID");
			 
			 if (resResourceAlias != null)
			 {
				 if (tskWBSPathID != null)
				 {
					 bValid = true;
				 }
				 else {
					logError("error101 : WBS Path ID custom field not defined in mpp File");
					throw new Exception("Task Custom Field Not Defined");
				}
				 
			 }
			 else {
					logError("error102 : Resource Alias custom field not defined in mpp File");
					throw new Exception("Resource Custom Field Not Defined");
			 }
			
			logDebug("Total Custom Fields : " + String.valueOf(atts.size()));
			
//			for (CustomField cf : atts) {
//				
//				if (cf.getAlias().equals("Resource Alias") && cf.getFieldType().getFieldTypeClass().name().equals("RESOURCE")) {
//						
//						bValid = true;
//
//				}
//				else if (cf.getAlias().equals("WBS") && cf.getFieldType().getFieldTypeClass().name().equals("RESOURCE")) {
//					
//				}
//				
////				logDebug(cf.getAlias() + " | " +
////						cf.getFieldType().getFieldTypeClass().name());
////				
//				
//			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			logError(e);
			logInfo("Import Terminated due to Errors");
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
			batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
			System.exit(1);
			
		}
		
		return bValid;
	}

}
