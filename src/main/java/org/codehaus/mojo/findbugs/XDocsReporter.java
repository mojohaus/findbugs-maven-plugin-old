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

import java.io.Writer;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.apache.maven.plugin.logging.Log;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugReporterObserver;
import edu.umd.cs.findbugs.DelegatingBugReporter;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * The reporter controls the generation of the FindBugs report. It contains call back methods which gets called by
 * FindBugs if a bug is found.
 * 
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: XDocsReporter.java 2561 2006-10-24 21:06:39Z gleclaire $
 */
public final class XDocsReporter extends DelegatingBugReporter
{

    /**
     * The key to get the value if the line number is not available.
     * 
     */
    private static final String NOLINE_KEY = "report.findbugs.noline";

    /**
     * The sink to write the report to.
     * 
     */
    private  FindbugsXdocSink sink;

    /**
     * The bundle to get the messages from.
     * 
     */
    private  ResourceBundle resourceBundle;

    /**
     * The logger to write logs to.
     * 
     */
    private  Log log;

    /**
     * The threshold of bugs severity.
     * 
     */
    private  ThresholdParameter threshold;

    /**
     * The used effort for searching bugs.
     * 
     */
    private  EffortParameter effort;

    /**
     * The name of the current class which is analysed by FindBugs.
     * 
     */
    private  String currentClassName;

    /**
     * Signals if the report for the current class is opened.
     * 
     */
    private  boolean isCurrentClassReportOpened = false;

    /**
     * The Collection of Bugs and Error collected during analysis.
     * 
     */
    private SortedBugCollection bugCollection = new SortedBugCollection();

    /**
     * The output Writer stream.
     * 
     */
    private Writer outputWriter;

    /**
     * Default constructor.
     * @param realBugReporter the BugReporter to Delegate
     */
    public XDocsReporter( BugReporter realBugReporter )
    {
        super( realBugReporter );

        this.sink = null;
        this.resourceBundle = null;
        this.log = null;
        this.threshold = null;
        this.effort = null;

        // Add an observer to record when bugs make it through
        // all priority and filter criteria, so our bug count is
        // accurate.
        realBugReporter.addObserver( new BugReporterObserver()
        {
            public void reportBug( BugInstance bugInstance )
            {
                XDocsReporter.this.addBugReport( bugInstance );
            }
        } );

    }

    /**
     * @see edu.umd.cs.findbugs.BugReporter#finish()
     */
    public void finish()
    {

        // close the report, write it

        this.printErrors();
        this.getSink().body_();
        this.getSink().flush();
        this.getSink().close();

        super.finish();

    }

    /**
     * @return the effort
     */
    public EffortParameter getEffort()
    {
        return this.effort;
    }

    /**
     * @return the log
     */
    public Log getLog()
    {
        return this.log;
    }

    /**
     * @return the outputWriter
     */
    public Writer getOutputWriter()
    {
        return this.outputWriter;
    }

    /**
     * @return the resourceBundle
     */
    public ResourceBundle getResourceBundle()
    {
        return this.resourceBundle;
    }

    /**
     * @return the sink
     */
    public FindbugsXdocSink getSink()
    {
        if ( this.sink == null )
        {
            this.sink = new FindbugsXdocSink( this.getOutputWriter() );
            this.initialiseReport();

        }
        return this.sink;
    }

    /**
     * @return the threshold
     */
    public ThresholdParameter getThreshold()
    {
        return this.threshold;
    }

    public void logError( String message )
    {
        this.bugCollection.addError( message );
        super.logError( message );
    }

    public void logError( String message, Throwable e )
    {
        this.bugCollection.addError( message );
        super.logError( message, e );
    }

    /**
     * Observe a class.
     * 
     * @param classDescriptor The Class to Observe
     * @see edu.umd.cs.findbugs.classfile.IClassObserver #observeClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    public void observeClass( ClassDescriptor classDescriptor )
    {

        this.currentClassName = classDescriptor.toDottedClassName();

        if ( this.isCurrentClassReportOpened )
        {
            this.closeClassReportSection();
        }

        this.isCurrentClassReportOpened = false;

        super.observeClass( classDescriptor );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
     */
    public void reportMissingClass( ClassDescriptor classDescriptor )
    {
        this.bugCollection.addMissingClass( classDescriptor.toDottedClassName() );
        super.reportMissingClass( classDescriptor );
    }

    public void reportMissingClass( ClassNotFoundException ex )
    {
        String missingClassName = AbstractBugReporter.getMissingClassName( ex );
        this.bugCollection.addMissingClass( missingClassName );
        super.reportMissingClass( ex );
    }

