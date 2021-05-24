package com.ecosys.properties;

import org.springframework.beans.factory.annotation.Autowired;

public class GlobalConstants {
	
	
	public static String EPC_REST_USERNAME;
	public static String EPC_REST_PASSWORD;
	
	public static String EPC_REST_PROTOCOL;
	public static String EPC_REST_SERVER;
	public static String EPC_REST_PORT;
	public static String EPC_REST_BASEURI;
	
	public static String EPC_REST_Uri;
	
	public static String EPC_API_INVOICE;
	public static String EPC_API_GETMETCFILE;
	public static String EPC_API_GETMPRJFILE;
	
	@Autowired
	protected AppProperties appProperties;

	//Setter Method
	public void setAppProperties(AppProperties appProperties) {
		this.appProperties = appProperties;
		
		EPC_REST_USERNAME = this.appProperties.getProperty("epc.rest.username");
		EPC_REST_PASSWORD = this.appProperties.getProperty("epc.rest.password");
				
		EPC_REST_PROTOCOL = this.appProperties.getProperty("epc.rest.protocol");
		EPC_REST_SERVER = this.appProperties.getProperty("epc.rest.server");
		EPC_REST_PORT = this.appProperties.getProperty("epc.rest.port");
		EPC_REST_BASEURI = this.appProperties.getProperty("epc.rest.baseuri");
		
		EPC_REST_Uri = GlobalConstants.EPC_REST_PROTOCOL + "://" + GlobalConstants.EPC_REST_SERVER + ":" + GlobalConstants.EPC_REST_PORT + "/" + GlobalConstants.EPC_REST_BASEURI;
		
		
		EPC_API_INVOICE = this.appProperties.getProperty("epc.api.invoicedetails");
		
		EPC_API_GETMETCFILE = this.appProperties.getProperty("epc.api.getmetcfile");
		
		EPC_API_GETMPRJFILE = this.appProperties.getProperty("epc.api.getmprjfile");
		
		
		
	}


}
