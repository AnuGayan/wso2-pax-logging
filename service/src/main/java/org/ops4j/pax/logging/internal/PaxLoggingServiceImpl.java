/*
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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PaxLoggingConfigurator;
import org.knopflerfish.service.log.LogService;
import org.ops4j.pax.logging.EventAdminTracker;
import org.ops4j.pax.logging.PaxContext;
import org.ops4j.pax.logging.PaxLogger;
import org.ops4j.pax.logging.PaxLoggingService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.log.LogEntry;

public class PaxLoggingServiceImpl
    implements PaxLoggingService, LogService, ManagedService, ServiceFactory
{

    private LogReaderServiceImpl m_logReader;
    private EventAdminTracker m_eventAdmin;
    private AppenderTracker m_appenderTracker;
    private PaxContext m_context;
    
    private int m_logLevel=LOG_DEBUG;
    
    public PaxLoggingServiceImpl( LogReaderServiceImpl logReader, EventAdminTracker eventAdmin,
                                  AppenderTracker appenderTracker )
    {
        m_appenderTracker = appenderTracker;
        m_logReader = logReader;
        m_eventAdmin = eventAdmin;
        m_context = new PaxContext();
        configureDefaults();
    }

    public PaxLogger getLogger( Bundle bundle, String category, String fqcn )
    {
        Logger log4jLogger = Logger.getLogger( category );
        return new PaxLoggerImpl( bundle, log4jLogger, fqcn, this );
    }

    public int getLogLevel()
    {
        return m_logLevel;
    }

    public void log( int level, String message )
    {
        log( level, message, null );
    }

    public void log( int level, String message, Throwable exception )
    {
        log( (ServiceReference) null, level, message, exception );
    }

    public void log( ServiceReference sr, int level, String message )
    {
        log( sr, level, message, null );
    }

    public void log( ServiceReference sr, int level, String message, Throwable exception )
    {
        log( null, sr, level, message, exception );
    }

    /**
     * This method is used by the FrameworkHandler to log framework events.
     *
     * @param bundle    The bundle that caused the event.
     * @param level     The level to be logged as.
     * @param message   The message.
     * @param exception The exception, if any otherwise null.
     */
    void log( Bundle bundle, int level, String message, Throwable exception )
    {
        log( bundle, null, level, message, exception );
    }

    private void log( Bundle bundle, ServiceReference sr, int level, String message, Throwable exception )
    {
        // failsafe in case bundle is null
        if( null == bundle && null != sr )
        {
            bundle = sr.getBundle();
        }

        String category = "[undefined]";
        if( bundle != null )
        {
            category = bundle.getSymbolicName();
            if( null == category )
            {
                category = "[bundle@" + bundle.getBundleId() + ']';
            }
        }

        PaxLogger logger = getLogger( bundle, category, "" );
        switch( level )
        {
            case LOG_ERROR:
                logger.error( message, exception );
                break;
            case LOG_WARNING:
                logger.warn( message, exception );
                break;
            case LOG_INFO:
                logger.inform( message, exception );
                break;
            case LOG_DEBUG:
                logger.debug( message, exception );
                break;
            default:
                logger.warn( "Undefined Level: " + level + " : " + message, exception );
        }
        handleEvents( bundle, sr, level, message, exception);
    }

    void handleEvents( Bundle bundle, ServiceReference sr, int level, String message, Throwable exception )
    {
        LogEntry entry = new LogEntryImpl( bundle, sr, level, message, exception );
        m_logReader.fireEvent( entry );

        // This should only be null for TestCases.
        if( m_eventAdmin != null )
        {
            Event event = createEvent( bundle, level, entry, message, exception, sr ,getPaxContext().getContext()) ;
            m_eventAdmin.postEvent( event );
        }
    }

    public void updated( Dictionary configuration )
        throws ConfigurationException
    {
        if( configuration == null )
        {
            configureDefaults();
            return;
        }
        Properties extracted = new Properties();
        Enumeration list = configuration.keys();
        while( list.hasMoreElements() )
        {
            Object obj = list.nextElement();
            if( obj instanceof String )
            {
                String key = (String) obj;
                Object value = configuration.get( obj );
                if( key.startsWith( "log4j" ) )
                {
                    extracted.put( key, value );
                }
                else if( key.startsWith( "pax." ) )
                {
                    if( "pax.logging.entries.size".equals( key ) )
                    {
                        try
                        {
                            m_logReader.setMaxEntries( Integer.parseInt( (String) value ) );
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        // If the updated() method is called without any log4j properties,
        // then keep the default/previous configuration.
        if( extracted.size() == 0 )
        {
            configureDefaults();
            return;
        }
        PaxLoggingConfigurator configurator = new PaxLoggingConfigurator( m_appenderTracker );
        configurator.doConfigure( extracted, LogManager.getLoggerRepository() );
    }

    private void configureDefaults()
    {
        String levelName = System.getProperty( "org.ops4j.pax.logging.DefaultServiceLog.level", "DEBUG" ).trim();
        m_logLevel=convertLevel( levelName );

        PaxLoggingConfigurator configurator = new PaxLoggingConfigurator( m_appenderTracker );
        Properties defaultProperties = new Properties();
        defaultProperties.put( "log4j.rootLogger", convertLevel(m_logLevel)+", A1" );
        defaultProperties.put( "log4j.appender.A1", "org.apache.log4j.ConsoleAppender" );
        defaultProperties.put( "log4j.appender.A1.layout", "org.apache.log4j.TTCCLayout" );
        configurator.doConfigure( defaultProperties, LogManager.getLoggerRepository() );
    }

    static Event createEvent( Bundle bundle, int level, LogEntry entry, String message,
                              Throwable exception, ServiceReference sr,Map context )
    {
        String type;
        switch( level )
        {
            case LOG_ERROR:
                type = "LOG_ERROR";
                break;
            case LOG_WARNING:
                type = "LOG_WARNING";
                break;
            case LOG_INFO:
                type = "LOG_INFO";
                break;
            case LOG_DEBUG:
                type = "LOG_DEBUG";
                break;
            default:
                type = "LOG_OTHER";
        }
        String topic = "org/osgi/service/log/LogEntry/" + type;
        Dictionary props = new Hashtable();
        if( bundle != null )
        {
            props.put( "bundle", bundle );
            Long bundleId = new Long( bundle.getBundleId() );
            props.put( "bundle.id", bundleId );
            String symbolicName = bundle.getSymbolicName();
            if( symbolicName != null )
            {
                props.put( "bundle.symbolicname", symbolicName );
            }
        }
        props.put( "log.level", new Integer( level ) );
        props.put( "log.entry", entry );
        props.put( "message", message );
        props.put( "timestamp", new Long( System.currentTimeMillis() ) );
        if( exception != null )
        {
            props.put( "exception", exception );
            props.put( "exception.class", exception.getClass() );
            // Only save message if message is not null otherwise NPE is thrown
            if( exception.getMessage() != null )
            {
                props.put( "exception.message", exception.getMessage() );
            }
        }
        if( sr != null )
        {
            props.put( "service", sr );
            Long id = (Long) sr.getProperty( Constants.SERVICE_ID );
            props.put( "service.id", id );
            String pid = (String) sr.getProperty( Constants.SERVICE_PID );
            if( pid != null )
            {
                props.put( "service.pid", pid );
            }
            String[] objClass = (String[]) sr.getProperty( Constants.OBJECTCLASS );
            props.put( "service.objectClass", objClass );
        }
        if(context != null )
        {
            for(Iterator keys=context.keySet().iterator();keys.hasNext();){
                String key=(String)keys.next();
                props.put(key, context.get(key));
            }
        }
        return new Event( topic, props );
    }

    /*
     * use local class to delegate calls to underlying instance while keeping bundle reference
     */
    public Object getService( final Bundle bundle, ServiceRegistration registration )
    {
        class ManagedPaxLoggingService
            implements PaxLoggingService, LogService, ManagedService
        {

            public void log( int level, String message )
            {
                PaxLoggingServiceImpl.this.log( bundle, null, level, message, null );
            }

            public void log( int level, String message, Throwable exception )
            {
                PaxLoggingServiceImpl.this.log( bundle, null, level, message, exception );
            }

            public void log( ServiceReference sr, int level, String message )
            {
                PaxLoggingServiceImpl.this.log( bundle, sr, level, message, null );
            }

            public void log( ServiceReference sr, int level, String message, Throwable exception )
            {
                PaxLoggingServiceImpl.this.log( bundle, sr, level, message, exception );
            }

            public int getLogLevel()
            {
                return PaxLoggingServiceImpl.this.getLogLevel();
            }

            public PaxLogger getLogger( Bundle myBundle, String category, String fqcn )
            {
                return PaxLoggingServiceImpl.this.getLogger( myBundle, category, fqcn );
            }

            public void updated( Dictionary configuration )
                throws ConfigurationException
            {
                PaxLoggingServiceImpl.this.updated( configuration );
            }

            public PaxContext getPaxContext() {
                return PaxLoggingServiceImpl.this.getPaxContext();
            }
        }

        return new ManagedPaxLoggingService();
    }

    public void ungetService( Bundle bundle, ServiceRegistration registration, Object service )
    {
        // nothing to do...
    }

    public PaxContext getPaxContext() {
        return m_context;
    }
    
    private static int convertLevel( String levelName )
    {
        if( "DEBUG".equals( levelName ) )
        {
            return LOG_DEBUG;
        }
        else if( "INFO".equals( levelName ) )
        {
            return LOG_INFO;
        }
        else if( "ERROR".equals( levelName ) )
        {
            return LOG_ERROR;
        }
        else
        {
            return LOG_DEBUG;
        }
    }
    
    private static String convertLevel(int level)
    {
        switch(level){
            case LOG_DEBUG:return "DEBUG";
            case LOG_INFO:return "INFO";
            case LOG_ERROR:return "ERROR";
            default: return "DEBUG";
        }
    }
}
