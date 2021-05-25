package com.ecosys.service;

import com.ecosys.rest.BatchQueueRead.BatchQueueReadType;
import com.sun.jersey.api.client.Client;
import com.ecosys.exception.SystemException;

public interface IntegratorMgr {
	
	public void setClient(Client client);
	public void setBqrt(BatchQueueReadType bqrt);
	public void test() throws SystemException;
	public void process(String projectId) throws SystemException;
	//public String[] getArguments(String taskInternalID) throws SystemException;
	

}
