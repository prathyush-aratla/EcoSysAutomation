package com.ecosys.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ecosys.ImportProgress.MSPUpdateProjectProgressRequestType;
import com.ecosys.ImportProgress.MSPUpdateProjectProgressResultType;
import com.ecosys.ImportProgress.MSPUpdateProjectProgressType;
import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;


public class MppProgressImportMgrImpl extends IntegratorBase implements IntegratorMgr {
	
	String costObjectID , costObjectInternalID, minorPeriodID ;
	String mppFilePath;
	ProjectFile mppProject;
	
	public void test() throws SystemException {
		process("23568");
	}
	
	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		String arguments[] = getArguments(taskInternalID);
		
		costObjectID = arguments[0];
		costObjectInternalID = arguments[1];
		minorPeriodID = arguments[2];
		
		mppFilePath = getMPPFile(client, costObjectInternalID, minorPeriodID);
		mppProject = getProjectFile(mppFilePath);
		
		boolean bvalidFile;
		
		
		logInfo("Validating Import file...");
		bvalidFile = validateProjectFile(mppProject);		
		logInfo("Validation ends");
		
		if (bvalidFile) {			
			logInfo("Progress Import begins...");
			importProgress(mppProject);
			logInfo("Progress Import completed");		
		}

		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}

//	This method will fetch the progress percent from the Microsoft Projects File and update the EcoSys Project.
	private void importProgress(ProjectFile mppProjectFile) throws SystemException {
		
		List<MSPUpdateProjectProgressType> lstUpdateProgress = new ArrayList<MSPUpdateProjectProgressType>();

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
			
			logDebug(padRight("Object Path ID", 25)  + " | " +
					padRight("Description", 75) + " | " + 
					padRight("ID",10) + " | " + 
					"Percent Work Complete");
			
			for(Task task : project.getTasks()) {
				
				boolean bActive = task.getActive();
				boolean bSummary = task.getSummary();
				boolean bMilestone = task.getMilestone();
				double progressValue = task.getPercentageWorkComplete().doubleValue();
				
				String strWBS , pathID , wbsID , wbsName ;
									
				if (bActive && !bSummary && !bMilestone && progressValue > 0 ) {					
					
					MSPUpdateProjectProgressType progressRecord = new MSPUpdateProjectProgressType();
																			
					strWBS = (String) task.getFieldByAlias("WBS Path ID");
					pathID = pathidBuilder(costObjectID, mppProjectPrefix, strWBS);
					wbsID = pathID.substring(pathID.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
					wbsName = task.getName();
					
					
					if (strWBS.equals("null")) {
						throw new SystemException("WBS Path ID Formula not defined correctly in mpp file");
					}
					
					progressRecord.setObjectPathID(pathID);
					progressRecord.setObjectID(wbsID);
					progressRecord.setProgressPercent(progressValue);
					
					logDebug(padRight(pathID, 25)  + " | " +
							padRight(wbsName, 75) + " | " + 
							padRight(wbsID,10) + " | " + 
							progressValue);
					
					lstUpdateProgress.add(progressRecord);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logError(e);
			logInfo("Import Terminated due to Errors");
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
			batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
			System.exit(1);		
		}
		
		logInfo("Total Records prepared for update = " + lstUpdateProgress.size());
		
//		Update EcoSys Progress
		int passCnt=0 , failCnt=0;
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", costObjectInternalID);
		
		List<MSPUpdateProjectProgressResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstUpdateProgress, MSPUpdateProjectProgressRequestType.class,
				MSPUpdateProjectProgressResultType.class, com.ecosys.ImportProgress.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEPROGRESS , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPUpdateProjectProgressResultType result : resultList) {
			for(com.ecosys.ImportProgress.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.ImportProgress.ObjectResultType.class, com.ecosys.ImportETC.ResultMessageType.class, ort);
					logError(ort.getExternalId(), message);
					failCnt++;
				}
				else {
					logDebug("Cost Object with Internal ID : " + ort.getInternalId() + " updated");
					passCnt++;
				}
				
			}
			logInfo("Total created Items = " + passCnt + ", Failed Items = " + failCnt);
		}
		
	}

}
