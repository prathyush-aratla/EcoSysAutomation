package com.ecosys.startup;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.ecosys.service.ActualsImportMgrImpl;
import com.ecosys.service.BQService;
import com.ecosys.service.BatchQueueMgr;
import com.ecosys.service.EpcRestMgr;
import com.ecosys.service.IntegratorMgr;

public class MainRunner {
	
	protected static Logger logger = Logger.getLogger(MainRunner.class);
	
	protected static EpcRestMgr epcRestMgr;
	
	protected static BatchQueueMgr batchQueueMgr;

	public static void main(String[] args)  throws Exception{
		// TODO Auto-generated method stub
		
		
		try {
			logger.info("********Integration Starts*********");
			
			ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
			
			logger.info("Starting BatchQueue Testing.....");
			
//			BQService service = (BQService) context.getBean("BQService", BQService.class);
//			
//			String arguments[] = service.GetArguments("22961");
//			
//			logger.info("Argument A : " + arguments[0].toString());
//			logger.info("Argument B : " + arguments[1].toString());
			
			
			
			IntegratorMgr actualsImport = (ActualsImportMgrImpl) context.getBean("ActualsImport", ActualsImportMgrImpl.class);
			
			actualsImport.test();
			
//			ActualsImportMgrImpl actualsImport = (ActualsImportMgrImpl) context.getBean("ActualsImport", ActualsImportMgrImpl.class);
//			actualsImport.test();
//			String arguments[] = actualsImport.getArguments("22961");
//			logger.info("Argument A : " + arguments[0].toString());
//			logger.info("Argument B : " + arguments[1].toString());
			
			logger.info("********Integration End*********");
			((ClassPathXmlApplicationContext) context).close();
			
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}


	}

}
