package com.ecosys.service;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.ecosys.exception.SystemException;
import com.ecosys.mpupdateprogress.MSPUpdateProjectProgressRequestType;
import com.ecosys.mpupdateprogress.MSPUpdateProjectProgressResultType;
import com.ecosys.mpupdateprogress.MSPUpdateProjectProgressType;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;

public class MppProgressImportMgrImpl extends IntegratorBase implements IntegratorMgr {
	
	String costObjectID = ""; 
	
	public void test() throws SystemException {
		process("23342");
	}
	
	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		String arguments[] = getArguments(taskInternalID);
		
		costObjectID = arguments[0];
		logInfo("Progress Import begins...");
		importProgress(arguments[1], arguments[2]);
		logInfo("Progress Import ends...");
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}

//	This method will fetch the progress percent from the Microsoft Projects File and update the EcoSys Project.
	private void importProgress(String prjInternalID, String minorPeriodID) throws SystemException {
		// TODO Auto-generated method stub
		
		List<MSPUpdateProjectProgressType> lstUpdateProgress = new ArrayList<MSPUpdateProjectProgressType>();
		String mppFilePath;
		
		try {
			mppFilePath = getMPPFile(client, prjInternalID, minorPeriodID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			for(Task task : project.getTasks()) {
				
//				String cclFlag = "";
				
//				cclFlag = String.valueOf(task.getFieldByAlias("Cost Control Level"));

//				if (cclFlag.equals("Yes") & task.getPercentageComplete().doubleValue() > 0 ) {
					
				if (task.getActive() && !task.getSummary() && !task.getMilestone() && task.getPercentageWorkComplete().doubleValue() > 0 ) {					
					
					MSPUpdateProjectProgressType progressRecord = new MSPUpdateProjectProgressType();
					
					String strWBS = "", pathID = "" ;
					double progressValue;
					
					strWBS = task.getWBS();
					
					if (strWBS.contains(costObjectID)) {
						pathID = strWBS;
					}
					else {
						pathID = costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + strWBS; 
					}
					
					String wbsID = strWBS.substring(strWBS.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
					
					progressValue = (double) task.getPercentageWorkComplete();
					
					progressRecord.setObjectPathID(pathID);
					progressRecord.setObjectID(wbsID);
					progressRecord.setProgressPercent(progressValue);
					
					logDebug("Data Record : " + padRight(progressRecord.getObjectPathID(), 20)  + " | " +
							padRight(task.getName(), 40) + " | " + 
							progressRecord.getObjectID() + " | " + 
							progressRecord.getProgressPercent());
					
					lstUpdateProgress.add(progressRecord);
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
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
		parameterMap.put("RootCostObject", prjInternalID);
		
		List<MSPUpdateProjectProgressResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstUpdateProgress, MSPUpdateProjectProgressRequestType.class,
				MSPUpdateProjectProgressResultType.class, com.ecosys.mpupdateprogress.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEPROGRESS , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPUpdateProjectProgressResultType result : resultList) {
			for(com.ecosys.mpupdateprogress.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.mpupdateprogress.ObjectResultType.class, com.ecosys.mpupdateetc.ResultMessageType.class, ort);
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
