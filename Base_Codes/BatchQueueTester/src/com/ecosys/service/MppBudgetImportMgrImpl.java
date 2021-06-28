package com.ecosys.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.ecosys.ImportBudget.MSPImportBudgetRequestType;
import com.ecosys.ImportBudget.MSPImportBudgetResultType;
import com.ecosys.ImportBudget.MSPImportBudgetType;
import com.ecosys.ImportBudgetDates.MSPImportBudgetDatesRequestType;
import com.ecosys.ImportBudgetDates.MSPImportBudgetDatesResultType;
import com.ecosys.ImportBudgetDates.MSPImportBudgetDatesType;
import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;

public class MppBudgetImportMgrImpl extends IntegratorBase implements IntegratorMgr  {
	
	String costObjectID , costObjectInternalID ;
	String mppFilePath;
	ProjectFile mppProject;
	
	public void test() throws SystemException {
		process("23562");
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
			logInfo("Begin Budget Hours Import...");
			importBudgetHours(mppProject);
			logInfo("Budget Hours Import completed");
			
			logInfo("Begin Budget Dates Import...");
			importBudgetDates(mppProject);
			logInfo("Budget Dates Import completed");
		}

		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}

	//Method to Import Budget Hours in EcoSys from MPP File
	private void importBudgetHours(ProjectFile mppProjectFile) throws SystemException {
		
		List<MSPImportBudgetType> lstBudgetRecords = new ArrayList<MSPImportBudgetType>();
		
		ProjectFile project = mppProjectFile;
		String mppProjectPrefix;
		Task rootTask;
		
		try {
			rootTask = project.getTaskByID(Integer.valueOf(0));
			mppProjectPrefix = String.valueOf(rootTask.getFieldByAlias("WBS Path ID"));
			logDebug("Project Prefix : " + mppProjectPrefix);
			
			logDebug(
					padRight("WBS Path ID", 25)  + " | " +
					padRight("Description", 60)  + " | " +
					padRight("WBS ID", 10)  + " | " +
					padRight("Ext Key", 15)  + " | " +
					padRight("Res Details", 35)  + " | " +
					padRight("Budget Hours", 10)  );
			
			for(Task task : project.getTasks()) {
				
				if (task.getActive() && task.getResourceAssignments().size() > 0  ) {
					
					String strWBS , pathID , wbsID , wbsName, resourceName = null, resourceAlias = null ;
					String externalKey = costObjectID +GlobalConstants.EPC_HIERARCHY_SEPARATOR+ task.getUniqueID().toString();
					int resCount;
					double budgetHours;
									
					strWBS = (String) task.getFieldByAlias("WBS Path ID");
					pathID = pathidBuilder(costObjectID, mppProjectPrefix, strWBS);
					wbsID = pathID.substring(pathID.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
					wbsName = task.getName();
					resCount = task.getResourceAssignments().size();			
					budgetHours = Double.valueOf(task.getWork().toString().replace("h", ""));
					
					if (strWBS.equals("null")) {
						throw new SystemException("WBS Path ID Formula not defined correctly in mpp file");
					}
					
					if (budgetHours > 0) {
						
						for (ResourceAssignment assignment : task.getResourceAssignments()) {
							
							MSPImportBudgetType budgetRecord = new MSPImportBudgetType();
							
							Resource objResource = assignment.getResource();
							
							if (objResource != null) {
								
								resourceName = String.valueOf(objResource.getName());	
								resourceAlias = String.valueOf(objResource.getFieldByAlias("Resource Alias"));
							}
							
							logDebug(
									padRight(pathID, 25)  + " | " +
									padRight(wbsName, 60)  + " | " +
									padRight(wbsID, 10)  + " | " +
									padRight(externalKey, 15)  + " | " +
									padRight(resourceName + ", " + resourceAlias, 35)  + " | " +
									padRight(String.valueOf(budgetHours/resCount), 10)  );
							
							budgetRecord.setObjectPathID(pathID);
							budgetRecord.setResource(resourceAlias);
							budgetRecord.setResourceAlias(resourceName + ", " + resourceAlias);
							budgetRecord.setHours(budgetHours/resCount);
							
							lstBudgetRecords.add(budgetRecord);
						}
						
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
		
		logInfo("Count of Items to create : " + String.valueOf(lstBudgetRecords.size()));
		
		int passCnt=0 , failCnt=0;
		
		//Push Budget Records to EcoSys
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", costObjectInternalID);
		
		List<MSPImportBudgetResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstBudgetRecords, MSPImportBudgetRequestType.class,
				MSPImportBudgetResultType.class, com.ecosys.ImportBudget.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_IMPORTBUDGET , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPImportBudgetResultType result : resultList) {
			for(com.ecosys.ImportBudget.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.ImportBudget.ObjectResultType.class, com.ecosys.ImportBudget.ResultMessageType.class, ort);
					logError(ort.getExternalId() + " " +   message);
					failCnt++;
				}
				else {
					logDebug("Budget Item : " + ort.getInternalId() + " Created");
					passCnt++;
				}
				
			}
		}
    	
    	logInfo("Total created Items = " + passCnt + ", Failed Items = " + failCnt);
	}
	
	//Method to Import Budget Dates for WorkPackages in EcoSys from MPP Project File
	private void importBudgetDates(ProjectFile mppProjectFile) throws SystemException {
		
		List<MSPImportBudgetDatesType> lstBudgetDates = new ArrayList<MSPImportBudgetDatesType>();
		
		ProjectFile project = mppProjectFile;
		String mppProjectPrefix;
		Task rootTask;

		try {
			rootTask = project.getTaskByID(Integer.valueOf(0));
			mppProjectPrefix = String.valueOf(rootTask.getFieldByAlias("WBS Path ID"));
			logDebug("Project Prefix : " + mppProjectPrefix);
			
			logDebug(
					padRight("WBS Path ID", 25)  + " | " +
					padRight("WBS ID", 10)  + " | " +
					padRight("Start Date", 10)  + " | " +
					padRight("End Date", 10)  );
			
			for(Task task : project.getTasks()) { 
				
				boolean bActive = task.getActive();
				boolean bSummary = task.getSummary();
				boolean bMilestone = task.getMilestone();
				
				if (bActive && !bSummary && !bMilestone) {
					
					MSPImportBudgetDatesType dateRecord = new MSPImportBudgetDatesType();
					
					String strWBS, pathID, wbsID;
					Date startDate, endDate;
					
					strWBS = (String) task.getFieldByAlias("WBS Path ID");
					pathID = pathidBuilder(costObjectID, mppProjectPrefix, strWBS);
					wbsID = pathID.substring(pathID.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
					startDate = task.getStart();
					endDate = task.getFinish();
					
					if (strWBS.equals("null")) {
						throw new SystemException("WBS Path ID Formula not defined correctly in mpp file");
					}
					
					logDebug(
							padRight(pathID, 25)  + " | " +
							padRight(wbsID, 10)  + " | " +
							padRight(String.valueOf(startDate), 10)  + " | " +
							padRight(String.valueOf(endDate), 10)  );
					
					dateRecord.setObjectID(wbsID);
					dateRecord.setObjectPathID(pathID);
					dateRecord.setTimePhasing("LIN");
					dateRecord.setStartDate(dateToXMLGregorianCalendar(startDate));
					dateRecord.setEndDate(dateToXMLGregorianCalendar(endDate));
					
					lstBudgetDates.add(dateRecord);
					
				}
			}
		} catch (SystemException e) {
			e.printStackTrace();
			logError(e);
			logInfo("Import Terminated due to Errors");
			batchQueueMgr.updateTaskStatus(client, bqrt, GlobalConstants.BATCH_QUEUE_STATUS_ERROR);
			batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
			System.exit(1);
		}
		
		logInfo("Count of Items to create : " + String.valueOf(lstBudgetDates.size()));
		
		int passCnt=0 , failCnt=0;
		
		//Push data to EcoSys
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", costObjectInternalID);
		
		List<MSPImportBudgetDatesResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstBudgetDates, MSPImportBudgetDatesRequestType.class,
				MSPImportBudgetDatesResultType.class, com.ecosys.ImportBudgetDates.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_IMPORTBUDGETDATES , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPImportBudgetDatesResultType result : resultList) {
			for(com.ecosys.ImportBudgetDates.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.ImportBudgetDates.ObjectResultType.class, com.ecosys.ImportBudgetDates.ResultMessageType.class, ort);
					logError(ort.getExternalId() + " " +   message);
					failCnt++;
				}
				else {
					logDebug("Budget Item : " + ort.getInternalId() + " Created");
					passCnt++;
				}
			}
		}
    	
    	logInfo("Total created Items = " + passCnt + ", Failed Items = " + failCnt);
		
	}
}
