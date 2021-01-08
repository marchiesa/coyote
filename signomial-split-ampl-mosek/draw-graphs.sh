#!/bin/bash

#signomial
for file in `ls solutions/ | sed  -e "s/-destination2ht.*//"  -e "s/-splitting-ratio-formulation-data.*//" | uniq`
#for file in 1221
do
	fileName=`echo $file | sed "s/\..*//"`
	echo $fileName
	for obj in maxcong fortz
	do
		rm draw.plot
		echo "set term png" >> draw.plot
		echo "set output \"results/graph-comparison-$fileName-$obj.png\""  >> draw.plot
		echo "set title \"$fileName-$obj\"" >> draw.plot
		echo "set auto x"  >> draw.plot
		echo "set yrange [0.7:1.6]"  >> draw.plot
		echo "set style data histogram"  >> draw.plot
		echo "set style histogram cluster gap 1"  >> draw.plot
		echo "set style fill solid"  >> draw.plot
		echo "set xtic rotate by -45 scale 0"  >> draw.plot
		plot="plot "
		for traffic in gravity bimodal gaussian uniform
		do
			plot=`echo $plot "'results/$fileName-results-$traffic-$obj.txt' u 2:xticlabel(1) title \"$traffic\", "`
		done
		plot=`echo $plot "'results/$fileName-results-constant-$obj.txt' u 2:xticlabel(1) title \"constant\" "`
		echo $plot >> draw.plot
		gnuplot draw.plot
	done
done

