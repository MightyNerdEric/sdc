#!/bin/sh

##############################
#   Validation Tool   #
##############################

CURRENT_DIR=`pwd`
BASEDIR=$(dirname $0)

if [ `echo ${BASEDIR} | cut -c1-1` = "/" ]
then
                FULL_PATH=$BASEDIR
else
                FULL_PATH=$CURRENT_DIR/$BASEDIR
fi

. ${FULL_PATH}/baseOperation.sh

mainClass="org.openecomp.sdc.asdctool.main.ValidationTool"

#command="java $JVM_LOG_FILE -cp $JARS $mainClass reportOutput.txt /home/vagrant/catalog-be/config/catalog-be/"
command="java $JVM_LOG_FILE -cp $JARS $mainClass . /apps/jetty/base/be/config/catalog-be/"
echo $command

$command
result=$?

echo "***********************************"
echo "***** $result *********************"
echo "***********************************"

exit $result