#!/usr/bin/env groovy

def findbugsHome = System.getenv("FINDBUGS_HOME")

def cli = new CliBuilder(usage:'fb2repo -f findbugs.home -version version')
cli.h(longOpt: 'help', 'usage information')
cli.f(argName: 'findbugs.home',  longOpt: 'home', required: false, args: 1, type:GString, 'Findbugs home directory')
cli.v(argName: 'version',  longOpt: 'version', required: true, args: 1, type:GString, 'Findbugs version')

def opt = cli.parse(args)
if (opt.h) cli.help()
if (opt.f) findbugsHome = opt.f
def findbugsVersion = opt.v

println "findbugsHome is ${findbugsHome}"
println "findbugsVersion is ${findbugsVersion}"
println "Done parsing"

def cmdPrefix = """"""

println "os.name is " + System.getProperty("os.name")

if (System.getProperty("os.name").toLowerCase().contains("windows")) cmdPrefix = """cmd /c """



def cmd = cmdPrefix + """mvn repository:bundle-pack -Dfile=${findbugsHome}/lib/findbugs.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugs -Dversion=${findbugsVersion}"""
def proc = cmd.execute()
println proc.text

cmd = cmdPrefix + """mvn repository:bundle-pack -Dfile=${findbugsHome}/lib/annotations.jar -DgroupId=net.sourceforge.findbugs -DartifactId=annotations -Dversion=${findbugsVersion}"""
proc = cmd.execute()
println proc.text

cmd = cmdPrefix + """mvn repository:bundle-pack -Dfile=${findbugsHome}/lib/findbugs-ant.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugs-ant -Dversion=${findbugsVersion}"""
proc = cmd.execute()
println proc.text

cmd = cmdPrefix + """mvn repository:bundle-pack -Dfile=${findbugsHome}/lib/bcel.jar -DgroupId=net.sourceforge.findbugs -DartifactId=bcel -Dversion=${findbugsVersion}"""
proc = cmd.execute()
println proc.text

cmd = cmdPrefix + """mvn repository:bundle-pack -Dfile=${findbugsHome}/lib/jsr305.jar -DgroupId=net.sourceforge.findbugs -DartifactId=jsr305 -Dversion=${findbugsVersion}"""
proc = cmd.execute()
println proc.text

