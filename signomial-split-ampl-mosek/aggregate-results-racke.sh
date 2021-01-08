#!/bin/bash


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
			for x in `ls -l solutions-racke/$fileName*solution* | awk '{print $9}'`
				do
				number=`echo $x | sed -e "s/.*$fileName/$fileName/" -e "s/.*solution//"`
				ecmp=`cat results-ecmp/$fileName-ecmp-$traffic-$obj.txt`
				racke=`cat results-racke/$fileName-racke-$number-$traffic-$obj.txt`
				ratio=`echo $racke/$ecmp | bc -l`
				echo $fileName $ratio  > results-racke/$fileName-results-$number-$traffic-$obj.txt
			done
		done
    done
done
