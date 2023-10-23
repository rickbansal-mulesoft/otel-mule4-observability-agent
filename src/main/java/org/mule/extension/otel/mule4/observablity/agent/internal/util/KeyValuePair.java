package org.mule.extension.otel.mule4.observablity.agent.internal.util;

import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//----------------------------------------------------------------------------------
//	General class for storing a <key,value> pair as a Mule Parameter
//----------------------------------------------------------------------------------
public abstract class KeyValuePair
{

	@Parameter
	private String key;

	@Parameter
	private String value;

    private static Logger logger = LoggerFactory.getLogger(KeyValuePair.class);

	public String getKey()
	{
		return key;
	}

	public String getValue()
	{
		return value;
	}

	public String getKey(ExpressionManager em, EnrichedServerNotification n)
	{
	    return resolve(key, em, n);
	}

	public String getValue(ExpressionManager em, EnrichedServerNotification n)
	{
	    return resolve(value, em, n);
	}
	
	//------------------------------------------------------------------------------
	// 	Helper method for converting a List<KeyValuePair> to a comma separated
	//  string with the following format:
	//  "key1=value1, key2=value2, key3=value3, ..."
	//------------------------------------------------------------------------------
	public static String commaSeparatedList(List<? extends KeyValuePair> pairs)
	{
		if (pairs == null)
			return "";
		
		return pairs.stream().map(KeyValuePair::toString).collect(Collectors.joining(","));
	}

	//------------------------------------------------------------------------------------------------
	//  Dynamically resolve the value of a configuration property
	//------------------------------------------------------------------------------------------------
	/**
	 *  Dynamically resolve the value of a configuration property
	 * 
     * @param property - property to resolve
     * @param em - reference to the ExpressionManager instance
	 * 
	 * @return resolved value
	 */ 
	private String resolve(String property, ExpressionManager em, EnrichedServerNotification n)
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

	            value = (String) em.evaluate(exp).getValue();
	        }
	        else if (property.startsWith("\"#[") && property.endsWith("]\""))
	        {
                // strip off beginning and ending of extra quotes
                exp = property.substring(1, property.length()-1);

                //
                // The only way I got DW or MEL expressions which are part  of a static configuration to 
                // dynamically resolve was by creating new session on the ExpressionManager and using that 
                // session to evaluate the expressions
                //
                ExpressionLanguageSession els = em.openSession(n.getEvent().asBindingContext());
                value = (String) els.evaluate(exp, DataType.STRING).getValue();                   
	        }
	        else if (em.isExpression(property))
	        {
	            value = (String) em.evaluate(property).getValue();
	        }
	    }
	    catch (Exception e)
	    {
	        logger.debug(e.getMessage());
	        value = "unable to reslove attribute";
	    }

	    return value;
	}
	
	//------------------------------------------------------------------------------
	//	Override Object behavior
	//------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
	
		if (o == null || getClass() != o.getClass())
			return false;
		
		KeyValuePair that = (KeyValuePair) o;
		
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, value);
	}

	@Override
	public String toString()
	{
		return key + "=" + value;
	}
}
