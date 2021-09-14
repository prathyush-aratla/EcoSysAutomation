package com.ecosys.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ecosys.ImportWBS.MSPPutMppStructureRequestType;
import com.ecosys.ImportWBS.MSPPutMppStructureResultType;
import com.ecosys.ImportWBS.MSPPutMppStructureType;
import com.ecosys.ImportWBS.ObjectFactory;
import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;

public class MppWbsImportMgrImpl extends IntegratorBase implements IntegratorMgr {
	
	String costObjectID, costObjectInternalID ;
	String mppFilePath;
	ProjectFile mppProject;

	public void test() throws SystemException {
		process("24366");
	}
	
	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		
		String arguments[] = getArguments(taskInternalID);
		costObjectID = arguments[0];
		costObjectInternalID = arguments[1];
		
		mppFilePath = getMPPFile(client, costObjectInternalID);
		mppProject = getProjectFile(mppFilePath);
		
		boolean bvalidFile;
		logInfo("Starting validation");
		bvalidFile = validateProjectFile(getMPPFile(client, costObjectInternalID));	
		logInfo("Valdation Ends");
		
		if (bvalidFile) {			
			logInfo("Begin Project Structure Import...");
			importProjectStructure(mppProject);
			logInfo("Project structure Import completed.");			
		}
		
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}
	
	// Method to create/update Project Structure in EcoSys from MPP File
	private void importProjectStructure(ProjectFile mppProjectFile) throws SystemException {
		
		List<MSPPutMppStructureType> lstUpdateWBS = new ArrayList<MSPPutMppStructureType>();

		ProjectFile project = mppProjectFile;
		String mppProjectPrefix;
		Task rootTask;
		
		try {
			rootTask = project.getTaskByID(Integer.valueOf(0));
			mppProjectPrefix = String.valueOf(rootTask.getFieldByAlias("WBS Path ID"));
			logDebug("Project Prefix : " + mppProjectPrefix);
			if (mppProjectPrefix == "null") {
				throw new SystemException("WBS Path ID Formula not defined correctly in mpp file");
			}
			
			logDebug(
					padRight("WBS Path ID",25) +  " | " +
					padRight("ID",5) +  " | " + 
					padRight("Description",50) +  " | " +
					padRight("CCL ",5) +  " | " + 
					padRight("Rem Hrs",10) );			
			
			for(Task task : project.getTasks()) {				
//				Get Tasks which are Active. iPM currently including the milestone tasks as well.
//				!task.getMilestone() && task.getActive()
					if (task.getActive()) {
						MSPPutMppStructureType wbsRecord = new MSPPutMppStructureType();
						
						String strWBS, wbsID , wbsName, pathID, externalKey, costControlLevel = null ;
										
						strWBS = String.valueOf(task.getFieldByAlias("WBS Path ID"));
						pathID = pathidBuilder(costObjectID, mppProjectPrefix, strWBS);
						wbsID = pathID.substring(pathID.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
						wbsName = task.getName();
						externalKey = costObjectID +GlobalConstants.EPC_HIERARCHY_SEPARATOR+ task.getUniqueID().toString();
						
						if (strWBS.equals("null")) {
							throw new SystemException("WBS Path ID Formula not defined correctly in mpp file");
						}
						
// Logic to determine cost control level. for iPM Cost Control Level is defined in the mpp file	
						
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
		catch (SystemException e) {
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
		parameterMap.put("RootCostObject", costObjectInternalID);
		
		List<MSPPutMppStructureResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstUpdateWBS, MSPPutMppStructureRequestType.class,
				MSPPutMppStructureResultType.class, ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEWBS, GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppStructureResultType result : resultList) {
			for(com.ecosys.ImportWBS.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.ImportWBS.ObjectResultType.class, com.ecosys.ImportWBS.ResultMessageType.class, ort);
					logError(ort.getExternalId() + " " +   message);
					
					for(MSPPutMppStructureType rec : lstUpdateWBS) {
						
						if (rec.getExternalKey().equals(ort.getExternalId())) {
							
							logDebug(
									padRight(rec.getPathID(), 25)  + " | " +
									padRight(rec.getID(),5) + " | " +
									padRight(rec.getName(),75) + " | " +
									padRight(rec.getCostControlLevel(),5)  + " | " +
									padRight(rec.getExternalKey(),10) ); 
						}
					}
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
