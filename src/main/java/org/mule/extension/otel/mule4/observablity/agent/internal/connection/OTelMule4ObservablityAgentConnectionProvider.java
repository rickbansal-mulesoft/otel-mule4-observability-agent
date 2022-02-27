package org.mule.extension.otel.mule4.observablity.agent.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.connectivity.NoConnectivityTest;
import org.mule.runtime.api.connection.CachedConnectionProvider;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class (as it's name implies) provides connection instances and the
 * functionality to disconnect and validate those connections.
 * <p>
 * All connection related parameters (values required in order to create a
 * connection) must be declared in the connection providers.
 * <p>
 * This particular example is a {@link CachedConnectionProvider} which declares
 * that connections resolved by this provider be lazily created and cached connections.
 */

public class OTelMule4ObservablityAgentConnectionProvider implements CachedConnectionProvider<OtelSdkConnection>, NoConnectivityTest
{	
	private final Logger LOGGER = LoggerFactory.getLogger(OTelMule4ObservablityAgentConnectionProvider.class);
	
	@Override
	public OtelSdkConnection connect() throws ConnectionException
	{
		Supplier<ConnectionException> connectionExecption = OTelMule4ObservablityAgentConnectionProvider::connectionException;
		
		return OtelSdkConnection.get().orElseThrow(connectionExecption);
		
	}

	@Override
	public void disconnect(OtelSdkConnection connection)
	{
		try
		{
			connection.invalidate();
		} 
		catch (Exception e)
		{
			LOGGER.error("Error while disconnecting from the Otel SDK: " + e.getMessage(), e);
		}
	}

	@Override
	public ConnectionValidationResult validate(OtelSdkConnection connection)
	{
		return ConnectionValidationResult.success();
	}
	
	public static ConnectionException connectionException()
	{
		return new ConnectionException("Cannot get an OpenTelemetry SDK connection before the SDK is initialized"); 
	}
}
