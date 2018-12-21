#!/bin/bash

INPUT=$1
METHOD=$2

# Log Location on Server.
LOG_LOCATION=~/logs
exec > >(tee -i ${INPUT}-${METHOD}_qAssess-Luzzu.log)
exec 2>&1


java -cp luzzu-qualityassessment-java-0.1.0-SNAPSHOT.jar \
io.github.luzzu.qualitymetrics.Main \
${INPUT} ${METHOD}
