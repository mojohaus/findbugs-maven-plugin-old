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

import groovy.util.slurpersupport.GPathResult
import org.apache.maven.doxia.sink.Sink
import org.apache.maven.doxia.tools.SiteTool
import org.apache.maven.plugin.logging.Log
import org.codehaus.plexus.util.PathTool

/**
 * The reporter controls the generation of the FindBugs report. It contains call back methods which gets called by
 * FindBugs if a bug is found.
 *
 * @author <a href="mailto:gleclaire@codehaus.org">Garvin LeClaire</a>
 * @version $Id: FindbugsReportGenerator.groovy Z gleclaire $
 */
class FindbugsReportGenerator implements FindBugsInfo {

  /**
   * The key to get the value if the line number is not available.
   *
   */
  static final String NOLINE_KEY = "report.findbugs.noline"

  /**
   * The key to get the column title for the line.
   *
   */
  static final String COLUMN_LINE_KEY = "report.findbugs.column.line"

  /**
   * The key to get the column title for the bug.
   *
   */
  static final String COLUMN_BUG_KEY = "report.findbugs.column.bug"

  /**
   * The key to get the column title for the bugs.
   *
   */
  static final String COLUMN_BUGS_KEY = "report.findbugs.column.bugs"

  /**
   * The key to get the column title for the category.
   *
   */
  static final String COLUMN_CATEGORY_KEY = "report.findbugs.column.category"

  /**
   * The key to get the column title for the priority.
   *
   */
  static final String COLUMN_PRIORITY_KEY = "report.findbugs.column.priority"

  /**
   * The key to get the column title for the details.
   *
   */
  static final String COLUMN_DETAILS_KEY = "report.findbugs.column.details"

  /**
   * The key to get the report title of the Plug-In from the bundle.
   *
   */
  static final String REPORT_TITLE_KEY = "report.findbugs.reporttitle"

  /**
   * The key to get the report link title of the Plug-In from the bundle.
   *
   */
  static final String LINKTITLE_KEY = "report.findbugs.linktitle"

  /**
   * The key to get the report link of the Plug-In from the bundle.
   *
   */
  static final String LINK_KEY = "report.findbugs.link"

  /**
   * The key to get the name of the Plug-In from the bundle.
   *
   */
  static final String NAME_KEY = "report.findbugs.name"

  /**
   * The key to get the files title of the Plug-In from the bundle.
   *
   */
  static final String FILES_KEY = "report.findbugs.files"

  /**
   * The key to get the threshold of the report from the bundle.
   *
   */
  static final String THRESHOLD_KEY = "report.findbugs.threshold"

  /**
   * The character to separate URL tokens.
   *
   */
  static final String URL_SEPARATOR = "/"

  /**
   * The key to get the effort of the report from the bundle.
   *
   */
  static final String EFFORT_KEY = "report.findbugs.effort"

  /**
   * The key to get the link to FindBugs description page from the bundle.
   *
   */
  static final String DETAILSLINK_KEY = "report.findbugs.detailslink"

  /**
   * The key to get the version title for FindBugs from the bundle.
   *
   */
  static final String VERSIONTITLE_KEY = "report.findbugs.versiontitle"

  /**
   * The key to get the files title of the Plug-In from the bundle.
   *
   */
  static final String SUMMARY_KEY = "report.findbugs.summary"

  /**
   * The key to column title for the Class.
   *
   */
  static final String COLUMN_CLASS_KEY = "report.findbugs.column.class"

  /**
   * The key to column title for the Classes.
   *
   */
  static final String COLUMN_CLASSES_KEY = "report.findbugs.column.classes"

  /**
   * The key to column title for the errors.
   *
   */
  static final String COLUMN_ERRORS_KEY = "report.findbugs.column.errors"

  /**
   * The key to column title for the files.
   *
   */
  static final String COLUMN_FILES_KEY = "report.findbugs.column.files"

  /**
   * The key to column title for the files.
   *
   */
  static final String COLUMN_MISSINGCLASSES_KEY = "report.findbugs.column.missingclasses"

  /**
   * The sink to write the report to.
   *
   */
  Sink sink

  /**
   * The bundle to get the messages from.
   *
   */
  ResourceBundle bundle

  /**
   * The logger to write logs to.
   *
   */
  Log log


  /**
   * The threshold of bugs severity.
   *
   */
  String threshold

  /**
   * The used effort for searching bugs.
   *
   */
  String effort

