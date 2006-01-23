/*Copyright (c) 2004, The Codehaus
 *
 *Permission is hereby granted, free of charge, to any person obtaining a copy of
 *this software and associated documentation files (the "Software"), to deal in
 *the Software without restriction, including without limitation the rights to
 *use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 *of the Software, and to permit persons to whom the Software is furnished to do
 *so, subject to the following conditions:
 *
 *The above copyright notice and this permission notice shall be included in all
 *copies or substantial portions of the Software.
 *
 *THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *SOFTWARE.
 */

package org.codehaus.mojo.findbugs;

import edu.umd.cs.findbugs.Detector;

/**
 * Constant values for the configuration parameter <code>threshold</code>.
 * 
 * @author $Author: cyrill $
 * @version $Revision: 28 $ $Date$
 * 
 * @author <a href="mailto:ruettimac@mac.com">Cyrill Ruettimann</a>
 * 
 * $Revision: 28 $
 * $Date$
 * $Author: cyrill $
 */
public final class ThresholdParameter
{

    /**
     * High priority threshold.
     * 
     */
    static final ThresholdParameter HIGH = new ThresholdParameter( "High", Detector.HIGH_PRIORITY );

    /**
     * Normal priority threshold.
     * 
     */
    static final ThresholdParameter NORMAL = new ThresholdParameter( "Normal", Detector.NORMAL_PRIORITY );

    /**
     * Normal priority threshold.
     * 
     */
    static final ThresholdParameter LOW = new ThresholdParameter( "Low", Detector.LOW_PRIORITY );

    /**
     * Normal priority threshold.
     * 
     */
    static final ThresholdParameter EXP = new ThresholdParameter( "Exp", Detector.EXP_PRIORITY );

    /**
     * Normal priority threshold.
     * 
     */
    static final ThresholdParameter IGNORE = new ThresholdParameter( "Ignore", Detector.IGNORE_PRIORITY );

    /**
     * Default priority threshold.
     * 
     */
    static final ThresholdParameter DEFAULT = new ThresholdParameter( "Low", Detector.LOW_PRIORITY );

    /**
     * The threshold value.
     * 
     */
    private transient int mValue;

    /**
     * The threshold name.
     * 
     */
    private transient String mName;

    /**
     * Hide default constructor.
     * 
     */
    private ThresholdParameter()
    {
        super();
    }

    /**
     * Default constructor.
     * 
     * @param pName
     *            The threshold name to set.
     * @param pValue
     *            The threshold value to set.
     */
    private ThresholdParameter( final String pName, final int pValue )
    {
        super();

        if ( pName == null || pName.trim().length() == 0 )
        {
            throw new IllegalArgumentException( "Argument pName not allowed to be null" );
        }

        mValue = pValue;
        mName = pName;
    }

    /**
     * @return The threshold value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * @return The threshold name.
     */
    public String getName()
    {
        return mName;
    }
}
