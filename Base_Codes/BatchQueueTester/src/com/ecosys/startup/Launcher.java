package com.ecosys.startup;

import org.apache.log4j.Logger;
import org.joda.time.format.ISOPeriodFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.ecosys.service.BatchQueueMgr;
import com.ecosys.service.EpcRestMgr;
import com.ecosys.service.IntegratorMgr;
import com.ecosys.service.MppIntegrationImpl;
import com.ecosys.util.Stopwatch;

public class Launcher {
	
	protected static Logger logger = Logger.getLogger(Launcher.class);
	
	protected static EpcRestMgr epcRestMgr;
	
	protected static BatchQueueMgr batchQueueMgr;


	public static void main(String[] args)  throws Exception{
		// TODO Auto-generated method stub
		
		String taskInternalID;
		
		taskInternalID = args[0];
		
		Stopwatch timerTotal = new Stopwatch();
		timerTotal.start();
		
		int error_code = 0;
		
		try {
			logger.info("********Integration Starts*********");
			
			ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
			
			logger.info("Starting BatchQueue Testing.....");
			
			IntegratorMgr mppIntegration = (MppIntegrationImpl) context.getBean("MppIntegration", MppIntegrationImpl.class);
			
//			mppIntegration.test();
			
			mppIntegration.process(taskInternalID);
			
			logger.info("----> Time taken : " + timerTotal.stop().toString(ISOPeriodFormat.alternateExtended()));
			
			logger.info("********Integration End*********");
			
			((ClassPathXmlApplicationContext) context).close();
			
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			error_code = -1;
		}
	
		System.exit(error_code);
		

	}

}