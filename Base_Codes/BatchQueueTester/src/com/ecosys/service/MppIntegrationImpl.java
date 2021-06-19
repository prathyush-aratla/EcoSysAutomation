package com.ecosys.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.ecosys.ImportETC.MSPPutMppDataRequestType;
import com.ecosys.ImportETC.MSPPutMppDataResultType;
import com.ecosys.ImportETC.MSPPutMppDataType;
import com.ecosys.ImportWBS.MSPPutMppStructureRequestType;
import com.ecosys.ImportWBS.MSPPutMppStructureResultType;
import com.ecosys.ImportWBS.MSPPutMppStructureType;
import com.ecosys.ImportWBS.ObjectFactory;
import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;

public class MppIntegrationImpl extends IntegratorBase implements IntegratorMgr {
	
	String costObjectID = ""; 
	
	public void test() throws SystemException {
		process("23544");
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
		
		case "Import Project Structure": {
			logInfo("Starting update Project Structure...");
			importProjectStructure(arguments[1]);
			logInfo("Update project structure completed.");
			break;
			
			}
		
		case "Import Budget":{
			
			break;
		}
		
		case "Import Progress":{
			updateProgress(arguments[1], arguments[2]);
			break;
		}
		
		case "Import ETC Hours": {		
//			logInfo("Updating Project Structure started..");
//			importProjectStructure(arguments[1]);
//			logInfo("Project Structure Update completed.");
			logInfo("ETC Hours Update started..");
			importETCHours(arguments[1],arguments[2]);
//			updateProgress(arguments[1], arguments[2]);
			logInfo("ETC Hours update completed.");
			break;
		}
		
		default: {
			logError("Integration type not found : " + integrationType);
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);		
			}
			
		}
		
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}
	
	
	
	// Method to create/update Project Structure in EcoSys from MPP File
	private void importProjectStructure(String prjInternalID) throws SystemException {
		
		// TODO Auto-generated method stub
		List<MSPPutMppStructureType> lstUpdateWBS = new ArrayList<MSPPutMppStructureType>();
		String mppFilePath;
		try {
			mppFilePath = getMPPFile(client, prjInternalID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
		// The below process to enumerate WBS with CCL=Yes
			HashMap<String , String> cMap = new HashMap<String, String>();
			for(Task task : project.getTasks()) {
				if (task.getActive() == true & !task.hasChildTasks() & task.getResourceAssignments().size() > 0) {
				cMap.put(costObjectID +"." + task.getParentTask().getWBS(), costObjectID +"." + task.getWBS() );

				}
			}
//			cMap.forEach((key,value) -> System.out.println(key + " <--- " + value));

			
			for(Task task : project.getTasks()) {
				
				if (task.getResourceAssignments().size() == 0 ) {
					MSPPutMppStructureType wbsRecord = new MSPPutMppStructureType();
					String costControlLevel = "";
					String strWBS = task.getWBS();
					String pathID = "";
					
					if (strWBS.contains(costObjectID)) {
						pathID = strWBS;
					}
					else {
						pathID = costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + strWBS; 
					}
					
					String wbsID = strWBS.substring(strWBS.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
					String wbsName = task.getName();
					
					if (cMap.containsKey(pathID)) {
						costControlLevel = "Y";
					}
									
					wbsRecord.setPathID(pathID);
					wbsRecord.setID(wbsID);
					wbsRecord.setName(wbsName);
					wbsRecord.setCostControlLevel(costControlLevel);
					
					if (task.getOutlineLevel()!=0) {
						
						lstUpdateWBS.add(wbsRecord);
						
						logDebug("Record added - PathID: " + pathID + ", Name: "+ wbsName + ", CCL=" + costControlLevel);
					}	
				}
			}
		} 
		catch (SystemException | IOException | MPXJException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logError(e);
		}
		
		logInfo("Count of WBS Items updated : " + String.valueOf(lstUpdateWBS.size()));
		
		//Push WBS Records to EcoSys
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", prjInternalID);
		
		List<MSPPutMppStructureResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstUpdateWBS, MSPPutMppStructureRequestType.class,
				MSPPutMppStructureResultType.class, ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEWBS, GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppStructureResultType result : resultList) {
			for(com.ecosys.ImportWBS.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.ImportWBS.ObjectResultType.class, com.ecosys.ImportWBS.ResultMessageType.class, ort);
					logError(ort.getExternalId(), message);
				}
				logDebug("Cost Object Internal ID : " + ort.getInternalId() + " Created/Updated");
			}
		}
	}
	
	//Method to update progress
	private void updateProgress(String prjInternalID, String minorPeriodID) throws SystemException {
		String mppFilePath="";
//		List<MSPPutMppStructureType> lstProgressItems = new ArrayList<MSPPutMppStructureType>();
			
		
		try {
			mppFilePath = getMPPFile(client, prjInternalID, minorPeriodID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			HashMap<String , Number> progressMap = new HashMap<String, Number>();
			
			for(Task task : project.getTasks()) {
				if (task.getActive()== true & !task.hasChildTasks()) {
//				logDebug(costObjectID + "." + task.getWBS() + ", " +
//						task.hasChildTasks() + " | " +
//						costObjectID +"." +task.getParentTask().getWBS() + ", " +
//						task.getParentTask().getPercentageComplete());
				progressMap.put(costObjectID +"." +task.getParentTask().getWBS(), 
						task.getParentTask().getPercentageComplete());
				}
			}
			progressMap.forEach((key,value) -> System.out.println(key + " = " + value));
//			progressMap.forEach((key,value) -> {
//			MSPPutMppStructureType objRecord = new MSPPutMppStructureType();	
//				objRecord.setPathID(key);
//				objRecord.setCostControlLevel("Y");
//				
//				lstProgressItems.add(objRecord);
//			});
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			logError(e);
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
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
					
					String resourceName = "", resourceAlias = "", strWBS = "", wbsPathID = "", origWBS= "";
					int resCount;
					double totalRemHrs;
					
					Date startDate = task.getStart();
					Date endDate = task.getFinish();
					
					origWBS = task.getWBS();
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
							
							logDebug("Record Details : " + origWBS + ", " +
											resourceName + ", " +
											resourceAlias + ", " +
											(totalRemHrs/resCount) + " hrs, " +
											dateToXMLGregorianCalendar(startDate).toString() + ", " +
											dateToXMLGregorianCalendar(endDate).toString());
							
							etcRecord.setObjectPathID(wbsPathID);
							etcRecord.setResource(resourceAlias);
							etcRecord.setMPPETC(totalRemHrs/resCount);
							etcRecord.setStartDate(dateToXMLGregorianCalendar(startDate));
							etcRecord.setEndDate(dateToXMLGregorianCalendar(endDate));
							
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
			logError(e);
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
		}
		
		logInfo("Count of Transactions prepared : " + String.valueOf(lstETCRecords.size()));
		
		int passCnt=0 , failCnt=0;
		
		//Push ETC Records to EcoSys
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", prjInternalID);
		parameterMap.put("ProjectPeriod", minorPeriodID);
		
		List<MSPPutMppDataResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstETCRecords, MSPPutMppDataRequestType.class,
				MSPPutMppDataResultType.class, com.ecosys.ImportETC.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEETC , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppDataResultType result : resultList) {
			for(com.ecosys.ImportETC.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.ImportETC.ObjectResultType.class, com.ecosys.ImportETC.ResultMessageType.class, ort);
					logError(ort.getExternalId(), message);
					failCnt++;
				}
				else {
					logDebug("ETC Tansaction : " + ort.getInternalId() + " Created");
					passCnt++;
				}
				
			}
		}
    	
    	logInfo("Total created Items = " + passCnt + ", Failed Items = " + failCnt);
	}
}
