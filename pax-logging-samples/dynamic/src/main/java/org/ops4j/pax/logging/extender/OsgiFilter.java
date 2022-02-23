/*  Copyright 2012 Guillaume Nodet.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.logging.extender;

import org.ops4j.pax.logging.spi.PaxFilter;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

public class OsgiFilter implements PaxFilter {

    public int doFilter(PaxLoggingEvent event) {
        return (event.getMessage().contains("foo")) ? ACCEPT : DENY;
    }
}
