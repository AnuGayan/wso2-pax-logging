/*
 * Copyright 2005 Makas Tzavellas.
 * Copyright 2005 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.logging.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;
import org.ops4j.pax.logging.PaxLoggingService;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

/**
 * ServiceFactory implementation to return LogService implementation instances.
 */
public class LoggingServiceFactory
    implements ServiceFactory
{

    /**
     * Dictonary key
     */
    public static final String LOG4J_LOGGER_NAME = "Log4J-LoggerName";

    /**
     * Dictionary lookup key
     */
    public static final String LOG4J_CONFIG_FILE = "Log4J-ConfigFile";

    /**
     * System PaxLogger
     */
    private static final Logger m_SystemLogger;

    /**
     * The config factory
     */
    private ConfigFactory m_ConfigFactory;

    /**
     * Merged log4j properties from all the bundles.
     */
    private Properties m_MergedProperties;

    /**
     * Flag used to determine if the operator is using a single global log4j properties
     * or the merged log4j properties.
     */
    private boolean m_IsUsingGlobal;
    private Object m_PaxLogging;

    static
    {
        m_SystemLogger = Logger.getLogger( "pax.logging.system" );
    }

    /**
     * Constructor
     *
     * @param config the Configuration Factory to use
     */
    public LoggingServiceFactory( ConfigFactory config, PaxLoggingService paxLogging  )
    {
        m_PaxLogging = paxLogging;
        m_ConfigFactory = config;
        m_MergedProperties = new Properties();
        m_MergedProperties.put( "log4j.rootLogger", "DEBUG, A1" );
        m_MergedProperties.put( "log4j.appender.A1", "org.apache.log4j.ConsoleAppender" );
        m_MergedProperties.put( "log4j.appender.A1.layout", "org.apache.log4j.PatternLayout" );
        m_MergedProperties.put( "log4j.appender.A1.layout.ConversionPattern", "%-4r [%t] %-5p %c %x - %m%n" );
        m_ConfigFactory.configure( m_MergedProperties );
    }

    /**
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle,
     *      org.osgi.framework.ServiceRegistration)
     */
    public Object getService( Bundle bundle, ServiceRegistration registration )
    {
        Dictionary dictionary = bundle.getHeaders();
        String loggerName = (String) dictionary.get( LOG4J_LOGGER_NAME );
        if( loggerName != null )
        {
            String configFileName = (String) dictionary.get( LOG4J_CONFIG_FILE );
            try
            {
                mergeProperties( bundle, loggerName, configFileName );
            } catch( IOException e )
            {
                m_SystemLogger.error(
                    "Can not read Log4J configuration " + configFileName + " from bundle for '" + loggerName + "'.", e
                );
            }
        }
        else
        {
            loggerName = bundle.getLocation();
        }
        return m_PaxLogging;
    }

    /**
     * Tries to append a logger for the provided bundle, reading
     * the properties from the specified log4j config file
     *
     * @param bundle         the bundle that contains the log4j configuration in it's classpath
     * @param loggerName     the loggers name to use
     * @param configFileName the location of the log4j config file
     */
    private void mergeProperties( Bundle bundle, String loggerName, String configFileName )
        throws IOException
    {
        InputStream is = null;
        try
        {
            URL resource = bundle.getResource( configFileName );
            if (resource == null) {
                return;
            }
            is = resource.openStream();
            if( is != null )
            {
                Properties properties = new Properties();
                properties.load( is );
                String loggerNameKey = "log4j.logger." + loggerName;
                String loggerOutputValue = properties.getProperty( loggerNameKey );
                if( loggerOutputValue == null )
                {
                    // Makas - Aug 10, 2005: Cant find the value using the logger
                    // name given.
                    // Ignore it so they can check the mistake they made between
                    // the given logger name
                    // and the one specified in the properties file.
                    return;
                }
                int pos = loggerOutputValue.lastIndexOf( ',' );
                String outputFileIdentifier = loggerOutputValue.substring( pos + 1 ).trim();
                String newLoggerName = loggerName + outputFileIdentifier;

                loggerOutputValue = loggerOutputValue.replaceAll( outputFileIdentifier, newLoggerName );

                properties.put( loggerNameKey, loggerOutputValue );
                String appenderName = "log4j.appender." + outputFileIdentifier;
                String newAppenderName = "log4j.appender." + newLoggerName;

                Set set = properties.keySet();
                Iterator itr = set.iterator();
                Map newProperties = new HashMap();
                while( itr.hasNext() )
                {
                    String origKey = (String) itr.next();
                    if( origKey.startsWith( appenderName ) )
                    {
                        String newKey = origKey.replaceAll( appenderName, newAppenderName );
                        // Makas - Aug 10, 2005: Replace the old key with the new.
                        Object value = properties.get( origKey );
                        itr.remove();
                        newProperties.put( newKey, value );
                    }
                }
                properties.putAll( newProperties );
                if( m_MergedProperties != null )
                {
                    properties.putAll( m_MergedProperties );
                }
                if( m_IsUsingGlobal == false )
                {
                    m_ConfigFactory.configure( properties );
                }
                m_MergedProperties = properties;
            }
        } finally
        {
            if( is != null )
            {
                is.close();
            }
        }
    }

    /**
     * Disposes the LogService instance.
     *
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle,
     *      org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService( Bundle bundle, ServiceRegistration registration, Object service )
    {
    }

    /**
     * Overwrites all the LogService properties using the Log4J properties specified
     * in this configuration.
     *
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    public void updated( Dictionary configuration )
        throws ConfigurationException
    {
        if( configuration == null )
        {
            useGlobalProperties();
            return;
        }
        Object configFile = configuration.get( LOG4J_CONFIG_FILE );
        if( configFile == null || "".equals( configFile.toString() ) )
        {
            usePropertiesInProvidedConfiguration( configuration );
            return;
        }
        usePropertiesInURL( configFile );
    }

    private void usePropertiesInProvidedConfiguration( Dictionary configuration )
    {
        Properties extracted = new Properties();
        Enumeration list = configuration.keys();
        while( list.hasMoreElements() )
        {
            Object obj = list.nextElement();
            if( obj instanceof String )
            {
                String key = (String) obj;
                if( key.startsWith( "log4j" ) )
                {
                    Object value = configuration.get( obj );
                    extracted.put( key, value );
                }
            }
        }
        // If the updated() method is called without any Configuration URL and without any log4j properties,
        // then keep the default/previous configuration.
        if( extracted.size() == 0 )
        {
            return;
        }
        m_ConfigFactory.configure( extracted );
        m_IsUsingGlobal = false;
    }

    private void usePropertiesInURL( Object configFile )
        throws ConfigurationException
    {
        InputStream is = null;
        try
        {
            URL url = new URL( configFile.toString() );
            is = url.openStream();
            Properties properties = new Properties();
            properties.load( is );
            m_ConfigFactory.configure( properties );
            m_IsUsingGlobal = true;
        } catch( MalformedURLException e )
        {
            String message = "Cannot read log4j configuration from " + configFile;
            ConfigurationException ce = new ConfigurationException( LOG4J_CONFIG_FILE, message );
            ce.initCause( e );
            throw ce;
        } catch( IOException e )
        {
            String message = "Cannot read log4j configuration from " + configFile;
            ConfigurationException ce = new ConfigurationException( LOG4J_CONFIG_FILE, message );
            ce.initCause( e );
            throw ce;
        } finally
        {
            if( is != null )
            {
                try
                {
                    is.close();
                } catch( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void useGlobalProperties()
    {
        m_IsUsingGlobal = false;
        m_ConfigFactory.configure( m_MergedProperties );
    }
}
