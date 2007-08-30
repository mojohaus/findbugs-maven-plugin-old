
cls
@call mvn repository:bundle-pack -Dfile=lib/findbugs.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugs -Dversion=%1%


@call mvn repository:bundle-pack -Dfile=lib/annotations.jar -DgroupId=net.sourceforge.findbugs -DartifactId=annotations -Dversion=%1%
 

@call mvn repository:bundle-pack -Dfile=plugin/coreplugin.jar -DgroupId=net.sourceforge.findbugs -DartifactId=coreplugin -Dversion=%1%


@call mvn repository:bundle-pack -Dfile=lib/findbugs-ant.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugs-ant -Dversion=%1%


@call mvn repository:bundle-pack -Dfile=lib/findbugsGUI.jar -DgroupId=net.sourceforge.findbugs -DartifactId=findbugsGUI -Dversion=%1%


@call mvn repository:bundle-pack -Dfile=lib/bcel.jar -DgroupId=net.sourceforge.findbugs -DartifactId=bcel -Dversion=%1%


