package org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced;

import java.util.Objects;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class MuleComponent
{
	@Parameter
	@Summary("Provide the namespace for this component (e.g., http, mule, db, ...")
	@Example("http")
	private String namespace;
	

	@Parameter
	@Summary("Provide the name for this component (e.g., listener, requester, select, ...")
	@Example("requester")
	private String name;
	
	public String getNamespace()
	{
		return this.namespace;
	}
	
	public void setNamespace(String namespace)
	{
		this.namespace = namespace;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getComponentFQDN()
	{
		return getNamespace() + ":" + getName();
	}
	
	//------------------------------------------------------------------------------
	//	Override Object behavior
	//------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
	
		/*
		if (o == null || getClass() != o.getClass())
			return false;
		
		*/
		if (o == null || !(o instanceof MuleComponent))
			return false;
		
		MuleComponent that = (MuleComponent) o;
		
		return Objects.equals(namespace, that.namespace) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(namespace, name);
	}

	@Override
	public String toString()
	{
		return  getComponentFQDN();
	}

}
