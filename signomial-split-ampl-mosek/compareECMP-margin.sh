#!/bin/bash

#ecmp signomial
#for file in `ls solutions/ |  sed  -e "s/-destination2ht.*//" -e "s/-graph.*//"  -e "s/-splitting-ratio-formulation-data.*//" -e "s/-optimal.*//" | uniq`
for file in Digex.lgf
do
	echo results/$file
	fileName=`echo $file | sed "s/\.lgf//"`
	echo $fileName
#	java -jar signomial-formulation.jar -optimaldag topologies/Backbone/$file  --augment --aggregate > /dev/null
#	mv $file-optimalDAG-splitting-ratio-formulation-data.txt 	solutions/$file-optimalDAG-splitting-ratio-formulation-data.txt

	java -jar signomial-formulation.jar -check topologies/Backbone/$file  --solutionecmp --augment  --aggregate > /dev/null

	java -jar signomial-formulation.jar -check topologies/Backbone/$file  --optimal-dag --augment  --aggregate > /dev/null

	java -jar signomial-formulation.jar -check topologies/Backbone/$file --augment  --aggregate > /dev/null

#	java -jar signomial-formulation.jar -checkdemandmargin topologies/Backbone/$file  --solutionecmp --augment  --aggregate > /dev/null

#	java -jar signomial-formulation.jar -checkdemandmargin topologies/Backbone/$file --augment  --aggregate > /dev/null

#	java -jar signomial-formulation.jar -checkdemandmargin topologies/Backbone/$file --optimal-dag --augment  --aggregate > /dev/null

#	java -jar signomial-formulation.jar -checkdemandmargin topologies/Backbone/$file --augment --margin  --aggregate > /dev/null
done



