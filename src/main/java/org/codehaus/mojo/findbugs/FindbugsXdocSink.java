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

import org.apache.maven.doxia.module.HtmlTools;
import org.apache.maven.doxia.sink.SinkAdapter;
import org.apache.maven.doxia.util.LineBreaker;

/**
 * A doxia Sink which produces an FindBugs model.
 * 
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindbugsXdocSink.java 2561 2006-10-24 21:06:39Z gleclaire $
 */
public final class FindbugsXdocSink extends SinkAdapter
{
    private static final String EOL = System.getProperty( "line.separator" );

    private LineBreaker out;

    public FindbugsXdocSink( Writer out )
    {
        this.out = new LineBreaker( out );
    }

    public void analysisErrorTag( String className )
    {
        this.markup( "<AnalysisError>" );
        this.markup( className );
        this.markup( " </AnalysisError>" + EOL );
    }

    public void body()
    {
        this.markup( "<BugCollection>" + EOL );
    }

    public void body( String version, String threshold, String effort )
    {
        this.markup( "<BugCollection" );
        this.markup( " version=" + '"' + version + '"' );
        this.markup( " threshold=" + '"' + threshold + '"' );
        this.markup( " effort=" + '"' + effort + '"' );

        this.markup( " >" + EOL );
    }

    public void body_()
    {
        this.markup( "</BugCollection>" + EOL );
        this.out.flush();
    }

    public void bugInstance( String type, String priority, String category, String message, String lineNumber )
    {
        this.markup( "<BugInstance" );
        this.markup( " type=" + '"' + HtmlTools.escapeHTML( type ) + '"' );
        this.markup( " priority=" + '"' + HtmlTools.escapeHTML( priority ) + '"' );
        this.markup( " category=" + '"' + HtmlTools.escapeHTML( category ) + '"' );
        this.markup( " message=" + '"' + HtmlTools.escapeHTML( message ) + '"' );
        this.markup( " lineNumber=" + '"' + HtmlTools.escapeHTML( lineNumber ) + '"' );
        this.markup( " />" + EOL );
    }

    public void classTag( String className )
    {
        this.markup( "<file" );
        this.markup( " classname=" + '"' + className + '"' );
        this.markup( " >" + EOL );
    }

    public void classTag_()
    {
        this.markup( "</file>" + EOL );
    }

    public void close()
    {
        this.out.close();
    }

    public void errorTag()
    {
        this.markup( "<Errors>" + EOL );
    }

    public void errorTag_()
    {
        this.markup( "</Errors>" + EOL );
    }

    public void flush()
    {
        this.out.flush();
    }

    public void head()
    {
        this.markup( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + EOL );
    }

    public void missingClassTag( String className )
    {
        this.markup( "<MissingClass>" );
        this.markup( className );
        this.markup( " </MissingClass>" + EOL );
    }

    protected void markup( String text )
    {
        this.out.write( text, true );
    }

}
