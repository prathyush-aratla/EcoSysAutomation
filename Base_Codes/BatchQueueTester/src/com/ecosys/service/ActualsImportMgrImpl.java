package com.ecosys.service;

import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;

public class ActualsImportMgrImpl extends IntegratorBase implements IntegratorMgr  {
	
	
	public void test() throws SystemException {
		process("22961");
	}

	public void process(String taskInternalID) throws SystemException {
		if (client == null) setClient(epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME, GlobalConstants.EPC_REST_PASSWORD));
		if (bqrt == null) setBqrt(this.batchQueueMgr.readTask(client, taskInternalID));
				
		String projectId = bqrt.getCostObjectHierarchyPathID();
		logInfo("BatchQueue Project ID : " + projectId);
		
		String[] prjArguments = getArguments(taskInternalID);
		
		for (String  i : prjArguments) {
			
			logInfo("Argument "+ i.toString());
			
		}
		
		batchQueueMgr.logBatchQueue(client, this.loggerList, GlobalConstants.EPC_REST_Uri);
		
	}
	
	
		
}