package com.ecosys.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.ecosys.ImportETC.MSPPutMppDataRequestType;
import com.ecosys.ImportETC.MSPPutMppDataResultType;
import com.ecosys.ImportETC.MSPPutMppDataType;

import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;


public class MppEtcImportMgrImpl extends IntegratorBase implements IntegratorMgr  {
	
	String costObjectID , costObjectInternalID, minorPeriodID ;
	String mppFilePath;
	ProjectFile mppProject;
	
	public void test() throws SystemException {
		process("23567");
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
			logInfo("ETC Import begins...");
			importETCHours(mppProject);
			logInfo("ETC Import completed");		
		}

		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}

	//Method to update Estimate-To-Complete Hours in EcoSys from MPP File
	private void importETCHours(ProjectFile mppProjectFile) throws SystemException {
		
		List<MSPPutMppDataType> lstETCRecords = new ArrayList<MSPPutMppDataType>();
		
		ProjectFile project = mppProjectFile;
		String mppProjectPrefix;
		Task rootTask;
		
		try {
			rootTask = project.getTaskByID(Integer.valueOf(0));
			mppProjectPrefix = String.valueOf(rootTask.getFieldByAlias("WBS Path ID"));
			logDebug("Project Prefix : " + mppProjectPrefix);
			
			logDebug(
					padRight("WBS Path ID", 25)  + " | " +
					padRight("Description", 75)  + " | " +
					padRight("WBS ID", 10)  + " | " +
					padRight("Ext Key", 10)  + " | " +
					padRight("Res. Count", 10)  + " | " +
					padRight("Rem. Hours", 10)  );
			
			for(Task task : project.getTasks()) {
				
				boolean bActive;
				int resCount;
				String strWBS , pathID , wbsID , wbsName, externalKey, resourceName = null, resourceAlias = null ;
				Date startDate, endDate;
				double totalRemHrs;
				
				bActive = task.getActive();
				resCount = task.getResourceAssignments().size();
				externalKey = costObjectID +GlobalConstants.EPC_HIERARCHY_SEPARATOR+ task.getUniqueID().toString();
				startDate = task.getStart();
				endDate = task.getFinish();
				
				
				if ( bActive && resCount > 0 ) {
					strWBS = (String) task.getFieldByAlias("WBS Path ID");
					pathID = pathidBuilder(costObjectID, mppProjectPrefix, strWBS);
					wbsID = pathID.substring(pathID.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
					wbsName = task.getName();
					resCount = task.getResourceAssignments().size();			
					totalRemHrs = Double.valueOf(task.getRemainingWork().toString().replace("h", ""));
					
					if (strWBS.equals("null")) {
						throw new SystemException("WBS Path ID Formula not defined correctly in mpp file");
					}
					
					logDebug(
							padRight(pathID, 25)  + " | " +
							padRight(wbsName, 75)  + " | " +
							padRight(wbsID, 10)  + " | " +
							padRight(externalKey, 10)  + " | " +
							padRight(String.valueOf(resCount), 10)  + " | " +
							padRight(String.valueOf(totalRemHrs), 10)  );
					
					if (totalRemHrs > 0) {
						
						for (ResourceAssignment assignment : task.getResourceAssignments()) {
							
							MSPPutMppDataType etcRecord = new MSPPutMppDataType();
							
							Resource objResource = assignment.getResource();
							
							if (objResource != null) {
								
								resourceName = String.valueOf(objResource.getName());	
								resourceAlias = String.valueOf(objResource.getFieldByAlias("Resource Alias"));
							}
													
							etcRecord.setObjectPathID(pathID);
							etcRecord.setResource(resourceAlias);
							etcRecord.setResourceAlias(resourceName + ", " + resourceAlias);
							etcRecord.setMPPETC(totalRemHrs/resCount);
							etcRecord.setStartDate(dateToXMLGregorianCalendar(startDate));
							etcRecord.setEndDate(dateToXMLGregorianCalendar(endDate));
							
							lstETCRecords.add(etcRecord);
							
						}
						
					}
					else {
						logDebug("Resources in Path ID " + pathID + " skipped due to no hours left.");
					}
				}	
			}
		}
		
		catch (SystemException e) {
			// TODO: handle exception
			e.printStackTrace();
			logError(e);
			logInfo("Import Terminated due to Errors");
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
			batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
			System.exit(1);
		}
		
		logInfo("Count of Transactions prepared : " + String.valueOf(lstETCRecords.size()));
		
		int passCnt=0 , failCnt=0;
		
		//Push ETC Records to EcoSys
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", costObjectInternalID);
		parameterMap.put("ProjectPeriod", minorPeriodID);
		
		List<MSPPutMppDataResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstETCRecords, MSPPutMppDataRequestType.class,
				MSPPutMppDataResultType.class, com.ecosys.ImportETC.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEETC , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppDataResultType result : resultList) {
			for(com.ecosys.ImportETC.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.ImportETC.ObjectResultType.class, com.ecosys.ImportETC.ResultMessageType.class, ort);
					logError(ort.getExternalId() + " " +   message);
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
