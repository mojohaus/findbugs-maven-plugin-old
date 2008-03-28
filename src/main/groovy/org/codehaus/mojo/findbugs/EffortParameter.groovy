package org.codehaus.mojo.findbugs

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import edu.umd.cs.findbugs.FindBugs
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting

/**
 * Constant values for the configuration parameter <code>effort</code>.
 * 
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */
class EffortParameter
{

    /**
     * High effort.
     * 
     */
    static final EffortParameter MAX = new EffortParameter( "Max", FindBugs.MAX_EFFORT )

    /**
     * Normal effort.
     * 
     */
    static final EffortParameter DEFAULT = new EffortParameter( "Default", FindBugs.DEFAULT_EFFORT )

    /**
     * Low effort.
     * 
     */
    static final EffortParameter MIN = new EffortParameter( "Min", FindBugs.MIN_EFFORT )

    /**
     * The effort value.
     * 
     */
    AnalysisFeatureSetting[] value

    /**
     * The effort name.
     * 
     */
    String name

    /**
     * Hide default constructor.
     * 
     */
    private EffortParameter()
    {
        super()

        this.value = null
        this.name = null
    }

    /**
     * Default constructor.
     * 
     * @param name
     *            The effort name to set.
     * @param value
     *            The effort value to set.
     */
    private EffortParameter( final String name, final AnalysisFeatureSetting[] value )
    {
        super()

        if ( ( name == null ) || ( name.trim().length() == 0 ) )
        {
            throw new IllegalArgumentException( "Argument name not allowed to be null" )
        }

        this.value = value
        this.name = name
    }
    
    /**
     * @return The effort value.
     */
    AnalysisFeatureSetting[] getValue()
    {
        return this.value
    }

    /**
     * @return The effort name.
     */
    String getName()
    {
        return this.name
    }
}
