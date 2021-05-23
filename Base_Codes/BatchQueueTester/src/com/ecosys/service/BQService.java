package com.ecosys.service;


import org.springframework.beans.factory.annotation.Autowired;

import com.ecosys.exception.SystemException;
import com.ecosys.properties.GlobalConstants;
import com.sun.jersey.api.client.Client;

public class BQService {
	
	protected EpcRestMgr epcRestMgr;
	public String taskID = "";
	
	@Autowired
	protected BatchQueueMgr batchQueueMgr;


	public void setEpcRestMgr(EpcRestMgr epcRestMgr) {
		this.epcRestMgr = epcRestMgr;
	}
	
	public EpcRestMgr getEpcRestMgr() {
		return epcRestMgr;
	}


	public void setBatchQueueMgr(BatchQueueMgr batchQueueMgr) {
		this.batchQueueMgr = batchQueueMgr;
	}



	public String[] GetArguments(String taskID) throws SystemException {
		
		try {
			Client client = this.epcRestMgr.createClient(GlobalConstants.EPC_REST_USERNAME,
					GlobalConstants.EPC_REST_PASSWORD);
			
			String args[]=batchQueueMgr.readTask(client,taskID).getBatchQueueParam1ID().split("\\|");
			
			return args;
			
		} catch (Exception e) {
			// TODO: handle exception
			
			throw new SystemException(e);
		}
	}
	

}