    /**
     * @param effort
     *            the effort to set
     */
    public void setEffort( EffortParameter effort )
    {
        this.effort = effort;
    }

    /**
     * @param log
     *            the log to set
     */
    public void setLog( Log log )
    {
        this.log = log;
    }

    /**
     * @param outputWriter
     *            the outputWriter to set
     */
    public void setOutputWriter( Writer outputWriter )
    {
        this.outputWriter = outputWriter;
    }

    /**
     * @param resourceBundle
     *            the resourceBundle to set
     */
    public void setResourceBundle( ResourceBundle resourceBundle )
    {
        this.resourceBundle = resourceBundle;
    }

    /**
     * @param threshold
     *            the threshold to set
     */
    public void setThreshold( ThresholdParameter threshold )
    {
        this.threshold = threshold;
    }

    /**
     * Initialises the report.
     */
    private void initialiseReport()
    {
        this.getSink().head();
        this.getSink().head_();

        this.getSink().body( this.getFindBugsVersion(), this.threshold.getName(), this.effort.getName() );
    }

    protected void addBugReport( final BugInstance bugInstance )
    {

        final SourceLineAnnotation line = bugInstance.getPrimarySourceLineAnnotation();
        final BugPattern pattern = bugInstance.getBugPattern();
        final String lineNumber = this.valueForLine( line );
        final String category = pattern.getCategory();
        final String type = pattern.getType();
        final String priority = this.evaluateThresholdParameter( bugInstance.getPriority() );
        final String message = bugInstance.getMessage();

        if ( !this.isCurrentClassReportOpened )
        {
            this.getSink().classTag( this.currentClassName );
            this.isCurrentClassReportOpened = true;
        }

        this.getSink().bugInstance( type, priority, category, message, lineNumber );
    }

    /**
     * Closes the class report section.
     */
    protected void closeClassReportSection()
    {
        this.getSink().classTag_();
    }

    /**
     * Returns the threshold string value for the integer input.
     *
     * @param thresholdValue The ThresholdValue integer to evaluate.
     * @return The string valueof the Threshold object.
     * 
     */
    protected String evaluateThresholdParameter( int thresholdValue )
    {
        String thresholdName;

        switch ( thresholdValue )
        {
            case 1:
                thresholdName = ThresholdParameter.HIGH.getName();
                break;
            case 2:
                thresholdName = ThresholdParameter.NORMAL.getName();
                break;
            case 3:
                thresholdName = ThresholdParameter.LOW.getName();
                break;
            case 4:
                thresholdName = ThresholdParameter.EXP.getName();
                break;
            case 5:
                thresholdName = ThresholdParameter.IGNORE.getName();
                break;
            default:
                thresholdName = "Invalid Priority";
        }

        return thresholdName;

    }

    /**
     * Gets the Findbugs Version of the report.
     * 
     * @return The Findbugs Version used on the report.
     * 
     */
    protected String getFindBugsVersion()
    {
        return edu.umd.cs.findbugs.Version.RELEASE;
    }

    /**
     * Closes the class report section.
     */
    protected void printErrors()
    {

        this.log.info( "There are Errors" );

        this.getSink().errorTag();

        this.log.info( "Printing Errors" );

        for ( Iterator i = this.bugCollection.errorIterator(); i.hasNext(); )
        {
            AnalysisError analysisError = ( AnalysisError ) i.next();
            this.getSink().analysisErrorTag( analysisError.getMessage() );
        }

        this.log.info( "Printing Missing classes" );
        for ( Iterator i = this.bugCollection.missingClassIterator(); i.hasNext(); )
        {
            String missingClass = ( String ) i.next();
            this.getSink().missingClassTag( missingClass );
        }

        this.getSink().errorTag_();

    }

    /**
     * Return the value to display. If FindBugs does not provide a line number, a default message is returned. The line
     * number otherwise.
     * 
     * @param pLine
     *            The line to get the value from.
     * @return The line number the bug appears or a statement that there is no source line available.
     * 
     */
    protected String valueForLine( final SourceLineAnnotation pLine )
    {
        String value = null;

        if ( pLine == null )
        {
            value = this.resourceBundle.getString( XDocsReporter.NOLINE_KEY );
        }
        else
        {
            final int startLine = pLine.getStartLine();
            final int endLine = pLine.getEndLine();

            if ( startLine == endLine )
            {
                if ( startLine == -1 )
                {
                    value = this.resourceBundle.getString( XDocsReporter.NOLINE_KEY );
                }
                else
                {
                    value = String.valueOf( startLine );
                }
            }
            else
            {
                value = String.valueOf( startLine ) + "-" + String.valueOf( endLine );
            }
        }

        return value;
    }

}
