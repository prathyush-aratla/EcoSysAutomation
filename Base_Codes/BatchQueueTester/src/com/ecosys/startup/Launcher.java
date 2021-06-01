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
				
		Stopwatch timerTotal = new Stopwatch();
		timerTotal.start();
		
		int error_code = 0;
		
		try {
			logger.info("Starting Microsoft Project Data Import Process...");
			
			ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");		
			IntegratorMgr mppIntegration = (MppIntegrationImpl) context.getBean("MppIntegration", MppIntegrationImpl.class);
			
			if (args.length == 0 ) {
				
				mppIntegration.test();
				
			}
			else {
				
				String taskInternalID = args[0];
				
				mppIntegration.process(taskInternalID);
				
			}
			
			logger.info("Completed Microsoft Project Data Import Process... " + timerTotal.stop().toString(ISOPeriodFormat.alternateExtended()));	
			
			((ClassPathXmlApplicationContext) context).close();
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			logger.error(e.getMessage());
			error_code = 1;
		}
	
		System.exit(error_code);
	}

}