  /**
   * The name of the current class which is analysed by FindBugs.
   *
   */
  String currentClassName

  /**
   * Signals if the report for the current class is opened.
   *
   */
  boolean mIsCurrentClassReportOpened = false

  /**
   * Signals if the jxr report plugin is enabled.
   *
   */
  boolean isJXRReportEnabled = false

  /**
   * The running total of bugs reported.
   *
   */
  int bugCount

  /**
   * The running total of missing classes reported.
   *
   */
  int missingClassCount

  /**
   * The running total of files analyzed.
   *
   */
  int fileCount

  /**
   * The Set of missing classes names reported.
   *
   */
  Set missingClassSet = new HashSet()

  /**
   * The running total of errors reported.
   *
   */
  int errorCount

  /**
   * Location where generated html will be created.
   *
   */

  File outputDirectory

  /**
   * Location of the Xrefs to link to.
   *
   */
  File xrefLocation

  /**
   * Location of the Test Xrefs to link to.
   *
   */
  File xrefTestLocation

  /**
   * The directories containing the sources to be compiled.
   *
   */
  List compileSourceRoots

  /**
   * The directories containing the test-sources to be compiled.
   *
   */
  List testSourceRoots

  /**
   * Run Findbugs on the tests.
   *
   */
  boolean includeTests

  /**
   * "org.apache.maven.doxia.tools.SiteTool"
   *
   */
  SiteTool siteTool



  File basedir

  GPathResult findbugsResults



  List bugClasses

  /**
   * Default constructor.
   *
   * @param sink
   *            The sink to generate the report.
   * @param bundle
   *            The resource bundle to get the messages from.
   * @param basedir
   *            The project base directory.
   * @param siteTool
   *            Doxia SiteTool Handle.
   */
  FindbugsReportGenerator(Sink sink, ResourceBundle bundle, File basedir, SiteTool siteTool) {


    assert sink
    assert bundle
    assert basedir
    assert siteTool

    this.sink = sink
    this.bundle = bundle
    this.basedir = basedir
    this.siteTool = siteTool


    this.bugClasses = []

    this.currentClassName = ""

    this.bugCount = 0
    this.missingClassCount = 0
    this.errorCount = 0
    this.fileCount = 0

  }


  /**
   * @see edu.umd.cs.findbugs.BugReporter#finish()
   */
  void printBody() {
    log.debug("Finished searching for bugs!...")
    log.debug("sink is " + sink)

    bugClasses.each() {bugClass ->
      log.debug("finish bugClass is ${bugClass}")

      printBug(bugClass)
    }

    // close the report, write it
    sink.body_()

  }

  /**
   * Prints the top header sections of the report.
   */
  private void doHeading() {
    sink.head()
    sink.title()
    sink.text(getReportTitle())
    sink.title_()
    sink.head_()

    sink.body()

    // the title of the report
    sink.section1()
    sink.sectionTitle1()
    sink.text(getReportTitle())
    sink.sectionTitle1_()

    // information about FindBugs
    sink.paragraph()
    sink.text(bundle.getString(LINKTITLE_KEY) + " ")
    sink.link(bundle.getString(LINK_KEY))
    sink.text(bundle.getString(NAME_KEY))
    sink.link_()
    sink.paragraph_()

    sink.paragraph()
    sink.text(bundle.getString(VERSIONTITLE_KEY) + " ")
    sink.italic()
    sink.text(edu.umd.cs.findbugs.Version.RELEASE)
    sink.italic_()
    sink.paragraph_()

    sink.paragraph()
    sink.text(bundle.getString(THRESHOLD_KEY) + " ")
    sink.italic()
    sink.text(findbugsThresholds.get(threshold))
    sink.italic_()
    sink.paragraph_()

    sink.paragraph()
    sink.text(bundle.getString(EFFORT_KEY) + " ")
    sink.italic()
    sink.text(findbugsEfforts.get(effort))
    sink.italic_()
    sink.paragraph_()
    sink.section1_()

  }

