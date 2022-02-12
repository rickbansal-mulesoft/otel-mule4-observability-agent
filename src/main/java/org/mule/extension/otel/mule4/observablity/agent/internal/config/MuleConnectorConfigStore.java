package org.mule.extension.otel.mule4.observablity.agent.internal.config;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.mule.extension.otel.mule4.observablity.agent.internal.util.ObservabilitySemantics;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;

//------------------------------------------------------------------------------------------------
//	Class for storing various configuration details on a variety of MuleSoft connectors (e.g., DB 
//	Connector, HTTP Requester, ...).  This information is not available through the current APIs;
//	hence, it is being collected by parsing through all of the Mule configuration XML files.
//------------------------------------------------------------------------------------------------
public class MuleConnectorConfigStore
{
	private static Logger logger = LoggerFactory.getLogger(MuleConnectorConfigStore.class);
	
	private static MuleConnectorConfigStore muleConnectorConfigStore = null;

	//--------------------------------------------------------------------------------------------
	//	Collection of configuration details for a variety of Mule connectors.  
	//--------------------------------------------------------------------------------------------
	private Map<String, Object> configurations = new ConcurrentHashMap<>();
	
	private MuleConnectorConfigStore(MuleConfiguration muleConfiguration)
	{
	    DefaultMuleConfiguration defaultMuleConfiguration = (DefaultMuleConfiguration)muleConfiguration;;
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    
	    String httpNS = ObservabilitySemantics.HTTP_REQUESTER_URI_NS;
	    String dbNS   = ObservabilitySemantics.DB_URI_NS;

		if (muleConfiguration != null)
		{			
			String dirPath = defaultMuleConfiguration.getMuleHomeDirectory() + "/apps/" + 
                             defaultMuleConfiguration.getDataFolderName();
	
			Iterator<File> itFiles = FileUtils.iterateFiles(new File(dirPath), new SuffixFileFilter(".xml"), null);
			
			//------------------------------------------------------------------------------------
			//	Process all XML files in the data folder
			//------------------------------------------------------------------------------------			
			while (itFiles.hasNext())
			{
				logger.debug("Application source files folder: " + dirPath);
			
			    try 
			    {
			    	//----------------------------------------------------------------------------
			    	//	Create a DOM Document Builder and parse in the Mule Configuration XML file.
			    	//----------------------------------------------------------------------------
					DocumentBuilder db = dbf.newDocumentBuilder();
		
					Document doc = db.parse(itFiles.next());
					doc.getDocumentElement().normalize();
	
			    	//----------------------------------------------------------------------------
			    	//	Process all of the HTTP Requester Configurations in the XML file
			    	//----------------------------------------------------------------------------
					NodeList httpRequesterConfigNodesList = doc.getElementsByTagNameNS(httpNS, "request-config");
					int httpRequesterConfigNodes = httpRequesterConfigNodesList.getLength();
					
					for (int i = 0; i < httpRequesterConfigNodes; i++)
					{
						Node node = httpRequesterConfigNodesList.item(i);
						
						if (node.getNodeType() == Node.ELEMENT_NODE)
						{
							Element e = (Element) node;
							String configName = e.getAttribute("name");
							
							Element e2 = (Element)e.getElementsByTagNameNS(httpNS, "request-connection").item(0);
							String host = e2.getAttribute("host");
							String port = e2.getAttribute("port");
							
							configurations.put(configName, new HttpRequesterConfig(host, (port == "" ? "80": port)));
						}
					}
	
			    	//----------------------------------------------------------------------------
			    	//	Process all of the Database Configurations in the XML file
			    	//----------------------------------------------------------------------------
					NodeList dbConfigNodesList = doc.getElementsByTagNameNS(dbNS, "config");
					int dbConfigNodes = dbConfigNodesList.getLength();
					
					for (int i = 0; i < dbConfigNodes; i++)
					{
						Node node = dbConfigNodesList.item(i);
						
						if (node.getNodeType() == Node.ELEMENT_NODE)
						{
							Element e = (Element) node;
							String configName = e.getAttribute("name");
							
							Element e2 = (Element)e.getElementsByTagNameNS(dbNS, "*").item(0);
							String host = e2.getAttribute("host");
							String port = e2.getAttribute("port");
							String user = e2.getAttribute("user");
							String database = e2.getAttribute("database");
							
							configurations.put(configName, new DbConfig(host, port, user, database));
						}
					}
			    }
			    catch (Exception e)
			    {
			    	logger.debug(e.getMessage());
			    }
			}
		}	
	}
	
	//------------------------------------------------------------------------------------------------
	//	Retrieve the singleton instance of this MuleConnectorConfigStore object.  Create the singleton 
	//	if it doesn't already exist.
	//------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param muleConfiguration
	 * @param notification
	 * @return
	 */
	public static synchronized MuleConnectorConfigStore getInstance(MuleConfiguration muleConfiguration)
	{
		if (muleConnectorConfigStore == null)
		{
			muleConnectorConfigStore = new MuleConnectorConfigStore(muleConfiguration);
		}
		return muleConnectorConfigStore;
	}
	
	//------------------------------------------------------------------------------------------------
	//	Nested class for storing some configuration details on the HTTP Requester.
	//------------------------------------------------------------------------------------------------
	public static class HttpRequesterConfig
	{
		private String host;
		private String port;
		
		public HttpRequesterConfig(String host, String port)
		{
			this.host = host;
			this.port = port;
		}
		
		public String getHost()
		{
			return host;
		}
		
		public String getPort()
		{
			return port;
		}
	}
	
	//------------------------------------------------------------------------------------------------
	//	Nested class for storing some configuration details on the Database Connector.
	//------------------------------------------------------------------------------------------------
	public static class DbConfig
	{
		private String host;
		private String port;
		private String user;
		private String dbName;
		
		public DbConfig(String host, String port, String user, String dbName)
		{
			this.host = host;
			this.port = port;
			this.user = user;
			this.dbName = dbName;
		}
		
		public String getHost()
		{
			return host;
		}
		
		public String getPort()
		{
			return port;
		}
		
		public String getUser()
		{
			return user;
		}
		
		public String getDbName()
		{
			return dbName;
		}
	}	
	
	public Object getConfig(String key)
	{
		return configurations.get(key);
	}
}
