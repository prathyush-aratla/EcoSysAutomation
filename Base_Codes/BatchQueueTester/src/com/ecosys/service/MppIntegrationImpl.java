package com.ecosys.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;
import com.ecosys.putetchrs.MSPPutMppDataRequestType;
import com.ecosys.putetchrs.MSPPutMppDataResultType;
import com.ecosys.putetchrs.MSPPutMppDataType;
import com.ecosys.putprjwbs.MSPPutMppStructureRequestType;
import com.ecosys.putprjwbs.MSPPutMppStructureResultType;
import com.ecosys.putprjwbs.MSPPutMppStructureType;
import com.ecosys.putprjwbs.ObjectFactory;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;


public class MppIntegrationImpl extends IntegratorBase implements IntegratorMgr {
	
	String costObjectID = null; 
	
	public void test() throws SystemException {
		process("23350");
	}

	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		
		String arguments[] = getArguments(taskInternalID);
		costObjectID = arguments[0];
		String integrationType = getIntegrationType(taskInternalID);
		
//		Based on the Integration type code will call the relevant procedure.
//		The import ETC Hours will call both importProjectStrucutre and importETCHours. This is due to the fact that any new WBS created in the MPP File
//		Should be reflected before importing the hours
		switch (integrationType){
		
		case "Import ETC Hours": {
			
//			importProjectStructure(arguments[1]);
			importETCHours(arguments[1],arguments[2]);
			break;
			
			}
		case "Import Project Structure": {
			
			importProjectStructure(arguments[1]);
			break;
			
			}
		
		default: {
			logError("Integration type not found : " + integrationType);
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);		
			}
			
		}
		
		
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
		
	}
	
	//Method to create/update Project Structure in EcoSys from MPP File
	private void importProjectStructure(String prjInternalID) throws SystemException {
		
		// TODO Auto-generated method stub
		List<MSPPutMppStructureType> lstUpdateWBS = new ArrayList<MSPPutMppStructureType>();
		String mppFilePath;
		try {
			mppFilePath = getMPPFile(client, prjInternalID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			for(Task task : project.getTasks()) {
				
				if (task.getResourceAssignments().size()==0  & task.getActive() == true) {
					MSPPutMppStructureType wbsRecord = new MSPPutMppStructureType();
					String strWBS = task.getWBS();
					String pathID = "";
					if (strWBS.contains(costObjectID)) {
						pathID = strWBS;
					}
					else {
						pathID = costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + strWBS; 
					}
					String strID = strWBS.substring(strWBS.lastIndexOf(".") + 1);
					String strName = task.getName();
					wbsRecord.setPathID(pathID);
					wbsRecord.setID(strID);
					wbsRecord.setName(strName);
					
					if (task.getOutlineLevel()!=0) {
						
						lstUpdateWBS.add(wbsRecord);
						
						logInfo("Record added - PathID: " + pathID + " Task Name: "+ strName);
					}
						
				}
				
			}
		} catch (SystemException | IOException | MPXJException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logInfo("Count of WBS Items : " + String.valueOf(lstUpdateWBS.size()));
		
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", prjInternalID);
		
		List<MSPPutMppStructureResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstUpdateWBS, MSPPutMppStructureRequestType.class,
				MSPPutMppStructureResultType.class, ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEWBS, GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppStructureResultType result : resultList) {
			for(com.ecosys.putprjwbs.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.putprjwbs.ObjectResultType.class, com.ecosys.putprjwbs.ResultMessageType.class, ort);
					logError(ort.getExternalId(), message);
				}
				logDebug("Cost Object Internal ID : " + ort.getInternalId() + " Created/Updated");
			}
		}
		
	}
	
	//Method to update Estimate-To-Complete Hours in EcoSys from MPP File
	private void importETCHours(String prjInternalID, String minorPeriodID) throws SystemException {
		
		// TODO Auto-generated method stub
		
		List<MSPPutMppDataType> lstETCRecords = new ArrayList<MSPPutMppDataType>();
		String mppFilePath;
		try {
			mppFilePath = getMPPFile(client, prjInternalID, minorPeriodID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			for(Task task : project.getTasks()) {
				
				if (task.getResourceAssignments().size() > 0 & task.getActive() == true ) {
					
					String resourceName = "", resourceAlias = "", strWBS = "", wbsPathID = "";
					 
					int resCount;
					double totalRemHrs;
					
					strWBS = task.getParentTask().getWBS();
					
					if(strWBS.contains(costObjectID)) {
						wbsPathID = strWBS;
					}
					else {
						wbsPathID = costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + strWBS; 
					}
					
					resCount = task.getResourceAssignments().size();			
					totalRemHrs = Double.valueOf(task.getRemainingWork().toString().replace("h", ""));
					
					if (totalRemHrs > 0) {
						
						for (ResourceAssignment assignment : task.getResourceAssignments()) {
							
							MSPPutMppDataType etcRecord = new MSPPutMppDataType();
							
							Resource objResource = assignment.getResource();
							
							if (objResource != null) {
								
								resourceName = String.valueOf(objResource.getName());	
								resourceAlias = String.valueOf(objResource.getFieldByAlias("Resource Alias"));
								
							}

							
							logDebug("Record Details : " + wbsPathID + ", " +
											resourceName + ", " +
											resourceAlias);
							
							etcRecord.setObjectPathID(wbsPathID);
							etcRecord.setResource(resourceAlias);
							etcRecord.setMPPETC(totalRemHrs/resCount);
							etcRecord.setStartDate(null);
							etcRecord.setEndDate(null);
							
							lstETCRecords.add(etcRecord);
							
						}
						
					}
					else {
						logDebug("Resources in Path ID " + wbsPathID + " skipped due to no hours left.");
					}

				}	
			
			}
			

		}
		catch (SystemException | IOException | MPXJException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		logInfo("Count of Transactions created : " + String.valueOf(lstETCRecords.size()));
		
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", prjInternalID);
		parameterMap.put("ProjectPeriod", minorPeriodID);
		
		List<MSPPutMppDataResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstETCRecords, MSPPutMppDataRequestType.class,
				MSPPutMppDataResultType.class, com.ecosys.putetchrs.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEETC , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppDataResultType result : resultList) {
			for(com.ecosys.putetchrs.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.putetchrs.ObjectResultType.class, com.ecosys.putetchrs.ResultMessageType.class, ort);
					logError(ort.getExternalId(), message);
				}
				logDebug("ETC Tansaction : " + ort.getInternalId() + " Created");
			}
		}


	}






}
