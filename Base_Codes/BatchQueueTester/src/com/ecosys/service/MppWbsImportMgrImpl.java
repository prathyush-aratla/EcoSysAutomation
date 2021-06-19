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
	
	String costObjectID, costObjectInternalID ;

	public void test() throws SystemException {
		process("23561");
	}
	
	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		
		String arguments[] = getArguments(taskInternalID);
		costObjectID = arguments[0];
		costObjectInternalID = arguments[1];
		
		boolean bvalidFile;
		logInfo("Starting validation");
		bvalidFile = validateProjectFile(getMPPFile(client, costObjectInternalID));		
		logInfo("Valdation Ends");
		
		if (bvalidFile) {			
			logInfo("Starting update Project Structure...");
			importProjectStructure(arguments[1]);
			logInfo("Update project structure completed.");			
		}
		
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}
	
	// Method to create/update Project Structure in EcoSys from MPP File
	private void importProjectStructure(String prjInternalID) throws SystemException {
		
		// TODO Auto-generated method stub
		List<MSPPutMppStructureType> lstUpdateWBS = new ArrayList<MSPPutMppStructureType>();
		String mppFilePath;
		String mppProjectPrefix;
		try {
			mppFilePath = getMPPFile(client, prjInternalID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			Task rootTask = project.getTaskByID(Integer.valueOf(0));
			mppProjectPrefix = String.valueOf(rootTask.getFieldByAlias("WBS Path ID"));
			logDebug("Project Prefix : " + mppProjectPrefix);
			
			logDebug(
					padRight("WBS Path ID",25) +  " | " +
					padRight("ID",5) +  " | " + 
					padRight("Description",50) +  " | " +
					padRight("CCL ",5) +  " | " + 
					padRight("Rem Hrs",10) );			
			
			for(Task task : project.getTasks()) {				
//	Get only tasks which are active and non-milestone tasks.
//				!task.getMilestone() && task.getActive()
					if (task.getActive()) {
						MSPPutMppStructureType wbsRecord = new MSPPutMppStructureType();
						
						String strWBS, wbsID , wbsName, pathID, costControlLevel = null ;
						String externalKey = costObjectID +GlobalConstants.EPC_HIERARCHY_SEPARATOR+ task.getUniqueID().toString();
						
//						if (strWBS.contains(costObjectID)) {
//							pathID = strWBS;
//						} else {
//							pathID = costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + strWBS;
//						}
//						
//						wbsID = strWBS
//								.substring(strWBS.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
						
						
//						String cclFlg = String.valueOf(task.getFieldByAlias("Cost Control Level"));
						
						/* Auto Build logic to determine cost control level. for iPM Cost Control Level is defined in the mpp file*/						
//						if (!task.hasChildTasks() && task.getResourceAssignments().size() > 0 && !task.getMilestone()) {
//							costControlLevel = "Y";
//							wbsRecord.setType("Work Package");
//						}
						
						strWBS = (String) task.getFieldByAlias("WBS Path ID");
						pathID = pathIdBuilder(costObjectID, mppProjectPrefix, strWBS);
						wbsID = pathID.substring(pathID.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
						wbsName = task.getName();
						
						
						if(!task.getSummary() && !task.getMilestone() ) {
							costControlLevel = "Y";
							wbsRecord.setType("Work Package");							
						}
						
						logDebug(
								padRight(pathID, 25)  + " | " +
								padRight(wbsID,5) + " | " +
								padRight(wbsName,50) + " | " +
								padRight(costControlLevel,5)  + " | " +
								padRight(String.valueOf(task.getRemainingWork()),10) + " | " + 
								padRight(String.valueOf(task.getOutlineLevel()), 5));
						
						wbsRecord.setPathID(pathID);
						wbsRecord.setID(wbsID);
						wbsRecord.setName(wbsName);
						wbsRecord.setCostControlLevel(costControlLevel);
						wbsRecord.setExternalKey(externalKey);
						
						if (task.getOutlineLevel() > 0) {
							lstUpdateWBS.add(wbsRecord);

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
					logError(ort.getExternalId() + " " +   message);
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
