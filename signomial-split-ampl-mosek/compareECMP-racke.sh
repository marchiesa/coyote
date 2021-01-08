#!/bin/bash

mkdir solutions-racke
mkdir results-racke
mkdir results-ecmp
#racke
for file in `ls -l topologies/Backbone | sort -nk5 | awk '{print $9}'`
#for file in abilene_cost.lgf
do
	fileName=`echo $file | sed "s/\.lgf//"`
	for i in `seq 1 0`
	do
	 echo results/$file
		for traffic in gravity bimodal gaussian uniform constant
		do
			for obj in maxcong fortz
			do
				echo $traffic $obj
				java -jar signomial-formulation.jar -ecmp topologies/Backbone/$file  --$traffic --$obj > /dev/null
				cat results-ecmp/$fileName-ecmp-$traffic-$obj.txt >>  results-ecmp/$fileName-ecmp-$traffic-$obj-accumulator.txt
				
				java -jar signomial-formulation.jar -racke topologies/Backbone/$file  --$traffic --$obj > /dev/null
				for x in `ls -l solutions-racke/$fileName*solution* | awk '{print $9}'`
				do
					echo $x
					number=`echo $x | sed -e "s/.*$fileName/$fileName/" -e "s/.*solution//"`
					cat results-racke/$fileName-racke-$number-$traffic-$obj.txt >>  results-racke/$fileName-racke-$number-$traffic-$obj-accumulator.txt
				done
			done
		done
	done
done


#racke
for file in `ls solutions-racke/*solution* | sed  -e "s/-solution.*//" | uniq`
#for file in abilene_cost.lgf
do
	echo $file
	fileName=`echo $file | sed -e "s/[^\/]*\///" -e "s/\.lgf//"`
	for traffic in gravity bimodal gaussian uniform constant
	do
		for obj in maxcong fortz
		do
			cat results-ecmp/$fileName-ecmp-$traffic-$obj-accumulator.txt | awk '{sum+=$1;count+=1} END{avg = sum/count; printf("%f\n", avg)}' > results-ecmp/$fileName-ecmp-$traffic-$obj.txt
			for x in `ls -l solutions-racke/$fileName*solution* | awk '{print $9}'`
			do
				number=`echo $x | sed -e "s/.*$fileName/$fileName/" -e "s/.*solution//"`
				cat results-racke/$fileName-racke-$number-$traffic-$obj-accumulator.txt | awk '{sum+=$1;count+=1} END{avg = sum/count; printf("%f\n", avg)}' > results-racke/$fileName-racke-$number-$traffic-$obj.txt
			done
		done
	done
done

