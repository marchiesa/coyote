#!/bin/bash

#ecmp signomial
for file in `ls solutions/ | sed  -e "s/-destination2ht.*//"  -e "s/-splitting-ratio-formulation-data.*//" | uniq`
do
	for i in `seq 1 10`
	do
	 echo results/$file
	 fileName=`echo $file | sed "s/\.lgf//"`
	 echo $fileName
		for traffic in gravity bimodal gaussian uniform constant
		do
			for obj in maxcong fortz
			do
				echo $traffic $obj
				java -jar signomial-formulation.jar -checkdemand topologies/Backbone/$file  --$traffic --$obj --solutionecmp > /dev/null
				cat results-ecmp/$fileName-ecmp-$traffic-$obj.txt >>  results-ecmp/$fileName-ecmp-$traffic-$obj-accumulator.txt

				java -jar signomial-formulation.jar -checkdemand topologies/Backbone/$file --$traffic --$obj --augment > /dev/null
				cat results/$fileName-$traffic-$obj.txt >>  results/$fileName-$traffic-$obj-accumulator.txt
			done
		done
	done
done


#ecmp signomial
for file in `ls solutions/ | sed  -e "s/-destination2ht.*//"  -e "s/-splitting-ratio-formulation-data.*//" | uniq`
do
 fileName=`echo $file | sed "s/\.lgf//"`
 echo $fileName
	for traffic in gravity bimodal gaussian uniform constant
	do
		for obj in maxcong fortz
		do
			cat results-ecmp/$fileName-ecmp-$traffic-$obj-accumulator.txt | awk '{sum+=$1;count+=1} END{avg = sum/count; printf("%f\n", avg)}' > results-ecmp/$fileName-ecmp-$traffic-$obj.txt
			cat results/$fileName-$traffic-$obj-accumulator.txt | awk '{sum+=$1;count+=1} END{avg = sum/count; printf("%f\n", avg)}' > results/$fileName-$traffic-$obj.txt
		done
	done
done

