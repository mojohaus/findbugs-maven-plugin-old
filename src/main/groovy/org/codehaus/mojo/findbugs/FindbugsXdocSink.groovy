package org.codehaus.mojo.findbugs;

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

import java.io.Writer

import org.apache.maven.doxia.module.HtmlTools
import org.apache.maven.doxia.sink.SinkAdapter
import org.apache.maven.doxia.util.LineBreaker

/**
 * A doxia Sink which produces an FindBugs model.
 * 
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindbugsXdocSink.groovy 2561 2006-10-24 21:06:39Z gleclaire $
 */
class FindbugsXdocSink extends SinkAdapter
{

    String xdocEncoding
    static final String EOL = System.getProperty( "line.separator" )

    LineBreaker out

    FindbugsXdocSink( Writer out )
    {
        this.out = new LineBreaker( out )
        this.xdocEncoding = out.getEncoding()
    }

    void analysisErrorTag( String className )
    {
        this.markup( "<AnalysisError>" )
        this.markup( HtmlTools.escapeHTML( className ) )
        this.markup( " </AnalysisError>" + EOL )
    }

    void body()
    {
        this.markup( "<BugCollection>" + EOL )
    }

    void body( String version, String threshold, String effort )
    {
        this.markup( "<BugCollection" );
        this.markup( " version=" + '"' + version + '"' )
        this.markup( " threshold=" + '"' + threshold + '"' )
        this.markup( " effort=" + '"' + effort + '"' )

        this.markup( " >" + EOL );
    }

    void body_()
    {
        this.markup( "</BugCollection>" + EOL )
        this.out.flush();
    }

    void bugInstance( String type, String priority, String category, String message, String lineNumber )
    {
        this.markup( "<BugInstance" );
        this.markup( " type=" + '"' + HtmlTools.escapeHTML( type ) + '"' )
        this.markup( " priority=" + '"' + HtmlTools.escapeHTML( priority ) + '"' )
        this.markup( " category=" + '"' + HtmlTools.escapeHTML( category ) + '"' )
        this.markup( " message=" + '"' + HtmlTools.escapeHTML( message ) + '"' )
        this.markup( " lineNumber=" + '"' + HtmlTools.escapeHTML( lineNumber ) + '"' )
        this.markup( " />" + EOL )
    }

    void classTag( String className )
    {
        this.markup( "<file" );
        this.markup( " classname=" + '"' + HtmlTools.escapeHTML( className ) + '"' )
        this.markup( " >" + EOL )
    }

    void classTag_()
    {
        this.markup( "</file>" + EOL )
    }

    void close()
    {
        this.out.close()
    }

    void errorTag()
    {
        this.markup( "<Errors>" + EOL )
    }

    void errorTag_()
    {
        this.markup( "</Errors>" + EOL )
    }

    void flush()
    {
        this.out.flush()
    }

    void head()
    {
//        this.markup( "<?xml version=\"1.0\" encoding=\"" + xdocEncoding + "\"?>" + EOL )
        this.markup( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + EOL )
    }

    void missingClassTag( String className )
    {
        this.markup( "<MissingClass>" )
        this.markup( HtmlTools.escapeHTML( className ) )
        this.markup( " </MissingClass>" + EOL )
    }

    protected void markup( String text )
    {
        this.out.write( text, true )
    }

    void ProjectTag()
    {
        this.markup( "<Project>" + EOL )
    }

    void ProjectTag_()
    {
        this.markup( "</Project>" + EOL )
    }

    void srcDirTag( String className )
    {
        this.markup( "<SrcDir>" )
        this.markup( HtmlTools.escapeHTML( className ) )
        this.markup( "</SrcDir>" + EOL )
    }
}
