#!/usr/bin/env groovy

/*
 *
 * Usage Examples
 * 
 * groovy versionTest -m /opt -p /usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin
 * groovy versionTest -m /opt -p /usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin -b
 * groovy versionTest -m /opt -p /usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin -l NoBuildTestResults.txt
 * groovy versionTest -m /opt -p /usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin -l BuildTestResults.txt -b
 *
*/


def javaHome = System.getenv("JAVA_HOME")
File logFile
def buildPlugin = false
def mvnPhase = ""

def cli = new CliBuilder(usage:'versionTest -m maven.top.directory -p path  <-l logfile> <-b>')
cli.h(longOpt: 'help', 'usage information')
cli.m(argName: 'maven.top.directory',  longOpt: 'maven', required: true, args: 1, type:GString, 'Maven top directory containing version of Mven for test')
cli.p(argName: 'path',  longOpt: 'path', required: true, args: 1, type:GString, 'Path for System')
cli.l(argName: 'log',  longOpt: 'logFile', required: false, args: 1, type:GString, 'Optional log file')
cli.b(argName: 'build',  longOpt: 'build', required: false, args: 0, type:GString, 'build every time')


def opt = cli.parse(args)
if (!opt) { return }
if (opt.h) opt.usage()
if (opt.m) mavenTopDir = opt.m
if (opt.p) executePath = opt.p
if (opt.l) logFile = new File(opt.l)
if (opt.b) buildPlugin = true

if (logFile) {
 logFile.write "\n"
}


println "Maven Top Directory is ${mavenTopDir}"

mavenDirs = []

new File(mavenTopDir).eachDir{ dir ->
  if (dir.name.startsWith("maven-")) {
    println "This Maven directory is ${dir.name}"
    mavenDirs << dir
  }
}

println mavenDirs

String[] ENVtoArray() { ENV.collect { k, v -> "$k=$v" } }


if (buildPlugin) {
  mvnPhase = "clean install"
} else {
  mvnPhase = "shitty:clean shitty:test"
}

mavenDirs.each(){ mavenDir ->
  ENV = [JAVA_HOME: javaHome]
  ENV.M2_HOME = mavenDir.getAbsolutePath()
  ENV.PATH = executePath + File.pathSeparator + "${mavenDir}/bin"
  ENVtoArray()
  
  println "****************************************"
  proc = "mvn -version".execute(ENVtoArray(), null)
  proc.waitFor()
  procText = proc.text
  println procText
  println "****************************************"

  if (logFile) {
    logFile << procText
  }

  proc = "${mavenDir}/bin/mvn -Dshit=true ${mvnPhase}".execute(ENVtoArray(), null)
  proc.waitFor()
  procText = proc.text
  println procText

  if (logFile) {
    logFile << procText
  }
}