  /**
   * Print the bug collection to a line in the table
   *
   * @param bugInstance
   *            the bug to print
   */
  protected void printBug(String bugClass) {


    log.debug("printBug bugClass is ${bugClass}")

    openClassReportSection(bugClass)


    log.debug("printBug findbugsResults is ${findbugsResults}")

    findbugsResults.BugInstance.each() {bugInstance ->


      log.debug("bugInstance --->  ${bugInstance}")

      if ( bugInstance.Class[0].@classname.text() == bugClass ) {

        def type = bugInstance.@type.text()
        def category = bugInstance.@category.text()
        def message = bugInstance.LongMessage.text()
        def priority = bugInstance.@priority.text()
        def line = bugInstance.SourceLine[0]
        log.debug(message)

        sink.tableRow()

        // bug
        sink.tableCell()
        sink.text(message)
        sink.tableCell_()

        // category
        sink.tableCell()
        sink.text(category)
        sink.tableCell_()

        // description link
        sink.tableCell()
        sink.link(bundle.getString(DETAILSLINK_KEY) + "#" + type)
        sink.text(type)
        sink.link_()
        sink.tableCell_()

        // line
        sink.tableCell()

        if ( isJXRReportEnabled ) {
          log.debug("isJXRReportEnabled is enabled")
          sink.rawText(assembleJxrHyperlink(line))
        } else {
          sink.text(line.@start.text())
        }

        sink.tableCell_()

        // priority
        sink.tableCell()
        sink.text(findbugsPriority[priority.toInteger()])
        sink.tableCell_()

        sink.tableRow_()
      }
    }

    sink.table_()

    sink.section2_()

  }

  /**
   * Assembles the hyperlink to point to the source code.
   *
   * @param line
   *            The line number object with the bug.
   * @param lineNumber
   *            The line number to show in the hyperlink.
   * @return The hyperlink which points to the code.
   *
   */
  protected String assembleJxrHyperlink(GPathResult line) {
    String hyperlink
    String prefix


    log.debug("Inside assembleJxrHyperlink")
    log.debug("line is " + line.text())
    log.debug("outputDirectory is " + outputDirectory.getAbsolutePath())
    log.debug("xrefLocation is " + xrefLocation.getAbsolutePath())
    log.debug("xrefTestLocation is " + xrefTestLocation.getAbsolutePath())

    compileSourceRoots.each {compileSourceRoot ->
      if ( new File(compileSourceRoot + File.separator + line.@sourcepath.text()).exists() ) {
        prefix = PathTool.getRelativePath(outputDirectory.getAbsolutePath(), xrefLocation.getAbsolutePath());
        if ( !prefix ) {
          prefix = ".";
        }
        prefix = prefix + URL_SEPARATOR + xrefLocation.getName() + URL_SEPARATOR
        return
      }
    }


    if ( includeTests && !prefix ) {
      testSourceRoots.each {testSourceRoot ->
        if ( new File(testSourceRoot + File.separator + line.@sourcepath.text()).exists() ) {
          prefix = PathTool.getRelativePath(outputDirectory.getAbsolutePath(), xrefTestLocation.getAbsolutePath());
          if ( !prefix ) {
            prefix = ".";
          }
          prefix = prefix + URL_SEPARATOR + xrefTestLocation.getName() + URL_SEPARATOR
          return
        }
      }
    }

    def path = prefix + line.@classname.text().replaceAll("[.]", "/").replaceAll("[\$].*", "")
    String lineNumber = valueForLine(line)

    if ( !line ) {
      hyperlink = "<a href=\"" + path + ".html\">" + lineNumber + "</a>"
    } else {
      hyperlink = "<a href=\"" + path + ".html#" + line.@start.text() + "\">" + lineNumber + "</a>"
    }

    return hyperlink
  }

  /**
   * Gets the report title.
   *
   * @return The report title.
   *
   */
  protected String getReportTitle() {
    return bundle.getString(REPORT_TITLE_KEY)
  }

  /**
   * Initialised a bug report section in the report for a particular class.
   */
  protected void openClassReportSection(String bugClass) {
    String columnBugText = bundle.getString(COLUMN_BUG_KEY)
    String columnBugCategory = bundle.getString(COLUMN_CATEGORY_KEY)
    String columnDescriptionLink = bundle.getString(COLUMN_DETAILS_KEY)
    String columnLineText = bundle.getString(COLUMN_LINE_KEY)
    String priorityText = bundle.getString(COLUMN_PRIORITY_KEY)

    log.debug("openClassReportSection bugClass is ${bugClass}")

    log.debug("Opening Class Report Section")

    sink.anchor(bugClass)
    sink.anchor_()

    sink.section2()
    sink.sectionTitle2()
    sink.text(bugClass)
    sink.sectionTitle2_()
    sink.table()
    sink.tableRow()

    // bug
    sink.tableHeaderCell()
    sink.text(columnBugText)
    sink.tableHeaderCell_()

    // category
    sink.tableHeaderCell()
    sink.text(columnBugCategory)
    sink.tableHeaderCell_()

    // description link
    sink.tableHeaderCell()
    sink.text(columnDescriptionLink)
    sink.tableHeaderCell_()

    // line
    sink.tableHeaderCell()
    sink.text(columnLineText)
    sink.tableHeaderCell_()

    // priority
    sink.tableHeaderCell()
    sink.text(priorityText)
    sink.tableHeaderCell_()

    sink.tableRow_()
  }

