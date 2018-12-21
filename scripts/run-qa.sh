#!/bin/bash

SPARK_PATH=${SPARK_HOME}

INPUT=$1
OUTPUT=$2

# Log Location on Server.
LOG_LOCATION=~/logs
exec > >(tee -i ${INPUT}-${OUTPUT}_qAssess.log)
exec 2>&1


NUM_EXECUTORS=35 
EXECUTOR_MEMORY=19g 
EXECUTOR_CORES=5

HADOOP_MASTER=hdfs://qrowd1:8020

$SPARK_PATH/bin/spark-submit \
--class net.sansa_stack.rdf.spark.RDFQualityAssessment \
--master "spark://qrowd1:7077" \
--num-executors $NUM_EXECUTORS \
--executor-memory $EXECUTOR_MEMORY \
--executor-cores $EXECUTOR_CORES \
--driver-memory 4G \
--conf spark.default.parallelism=150 \
--conf spark.locality.wait=15s \
--conf spark.network.timeout=720s \
--conf spark.sql.shuffle.partitions=50 \
$HADOOP_MASTER/GezimSejdiu/DistRDFQualityAssessment/RDFQualityAssessment-spark2.4.0_2.11-2017-12.1-SNAPSHOT.jar \
-i $HADOOP_MASTER/GezimSejdiu/DistRDFQualityAssessment/${INPUT}.nt \
-o $HADOOP_MASTER/GezimSejdiu/DistRDFQualityAssessment/output/${INPUT}-${OUTPUT}-qAssess
