package com.ecosys.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import com.ecosys.exception.SystemException;

import com.ecosys.mpupdatewbs.MSPPutMppStructureRequestType;
import com.ecosys.mpupdatewbs.MSPPutMppStructureResultType;
import com.ecosys.mpupdatewbs.MSPPutMppStructureType;
import com.ecosys.mpupdatewbs.ObjectFactory;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;

public class MppWbsImportMgrImpl extends IntegratorBase implements IntegratorMgr {
	
	String costObjectID = ""; 
	
	public void test() throws SystemException {
		process("24366");
	}
	

	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		
		String arguments[] = getArguments(taskInternalID);
		costObjectID = arguments[0];
		
		logInfo("Starting update Project Structure...");
		importProjectStructure(arguments[1]);
		logInfo("Update project structure completed.");
		
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
			
			for(Task task : project.getTasks()) {

//					List<Task> lstChildTasks = task.getChildTasks();					
//					for (Task childTask : lstChildTasks) {
//						if (!childTask.hasChildTasks()) {
//							costControlLevel = "Y";
//						}
//					}
									
//	Get only tasks which are active and non-milestone tasks.
//				!task.getMilestone() && task.getActive()
				
					if (task.getActive()) {
						MSPPutMppStructureType wbsRecord = new MSPPutMppStructureType();
						String costControlLevel = "";
						String strWBS = task.getWBS();
						String pathID = "";
						String externalKey = costObjectID +GlobalConstants.EPC_HIERARCHY_SEPARATOR+ task.getUniqueID().toString();
						
						if (strWBS.contains(costObjectID)) {
							pathID = strWBS;
						} else {
							pathID = costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + strWBS;
						}
						
						String wbsID = strWBS
								.substring(strWBS.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
						String wbsName = task.getName();
						
//						String cclFlg = String.valueOf(task.getFieldByAlias("Cost Control Level"));
						
						/* Auto Build logic to determine cost control level. for iPM Cost Control Level is defined in the mpp file*/						
//						if (!task.hasChildTasks() && task.getResourceAssignments().size() > 0 && !task.getMilestone()) {
//							costControlLevel = "Y";
//							wbsRecord.setType("Work Package");
//						}
						
						if(!task.getSummary() && !task.getMilestone() ) {
							costControlLevel = "Y";
							wbsRecord.setType("Work Package");							
						}
						
						wbsRecord.setPathID(pathID);
						wbsRecord.setID(wbsID);
						wbsRecord.setName(wbsName);
						wbsRecord.setCostControlLevel(costControlLevel);
						wbsRecord.setExternalKey(externalKey);
						if (task.getOutlineLevel() != 0) {
							lstUpdateWBS.add(wbsRecord);
							logDebug("Record added - PathID: " + pathID +
									", Name: " + wbsName +
									", ExternalID: " + externalKey +
									", CCL=" + costControlLevel);
						} 
					}	
			}
		} 
		catch (SystemException | IOException | MPXJException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logError(e);
			logInfo("Import Terminated due to Errors");
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
			batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
			System.exit(1);
		}
		
		logInfo("Count of WBS Items to Update: " + String.valueOf(lstUpdateWBS.size()));
		
		//Push WBS Records to EcoSys
		int passCnt=0 , failCnt=0;
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", prjInternalID);
		
		List<MSPPutMppStructureResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstUpdateWBS, MSPPutMppStructureRequestType.class,
				MSPPutMppStructureResultType.class, ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEWBS, GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppStructureResultType result : resultList) {
			for(com.ecosys.mpupdatewbs.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.mpupdatewbs.ObjectResultType.class, com.ecosys.mpupdatewbs.ResultMessageType.class, ort);
					logError(ort.getExternalId(), message);
					failCnt++;
				}
				else {
					
					logDebug("Cost Object Internal ID : " + ort.getInternalId() + " Created/Updated");
					passCnt++;
				}
				
			}
			
			logInfo("Total created Items = " + passCnt + ", Failed Items = " + failCnt);
		}
	}
	
}
