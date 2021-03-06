package com.ecosys.startup;

import org.apache.log4j.Logger;
import org.joda.time.format.ISOPeriodFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.ecosys.properties.AppProperties;
import com.ecosys.service.IntegratorMgr;
import com.ecosys.service.MppWbsImportMgrImpl;
import com.ecosys.util.Stopwatch;

public class MppWbsImport {
	
	protected static Logger logger = Logger.getLogger(MppWbsImport.class);
	
	private static ApplicationContext springctx;
	@SuppressWarnings("unused")
	private static AppProperties appProperties;


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Stopwatch timerTotal = new Stopwatch();
		timerTotal.start();
		
		int error_code = 0;
		
		try {
			logger.info("Start Import WBS from Microsoft Project file");
			
			springctx = new ClassPathXmlApplicationContext("applicationContext.xml");
			
			appProperties = (AppProperties) springctx.getBean("appProperties",AppProperties.class);
			
			IntegratorMgr mppImportMgr = (MppWbsImportMgrImpl) springctx.getBean("MppWbsImport", MppWbsImportMgrImpl.class);
			
			if (args.length == 0 ) {
				mppImportMgr.test();
			}
			else {
				String taskInternalID = args[0];
				mppImportMgr.process(taskInternalID);
			}
			
			logger.info("Completed WBS Import Process... " + timerTotal.stop().toString(ISOPeriodFormat.alternateExtended()));	
			
			((ClassPathXmlApplicationContext) springctx).close();
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