  /**
   * Print the Summary Section.
   */
  protected void printSummary() {
    sink.section1()

    // the summary section
    sink.sectionTitle1()
    sink.text(bundle.getString(SUMMARY_KEY))
    sink.sectionTitle1_()

    sink.table()
    sink.tableRow()

    // classes
    sink.tableHeaderCell()
    sink.text(bundle.getString(COLUMN_CLASSES_KEY))
    sink.tableHeaderCell_()

    // bugs
    sink.tableHeaderCell()
    sink.text(bundle.getString(COLUMN_BUGS_KEY))
    sink.tableHeaderCell_()

    // Errors
    sink.tableHeaderCell()
    sink.text(bundle.getString(COLUMN_ERRORS_KEY))
    sink.tableHeaderCell_()

    // Missing Classes
    sink.tableHeaderCell()
    sink.text(bundle.getString(COLUMN_MISSINGCLASSES_KEY))
    sink.tableHeaderCell_()

    sink.tableRow_()

    sink.tableRow()

    // files
    sink.tableCell()
    sink.text(findbugsResults.FindBugsSummary.@total_classes.text())
    sink.tableCell_()

    // bug
    sink.tableCell()
    sink.text(findbugsResults.FindBugsSummary.@total_bugs.text())
    sink.tableCell_()

    // Errors
    sink.tableCell()
    sink.text(findbugsResults.Errors.@errors.text())
    sink.tableCell_()

    // Missing Classes
    sink.tableCell()
    sink.text(findbugsResults.Errors.@missingClasses.text())
    sink.tableCell_()

    sink.tableRow_()
    sink.table_()

    sink.section1_()
  }

  /**
   * Print the File Summary Section.
   */
  protected void printFilesSummary() {
    sink.section1()

    // the Files section
    sink.sectionTitle1()
    sink.text(bundle.getString(FILES_KEY))
    sink.sectionTitle1_()

    /**
     * Class Summary
     */

    sink.table()
    sink.tableRow()

    // files
    sink.tableHeaderCell()
    sink.text(bundle.getString(COLUMN_CLASS_KEY))
    sink.tableHeaderCell_()

    // bugs
    sink.tableHeaderCell()
    sink.text(bundle.getString(COLUMN_BUGS_KEY))
    sink.tableHeaderCell_()

    sink.tableRow_()

    findbugsResults.FindBugsSummary.PackageStats.ClassStats.each() {classStats ->

      def classStatsValue = classStats.'@class'.text()
      def classStatsBugCount = classStats.'@bugs'.text()

      if ( classStatsBugCount.toInteger() > 0 ) {
        sink.tableRow()

        // class name
        sink.tableCell()
        sink.link("#" + classStatsValue)
        sink.text(classStatsValue)
        sink.link_()
        sink.tableCell_()

        // class bug total count
        sink.tableCell()
        sink.text(classStatsBugCount)
        sink.tableCell_()

        sink.tableRow_()


        bugClasses << classStatsValue
      }
    }



    sink.table_()

    sink.section1_()
  }

  public void generateReport() {

    log.debug("Reporter Locale is " + this.bundle.getLocale().getLanguage())

    doHeading()

    printSummary()

    printFilesSummary()

    printBody()

    log.debug("Closing up report....................")
        
    sink.flush()
    sink.close()
  }

  /**
   * Return the value to display. If FindBugs does not provide a line number, a default message is returned. The line
   * number otherwise.
   *
   * @param line
   *            The line to get the value from.
   * @return The line number the bug appears or a statement that there is no source line available.
   *
   */
  protected String valueForLine(GPathResult line)
  {
    String value

    if ( line ) {
      def startLine = line.@start.text()
      def endLine = line.@end.text()

      if ( startLine == endLine ) {
        if ( !startLine ) {
          value = bundle.getString(NOLINE_KEY)
        } else {
          value = startLine
        }
      } else {
        value = startLine + "-" + endLine
      }
    } else {
      value = bundle.getString(NOLINE_KEY)
    }

    return value
  }

}
