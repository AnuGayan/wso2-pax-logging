/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * Copyright 2005 Niclas Hedhman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.logging;

import org.osgi.framework.Bundle;

public class DefaultServiceLog
    implements PaxLogger
{

    private Bundle m_bundle;
    private String m_categoryName;

    public DefaultServiceLog( Bundle bundle, String categoryName )
    {
        m_bundle = bundle;
        m_categoryName = categoryName;
    }

    public boolean isTraceEnabled()
    {
        return false;
    }

    public boolean isDebugEnabled()
    {
        return true;
    }

    public boolean isWarnEnabled()
    {
        return true;
    }

    public boolean isInfoEnabled()
    {
        return true;
    }

    public boolean isErrorEnabled()
    {
        return true;
    }

    public boolean isFatalEnabled()
    {
        return true;
    }

    public void trace( String message, Throwable t )
    {
    }

    public void debug( String message, Throwable t )
    {
        output( message );
        if( t != null )
        {
            t.printStackTrace( System.out );
        }
    }

    public void inform( String message, Throwable t )
    {
        output( message );
        if( t != null )
        {
            t.printStackTrace( System.out );
        }
    }

    public void warn( String message, Throwable t )
    {
        output( message );
        if( t != null )
        {
            t.printStackTrace( System.out );
        }
    }

    public void error( String message, Throwable t )
    {
        output( message );
        if( t != null )
        {
            t.printStackTrace( System.out );
        }
    }

    public void fatal( String message, Throwable t )
    {
        output( message );
        if( t != null )
        {
            t.printStackTrace( System.out );
        }
    }

    public int getLogLevel()
    {
        // Always DEBUG mode.
        return 10000;
    }

    public String getName()
    {
        return m_categoryName;
    }

    private void output( String message )
    {
        // Might be [null] if used by standard test cases.
        if( m_bundle != null )
        {
            System.out.print( m_bundle.getSymbolicName() );
        }
        System.out.print( "[" );
        System.out.print( m_categoryName );
        System.out.print( "] : " );
        System.out.println( message );
    }
}