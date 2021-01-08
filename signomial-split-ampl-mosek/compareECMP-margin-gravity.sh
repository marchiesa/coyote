#!/bin/bash

#ecmp signomial
#for file in `ls solutions/ | sed  -e "s/-destination2ht.*//"  -e "s/-splitting-ratio-formulation-data.*//" | uniq`
for file in triangle-equal.lgf
do
	echo results/$file
	fileName=`echo $file | sed "s/\.lgf//"`
	echo $fileName
	java -jar signomial-formulation.jar -check topologies/Backbone/$file  --solutionecmp --augment > /dev/null

	java -jar signomial-formulation.jar -check topologies/Backbone/$file --augment > /dev/null

	java -jar signomial-formulation.jar -checkdemandmargin topologies/Backbone/$file  --solutionecmp --augment > /dev/null

	java -jar signomial-formulation.jar -checkdemandmargin topologies/Backbone/$file --augment > /dev/null

	java -jar signomial-formulation.jar -checkdemandmargin topologies/Backbone/$file --augment --margin > /dev/null
done



