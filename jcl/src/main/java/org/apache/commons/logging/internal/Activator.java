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
package org.apache.commons.logging.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class Activator
    implements BundleActivator
{

    public void start( BundleContext iContext )
        throws Exception
    {
        LogFactory.setBundleContext( iContext );
        String name = getClass().getName();
        Log logger = LogFactory.getLog( name );
        logger.info( "Enabling Jakarta Commons Logging API support." );
    }

    public void stop( BundleContext iContext )
        throws Exception
    {
        String name = getClass().getName();
        Log logger = LogFactory.getLog( name );
        logger.info( "Disabling Jakarta Commons Logging API support." );
        LogFactory.release();
    }
}
