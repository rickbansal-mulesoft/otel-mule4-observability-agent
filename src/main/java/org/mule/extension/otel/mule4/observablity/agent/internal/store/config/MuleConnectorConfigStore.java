package org.mule.extension.otel.mule4.observablity.agent.internal.store.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExpressionManager;
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
/**
 *  Singleton {@code MuleConnectorConfigStore} class for storing various configuration details on 
 *  a variety of MuleSoft connectors.  For now the following connector types are supported:
 *  <ul>
 *  	<li> {@code Database Connector} </li>
 *  	<li> {@code HTTP Request Connector} </li>
 *  </ul>
 *  
 *  @see #getInstance(MuleConfiguration)
 */


public class MuleConnectorConfigStore
{
	private static Logger logger = LoggerFactory.getLogger(MuleConnectorConfigStore.class);
	
	private static MuleConnectorConfigStore muleConnectorConfigStore = null;

	private static ExpressionManager expressionManager = null;
	
	//--------------------------------------------------------------------------------------------
	//	Collection of configuration details for a variety of Mule connectors.  
	//--------------------------------------------------------------------------------------------
	private Map<String, Object> configurations = new ConcurrentHashMap<>();
	
	//--------------------------------------------------------------------------------------------
	//	Singleton constructor  
	//--------------------------------------------------------------------------------------------
	private MuleConnectorConfigStore(MuleConfiguration muleConfiguration, ExpressionManager em)
	{
	    DefaultMuleConfiguration defaultMuleConfiguration = (DefaultMuleConfiguration)muleConfiguration;
		
	    expressionManager = em;
	    
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    
	    String httpNS = Constants.HTTP_REQUESTER_URI_NS;
	    String dbNS   = Constants.DB_URI_NS;
	    String anypointMQNS = Constants.ANYPOINT_MQ_URI_NS;

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
			    	//	Create a DOM Document Builder and parse in the Mule Configuration XML file
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
							String protocol = e2.getAttribute("protocol");
							
					    	//--------------------------------------------------------------------
							// 	Create and store a HttpRequesterConfig into the configuration store
					    	//--------------------------------------------------------------------							
							configurations.put(configName, new HttpRequesterConfig(host,
									                                               port == "" ? "80": port,
									                                               protocol == "" ? "HTTP": protocol));
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
							String type = e2.getLocalName();
							String host = e2.getAttribute("host");
							String port = e2.getAttribute("port");
							String user = e2.getAttribute("user");
							String database = e2.getAttribute("database");
							
					    	//--------------------------------------------------------------------
							// 	Create and store a DbConfig into the configuration store
					    	//--------------------------------------------------------------------							
							configurations.put(configName, new DbConfig(host, port, user, database, type));
						}
					}
					
                    //----------------------------------------------------------------------------
                    //  Process all of the Anypoint MQ Configurations in the XML file
                    //----------------------------------------------------------------------------
                    NodeList anypointMQConfigNodesList = doc.getElementsByTagNameNS(anypointMQNS, "config");
                    int anyppointMQConfigNodes = anypointMQConfigNodesList.getLength();
                    
                    for (int i = 0; i < anyppointMQConfigNodes; i++)
                    {
                        Node node = anypointMQConfigNodesList.item(i);
                        
                        if (node.getNodeType() == Node.ELEMENT_NODE)
                        {
                            Element e = (Element) node;
                            String configName = e.getAttribute("name");
                            
                            Element e2 = (Element)e.getElementsByTagNameNS(anypointMQNS, "connection").item(0);
                            String url = e2.getAttribute("url");
                            String clientId = e2.getAttribute("clientId");
                            
                            //--------------------------------------------------------------------
                            //  Create and store a DbConfig into the configuration store
                            //--------------------------------------------------------------------                          
                            configurations.put(configName, new AnypointMQConfig(url, clientId));
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
	 * Retrieve the singleton instance of this MuleConnectorConfigStore object. 
	 * 
	 * @param muleConfiguration - Default Mule Configuration
	 * 
	 * @return Singleton {@code MuleConnectorConfigStore}
	 */
	public static synchronized MuleConnectorConfigStore getInstance(MuleConfiguration muleConfiguration,
	                                                                ExpressionManager expressionManager)
	{
		if (muleConnectorConfigStore == null)
		{
			muleConnectorConfigStore = new MuleConnectorConfigStore(muleConfiguration, expressionManager);
		}
		return muleConnectorConfigStore;
	}
	
	//------------------------------------------------------------------------------------------------
	//	Retrieve either a DB, Anypoint MQ or HTTP Requester Configuration
	//------------------------------------------------------------------------------------------------
	/**
	 * 	Retrieve either a DB, Anypoint MQ or HTTP Requester Configuration
	 * 
	 * @param key - name of the {@code config-reg} to retrieve
	 * 
	 * @return {@code DbConfig} or {@code HttpRequesterConfig }
	 */
	public <T> T getConfig(String key)
	{
		return (T) configurations.get(key);
	}
	
	public static ExpressionManager getExpressionManager()
	{
	    return expressionManager;
	}
	
	
	public static String resolveProperty(String property)
	{
	    String value = property;
	    String exp;
	    
	    try
	    {
    	    //
    	    //  Check if property is a placeholder
    	    //
            //if (property.matches("^\${.*}$"))
            if (property.startsWith("${") && property.endsWith("}"))
    	    {
    	        // strip off beginning and ending of placeholder syntax
    	        property = property.substring(2, property.length()-1);
    	        
    	        // create expression for evaluating a property
    	        exp = "#[p(\"" + property + "\")]";
    	        
    	        value = (String) expressionManager.evaluate(exp).getValue();
    	    }
    	    else if (expressionManager.isExpression(property))
    	    {
    	        value = (String) expressionManager.evaluate(property).getValue();
    	    }
	    }
	    catch (Exception e)
	    {
	        logger.debug(e.getMessage());
	    }
	    
	    return value;
	}
	
	//------------------------------------------------------------------------------------------------
    //  Nested class for storing some configuration details on the Anypoint MQ Connector.
    //------------------------------------------------------------------------------------------------
    public static class AnypointMQConfig
    {
        private String clientId;
        private URL url;
        
        public AnypointMQConfig(String url, String clientId) throws MalformedURLException
        {
            this.url = new URL(resolveProperty(url));
            this.clientId = resolveProperty(clientId);
        }
        
        public String getClientId()
        {
            return clientId;
        }
        
        public String getUrl()
        {
            return url.toExternalForm();
        }
        
        public String getHost()
        {
            return url.getHost();
        }
        
        public String getPort()
        {
            int port = url.getPort();
            
            return (port == -1) ? Integer.toString(url.getDefaultPort()) : Integer.toString(port);
        }
        
        public String getProtocol()
        {
            return url.getProtocol();
        }
        
        public String getPath()
        {
            return url.getPath();
        }
    }
    

	//------------------------------------------------------------------------------------------------
	//	Nested class for storing some configuration details on the HTTP Requester.
	//------------------------------------------------------------------------------------------------
	public static class HttpRequesterConfig
	{
		private String host;
		private String port;
		private String protocol;
		
		public HttpRequesterConfig(String host, String port, String protocol)
		{
		    this.host = resolveProperty(host);
		    this.port = resolveProperty(port);
		    this.protocol = resolveProperty(protocol);
		}
		
		public String getHost()
		{
			return host;
		}
		
		public String getPort()
		{
			return port;
		}
		
		public String getProtocol()
		{
			return protocol;
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
		private String connectionType;
		
		public DbConfig(String host, String port, String user, String dbName, String connectionType)
		{
		    this.host = resolveProperty(host);
		    this.port = resolveProperty(port);
		    this.user = resolveProperty(user);
		    this.dbName = resolveProperty(dbName);
		    this.connectionType = resolveProperty(connectionType);
	
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
		
		public String getConnectionType() 
		{
			return connectionType;
		}
	}	
}
