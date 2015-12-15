#!/bin/sh

. ./env.sh

echo "JAVA_HOME = " $JAVA_HOME
echo "CVSROOT = " $CVSROOT


if [ -z $JAVA_HOME ]
then
	echo "error"
	echo "please check JAVA_HOME"
	echo "please check CVSROOT"
	exit 1
fi

$JAVA_HOME/bin/java -Dcvs.root=$CVSROOT -jar clobcopy-1.0-jar-with-dependencies.jar cfgFile=clobcopy.properties
