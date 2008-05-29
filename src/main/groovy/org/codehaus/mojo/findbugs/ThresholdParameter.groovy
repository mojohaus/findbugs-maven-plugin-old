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

import edu.umd.cs.findbugs.Detector

/**
 * Constant values for the configuration parameter <code>threshold</code>.
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id$
 */
class ThresholdParameter
{

    /**
     * High priority threshold.
     *
     */
    static final ThresholdParameter HIGH = new ThresholdParameter("High", Detector.HIGH_PRIORITY)

    /**
     * Normal priority threshold.
     *
     */
    static final ThresholdParameter NORMAL = new ThresholdParameter("Normal", Detector.NORMAL_PRIORITY)

    /**
     * Low priority threshold.
     *
     */
    static final ThresholdParameter LOW = new ThresholdParameter("Low", Detector.LOW_PRIORITY)

    /**
     * Experimental priority threshold.
     *
     */
    static final ThresholdParameter EXP = new ThresholdParameter("Exp", Detector.EXP_PRIORITY)

    /**
     * ??? priority threshold.
     *
     */
    static final ThresholdParameter IGNORE = new ThresholdParameter("Ignore", Detector.IGNORE_PRIORITY)

    /**
     * Default priority threshold.
     *
     */
    static final ThresholdParameter DEFAULT = ThresholdParameter.LOW

    /**
     * The threshold value.
     *
     */
    int value

    /**
     * The threshold name.
     *
     */
    String name

    /**
     * Hide default constructor.
     *
     */
    private ThresholdParameter()
    {
        super()

        this.value = -1
        this.name = null
    }

    /**
     * Default constructor.
     *
     * @param name
     *            The threshold name to set.
     * @param value
     *            The threshold value to set.
     */
    private ThresholdParameter(final String name, final int value)
    {
        super()
//        if ( !name || (name.trim().length() == 0) )
        if ( !name )
        {
            throw new IllegalArgumentException("Argument name not allowed to be null or empty")
        }
        this.value = value
        this.name = name
    }
}
