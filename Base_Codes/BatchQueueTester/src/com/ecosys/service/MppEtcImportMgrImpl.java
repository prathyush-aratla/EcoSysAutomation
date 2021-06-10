package com.ecosys.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.ecosys.exception.SystemException;
import com.ecosys.mpupdateetc.MSPPutMppDataRequestType;
import com.ecosys.mpupdateetc.MSPPutMppDataResultType;
import com.ecosys.mpupdateetc.MSPPutMppDataType;
import com.ecosys.properties.GlobalConstants;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectConfig;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskField;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.reader.ProjectReader;

public class MppEtcImportMgrImpl extends IntegratorBase implements IntegratorMgr  {
	
	
	
	String costObjectID = ""; 
	
	public void test() throws SystemException {
		process("24546");
	}
	
	public void process(String taskInternalID) throws SystemException{
		
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
		String arguments[] = getArguments(taskInternalID);
		
		costObjectID = arguments[0];
		logInfo("Progress Import begins...");
		importETCHours(arguments[1], arguments[2]);
		logInfo("Progress Import ends...");
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
	}

	//Method to update Estimate-To-Complete Hours in EcoSys from MPP File
	private void importETCHours(String prjInternalID, String minorPeriodID) throws SystemException {
		
//		List<ResourcesType> m_resources = getEpcResources();
//		
//		for (ResourcesType presource : m_resources) {
//			logDebug(presource.getID() + " | " + presource.getName() + " | " + presource.getInternalID());
//		}
		
		// TODO Auto-generated method stub
		
		List<MSPPutMppDataType> lstETCRecords = new ArrayList<MSPPutMppDataType>();
		String mppFilePath;
		try {
			mppFilePath = getMPPFile(client, prjInternalID, minorPeriodID);
			InputStream input = new URL(mppFilePath).openStream();
			ProjectReader reader = new MPPReader();
			ProjectFile project = reader.read(input);
			
			ProjectConfig config = project.getProjectConfig();
			
			config.setAutoWBS(false);
			
			
//			project.getTasks().synchronizeTaskIDToHierarchy();
			
			for(Task task : project.getTasks()) {
				
				if (task.getActive() == true && task.getResourceAssignments().size() > 0  ) {
					
					@SuppressWarnings("unused")
					String resourceName = "", resourceAlias = "", strWBS = "", wbsPathID = "", origWBS= "";
					@SuppressWarnings("unused")
					String externalKey = costObjectID +GlobalConstants.EPC_HIERARCHY_SEPARATOR+ task.getUniqueID().toString();
					int resCount;
					double totalRemHrs;
					
					Date startDate = task.getStart();
					Date endDate = task.getFinish();
					
					origWBS = task.getWBS();
					strWBS = task.getParentTask().getWBS();
					
					if(origWBS.contains(costObjectID)) {
						wbsPathID = origWBS;
					}
					else {
						wbsPathID = costObjectID + GlobalConstants.EPC_HIERARCHY_SEPARATOR + origWBS; 
					}
					
					resCount = task.getResourceAssignments().size();			
					totalRemHrs = Double.valueOf(task.getRemainingWork().toString().replace("h", ""));
					
					logDebug(
							"Name : " +  padRight(task.getName(), 50)  +
							" | WBS : " + task.getWBS() +
							" | Outline : " + task.getOutlineNumber() +
							" | WBS Code : " + task.getFieldByAlias("WBS Code") +
							" | Rem Hrs: " + task.getRemainingWork() +
							" | " );
					
					if (totalRemHrs > 0) {
						
						for (ResourceAssignment assignment : task.getResourceAssignments()) {
							
							MSPPutMppDataType etcRecord = new MSPPutMppDataType();
							
							Resource objResource = assignment.getResource();
							
							if (objResource != null) {
								
								resourceName = String.valueOf(objResource.getName());	
								resourceAlias = String.valueOf(objResource.getFieldByAlias("Resource Alias"));
							}
							
//							logDebug("Record Details : " + origWBS + ", " +
//											resourceName + ", " +
//											resourceAlias + ", " +
//											(totalRemHrs/resCount) + " hrs, "
//											);
							
							etcRecord.setObjectPathID(wbsPathID);
							etcRecord.setResource(resourceAlias);
							etcRecord.setMPPETC(totalRemHrs/resCount);
							etcRecord.setStartDate(dateToXMLGregorianCalendar(startDate));
							etcRecord.setEndDate(dateToXMLGregorianCalendar(endDate));
							
							lstETCRecords.add(etcRecord);
							
						}
						
					}
					else {
//						logDebug("Resources in Path ID " + wbsPathID + " skipped due to no hours left.");
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
		
		logInfo("Count of Transactions prepared : " + String.valueOf(lstETCRecords.size()));
		
		int passCnt=0 , failCnt=0;
		
		//Push ETC Records to EcoSys
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		parameterMap.put("RootCostObject", prjInternalID);
		parameterMap.put("ProjectPeriod", minorPeriodID);
		
		List<MSPPutMppDataResultType>  resultList = this.epcRestMgr.postXMLRequestInBatch(client, lstETCRecords, MSPPutMppDataRequestType.class,
				MSPPutMppDataResultType.class, com.ecosys.mpupdateetc.ObjectFactory.class, GlobalConstants.EPC_REST_Uri, GlobalConstants.EPC_API_UPDATEETC , GlobalConstants.EPC_REST_BATCHSIZE, parameterMap, true);
		
    	for(MSPPutMppDataResultType result : resultList) {
			for(com.ecosys.mpupdateetc.ObjectResultType ort : result.getObjectResult()) {
				if(!ort.isSuccessFlag()) {
					String message = this.epcRestMgr.getErrorMessage(com.ecosys.mpupdateetc.ObjectResultType.class, com.ecosys.mpupdateetc.ResultMessageType.class, ort);
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
