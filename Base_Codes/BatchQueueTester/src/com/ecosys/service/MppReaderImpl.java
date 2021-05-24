package com.ecosys.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.ws.rs.core.NewCookie;

import com.ecosys.exception.SystemException;
import com.ecosys.getmetcfile.MSPGetMppFileResultType;
import com.ecosys.getmetcfile.MSPGetMppFileType;
import com.ecosys.getmprjfile.MSPGetMppFileStructureResultType;
import com.ecosys.getmprjfile.MSPGetMppFileStructureType;
import com.ecosys.properties.GlobalConstants;
import com.ecosys.putetchrs.MSPPutMppDataResultType;
import com.ecosys.putprjwbs.MSPPutMppStructureRequestType;
import com.ecosys.putprjwbs.MSPPutMppStructureResultType;
import com.ecosys.putprjwbs.MSPPutMppStructureType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;


public class MppReaderImpl extends IntegratorBase implements IntegratorMgr {
	
	String projectName = null; 
	
	public void test() throws SystemException {
		process("22961");
	}

	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		
		//Arg1 : Project Name ; Arg2 : ProjectInternalID ; Arg3 : MinorPeriodID
		String arguments[] = getArguments(taskInternalID);
		projectName = arguments[0];
		
		String integrationType = getIntegrationType(taskInternalID);
		
		switch (integrationType){
		
		case "Import ETC Hours": {
			
			importETCHours(arguments[1],arguments[2]);
			break;
			
			}
		case "Import Project Structure": {
			importProjectStructure(arguments[1]);
			break;
			
			}
		
		default: {
			logError("Integration type not found : " + integrationType);
			
			}
			
		}
		
		
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
		
	}
	
	
	private void importETCHours(String projectID, String minorPeriodID) throws SystemException {
		// TODO Auto-generated method stub
		
		try {
			
			String mppFilePath = getMPPFile(client, projectID, minorPeriodID);
			
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
		} catch (Exception e) {
			// TODO: handle exception
		}

		

	}

	private void importProjectStructure(String projectID) throws SystemException {
		// TODO Auto-generated method stub
		List<MSPPutMppStructureType> lstWBSStructure = new ArrayList<MSPPutMppStructureType>();
		String mppFilePath;
		try {
			mppFilePath = getMPPFile(client, projectID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			for(Task task : project.getTasks()) {
				
				if (task.getResourceAssignments().size()==0 ) {
					MSPPutMppStructureType wbsRecord = new MSPPutMppStructureType();
					String strWBS = task.getWBS();
					String pathID = "";
					if (strWBS.contains(projectName)) {
						pathID = strWBS;
					}
					else {
						pathID = projectName + GlobalConstants.EPC_HIERARCHY_SEPARATOR + strWBS; 
					}
					String strID = strWBS.substring(strWBS.lastIndexOf(".") + 1);
					String strName = task.getName();
					wbsRecord.setPathID(pathID);
					wbsRecord.setID(strID);
					wbsRecord.setName(strName);
					
					if (task.getOutlineLevel()!=0) {
						
						lstWBSStructure.add(wbsRecord);
						logInfo("Record added - PathID: " + pathID + " Task Name: "+ strName);
					}
						
				}
				
			}
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MPXJException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logInfo("Count of WBS Items : " + String.valueOf(lstWBSStructure.size()));
		
	//	List<MSPPutMppStructureResultType> resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstWBSStructure, 
	//			MSPPutMppStructureRequestType.class, MSPPutMppStructureResultType.class, com.ecosys.putprjwbs.ObjectFactory.class, 
	//			GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_GETMPRJFILE,GlobalConstants.EPC_API_GETMPRJFILE, null, true);
		
	}

	// get MPP file for retrieve ETC Data
	private String getMPPFile (Client client, String projectID, String minorPeriodID) throws SystemException {
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
			logError("Could not find the mpp file to create ETC data for Project - " + projectName);
		}
		
		return mppFilePath;
		
	}
	
	// get MPP file for retrieve Project Structure Data
	private String getMPPFile (Client client, String projectID) throws SystemException {
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
			logError("Could not find the mpp file to create Project Structure for Project - " + projectName);
			
		}
		
		return mppFilePath;
	}


}
