package com.ecosys.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.ecosys.ImportBudget.MSPImportBudgetRequestType;
import com.ecosys.ImportBudget.MSPImportBudgetResultType;
import com.ecosys.ImportBudget.MSPImportBudgetType;
import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;

public class MppBudgetImportMgrImpl extends IntegratorBase implements IntegratorMgr  {
	
	String costObjectID , costObjectInternalID ;
	
	public void test() throws SystemException {
		process("24546");
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
			logInfo("Begin Budget Hours Import...");
			importBudgetHours(costObjectInternalID);
			logInfo("Budget Hours Import completed");
			
			logInfo("Begin Budget Dates Import...");
			importBudgetDates(costObjectInternalID);
			logInfo("Budget Dates Import completed");
		}

		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}

	//Method to Import Budget Hours in EcoSys from MPP File
	private void importBudgetHours(String prjInternalID) throws SystemException {
		
		List<MSPImportBudgetType> lstBudgetRecords = new ArrayList<MSPImportBudgetType>();
		
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
					padRight("WBS Path ID", 25)  + " | " +
					padRight("Description", 75)  + " | " +
					padRight("WBS ID", 10)  + " | " +
					padRight("Resource ID", 10)  + " | " +
					padRight("Budget Hours", 10)  );
			
			for(Task task : project.getTasks()) {
				
				if (task.getActive() && task.getResourceAssignments().size() > 0  ) {
					
					@SuppressWarnings("unused")
					String strWBS , pathID , wbsID , wbsName, resourceName, resourceAlias = null ;
					@SuppressWarnings("unused")
					String externalKey = costObjectID +GlobalConstants.EPC_HIERARCHY_SEPARATOR+ task.getUniqueID().toString();
					int resCount;
					double budgetHours;
					
					Date startDate = task.getStart();
					Date endDate = task.getFinish();
					
					strWBS = (String) task.getFieldByAlias("WBS Path ID");
					pathID = pathIdBuilder(costObjectID, mppProjectPrefix, strWBS);
					wbsID = pathID.substring(pathID.lastIndexOf(GlobalConstants.EPC_HIERARCHY_SEPARATOR) + 1);
					wbsName = task.getName();
					resCount = task.getResourceAssignments().size();			
					budgetHours = Double.valueOf(task.getWork().toString().replace("h", ""));
					
					if (strWBS.equals("null")) {
						throw new SystemException("WBS Path ID Formula not defined correctly in mpp file");
					}
					
					logDebug(
							padRight(pathID, 25)  + " | " +
							padRight(wbsName, 75)  + " | " +
							padRight(wbsID, 10)  + " | " +
							padRight(String.valueOf(resCount), 10)  + " | " +
							padRight(String.valueOf(budgetHours), 10)  );
					
					if (budgetHours > 0) {
						
						for (ResourceAssignment assignment : task.getResourceAssignments()) {
							
							MSPImportBudgetType budgetRecord = new MSPImportBudgetType();
							
							Resource objResource = assignment.getResource();
							
							if (objResource != null) {
								
								resourceName = String.valueOf(objResource.getName());	
								resourceAlias = String.valueOf(objResource.getFieldByAlias("Resource Alias"));
							}
							
							budgetRecord.setObjectPathID(pathID);
							budgetRecord.setResource(resourceAlias);
							budgetRecord.setHours(budgetHours/resCount);
							
							lstBudgetRecords.add(budgetRecord);
						}
						
					}
					else {
//						logDebug("Resources in Path ID " + pathID + " skipped due to no hours left.");
					}
				}	
			}
		}
		
		catch (SystemException | IOException | MPXJException e) {
			// TODO: handle exception
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
		parameterMap.put("RootCostObject", prjInternalID);
		
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
	
	private void importBudgetDates(String prjInternalID) throws SystemException {
		
	}
}
