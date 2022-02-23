/*
 * Copyright 2008 Edward Yakop.
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.logging.it.appender;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.ops4j.pax.drone.api.DroneConnector;
import org.ops4j.pax.drone.connector.paxrunner.GenericConnector;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.spi.junit.DroneTestCase;
import org.ops4j.pax.logging.PaxLoggingService;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * Integration tests for custom appender.
 *
 * @author edward.yakop@gmail.com
 * @author Alin Dreghiciu
 */
public class CustomAppenderTest 
    extends DroneTestCase
{

    private static final String FILTER_PAX_LOGGING = "(" + Constants.SERVICE_PID + "=org.ops4j.pax.logging)";

    private static final String SERVICE_NAME = PaxAppender.class.getName();
    private static final String EXPECTED_MESSAGE = "Hello";

    public DroneConnector configure()
    {
        return GenericConnector.create( GenericConnector.createBundleProvision()
            .addBundle( "mvn:org.ops4j.pax.logging/pax-logging-service" )
            .addBundle( "mvn:org.ops4j.pax.logging/pax-logging-api" )
        ).setPlatform( Platforms.FELIX );
    }

    public final void testCustomAppender()
        throws Throwable
    {
        configurePaxLogging( bundleContext );
        CustomAppender customAppender = createAndRegisterCustomAppenderService( bundleContext );

        testLog4j( customAppender );
        // TODO: Test all supported loggings framework
    }

    private void testLog4j( CustomAppender customAppender )
    {
        // Ensure there's no events
        List events = customAppender.getEvents();
        events.clear();

        Class clazz = getClass();
        Logger log4j = Logger.getLogger( clazz );
        log4j.debug( EXPECTED_MESSAGE );

        assertEquals( 1, events.size() );

        PaxLoggingEvent event = (PaxLoggingEvent) events.get( 0 );
        assertEquals( Level.DEBUG.getSyslogEquivalent(), event.getLevel().getSyslogEquivalent() );
        assertEquals( EXPECTED_MESSAGE, event.getMessage() );

        events.clear();
    }

    private void configurePaxLogging( BundleContext bundleContext )
        throws Throwable
    {
        ServiceReference[] serviceReferences = bundleContext.getServiceReferences(
            ManagedService.class.getName(), FILTER_PAX_LOGGING
        );
        assertNotNull( serviceReferences );
        assertEquals( 1, serviceReferences.length );
        ServiceReference reference = serviceReferences[ 0 ];

        // *** Configure pax-logging

        // Load log4j properties
        Properties properties = new Properties();
        URL resource = CustomAppenderTest.class.getResource( "CustomAppenderTest.log.properties" );
        InputStream propertiesStream = resource.openStream();
        properties.load( propertiesStream );

        // Update configuration
        ManagedService service = (ManagedService) bundleContext.getService( reference );
        service.updated( properties );

        bundleContext.ungetService( reference );
    }

    private CustomAppender createAndRegisterCustomAppenderService( BundleContext bundleContext )
    {
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty( PaxLoggingService.APPENDER_NAME_PROPERTY, "custom" );
        CustomAppender customAppender = new CustomAppender();
        bundleContext.registerService( SERVICE_NAME, customAppender, serviceProperties );

        return customAppender;
    }
}