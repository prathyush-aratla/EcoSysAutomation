package com.ecosys.service;

import java.util.HashMap;
import java.util.List;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.core.NewCookie;

import com.ecosys.exception.SystemException;
import com.ecosys.getmetcfile.DocumentValueType;
import com.ecosys.getmetcfile.MSPGetMppFileResultType;
import com.ecosys.getmetcfile.MSPGetMppFileType;
import com.ecosys.getmprjfile.MSPGetMppFileStructureResultType;
import com.ecosys.getmprjfile.MSPGetMppFileStructureType;
import com.ecosys.properties.GlobalConstants;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;


public class MppReaderImpl extends IntegratorBase implements IntegratorMgr {
	
	public void test() throws SystemException {
		process("22963");
	}

	public void process(String taskInternalID) throws SystemException {
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		
		String arguments[] = getArguments(taskInternalID);
		
		String filePathA = getMPPFile(client, arguments[1]);
				
		String filePathB = getMPPFile(client, arguments[1], arguments[2]);
		
		logInfo("Project File : " + filePathA);
		logInfo("ETC File : " + filePathB);
		
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
		
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
		
		return mppFilePath;
	}


}
