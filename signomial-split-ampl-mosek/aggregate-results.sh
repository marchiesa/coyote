#!/bin/bash

echo "" > results.txt

#signomial
for file in `ls solutions/ | sed  -e "s/-destination2ht.*//"  -e "s/-splitting-ratio-formulation-data.*//" | uniq`
#for file in 1221
do
    fileName=`echo $file | sed "s/\..*//"`
	echo $fileName
    for traffic in gravity bimodal gaussian uniform constant
    do
		for obj in maxcong fortz
		do
		    ecmp=`cat results-ecmp/$fileName-ecmp-$traffic-$obj.txt`
		    weighted=`cat results/$fileName-$traffic-$obj.txt`
		    ratio=`echo $weighted/$ecmp | bc -l`
		    echo $fileName $ratio  > results/$fileName-results-$traffic-$obj.txt
		done
    done
done

