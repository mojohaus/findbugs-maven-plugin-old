#!groovy

def findbugsHome = System.getenv("FINDBUGS_HOME")

def cli = new CliBuilder(usage:'fb2local -f findbugs.home -version version')
cli.h(longOpt: 'help', 'usage information')
cli.f(argName: 'findbugs.home',  longOpt: 'home', required: false, args: 1, type:GString, 'Findbugs home directory')
cli.v(argName: 'version',  longOpt: 'version', required: true, args: 1, type:GString, 'Findbugs version')

def opt = cli.parse(args)
if (!opt) { return }
if (opt.h) opt.usage()
if (opt.f) findbugsHome = opt.f
def findbugsVersion = opt.v

println "findbugsHome is ${findbugsHome}"
println "findbugsVersion is ${findbugsVersion}"
println "Done parsing"

def cmdPrefix = """"""

println "os.name is " + System.getProperty("os.name")

if (System.getProperty("os.name").toLowerCase().contains("windows")) cmdPrefix = """cmd /c """

def modules = ["findbugs", "annotations", "findbugs-ant", "bcel", "jsr305", "jFormatString" ]

modules.each(){ module ->
    cmd = cmdPrefix + """mvn install:install-file -DpomFile=${module}.pom -Dfile=${findbugsHome}/lib/${module}.jar -DgroupId=net.sourceforge.findbugs -DartifactId=${module} -Dversion=${findbugsVersion} -Dpackaging=jar"""
    proc = cmd.execute()
    println proc.text
}
