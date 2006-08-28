package org.codehaus.mojo.findbugs;

/* Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;

/**
 * Constant values for the configuration parameter <code>effort</code>.
 * 
 * @author $Author: cyrill $
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * @version $Id$
 */
public final class EffortParameter
{

    /**
     * High effort.
     * 
     */
    static final EffortParameter MAX = new EffortParameter( "Max", FindBugs.MAX_EFFORT );

    /**
     * Normal effort.
     * 
     */
    static final EffortParameter DEFAULT = new EffortParameter( "Default", FindBugs.DEFAULT_EFFORT );

    /**
     * Low effort.
     * 
     */
    static final EffortParameter MIN = new EffortParameter( "Min", FindBugs.MIN_EFFORT );

    /**
     * The effort value.
     * 
     */
    private final transient AnalysisFeatureSetting[] mValue;

    /**
     * The effort name.
     * 
     */
    private final transient String mName;

    /**
     * Hide default constructor.
     * 
     */
    private EffortParameter()
    {
        super();

        this.mValue = null;
        this.mName = null;
    }

    /**
     * Default constructor.
     * 
     * @param pName
     *            The effort name to set.
     * @param pValue
     *            The effort value to set.
     */
    private EffortParameter( final String pName, final AnalysisFeatureSetting[] pValue )
    {
        super();

        if ( ( pName == null ) || ( pName.trim().length() == 0 ) )
        {
            throw new IllegalArgumentException( "Argument pName not allowed to be null" );
        }

        this.mValue = pValue;
        this.mName = pName;
    }

    /**
     * @return The effort value.
     */
    public AnalysisFeatureSetting[] getValue()
    {
        return this.mValue;
    }

    /**
     * @return The effort name.
     */
    public String getName()
    {
        return this.mName;
    }
}
