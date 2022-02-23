/*
 * Copyright 2006 Niclas Hedhman.
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
package org.ops4j.pax.logging.service.internal;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

public class AppenderBridgeImpl extends AppenderSkeleton
    implements Appender
{

    private PaxAppenderProxy m_delegate;

    public AppenderBridgeImpl( PaxAppenderProxy delegate )
    {
        m_delegate = delegate;
    }

    protected void append( LoggingEvent event )
    {
        PaxLoggingEvent paxEvent = new PaxLoggingEventImpl( event );
        m_delegate.doAppend( paxEvent );
    }

    public void close()
    {
        m_delegate.close();
    }

    public boolean requiresLayout()
    {
        return false;
    }
}
