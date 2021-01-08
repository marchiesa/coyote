#!/bin/bash

mkdir results-optimal
mkdir results-ecmp
rm results-optimal/results.txt
#racke
for file in `ls -l topologies/Backbone | sort -nk5 | awk '{print $9}'`
#for file in abilene_cost.lgf
do
	fileName=`echo $file | sed "s/\.lgf//"`
	echo results/$file
	
	java -jar signomial-formulation.jar -ecmp topologies/Backbone/$file  --gravity --maxcong > /dev/null
				
	java -jar signomial-formulation.jar -optimal topologies/Backbone/$file  --gravity --maxcong  > /dev/null
    ecmp=`cat results-ecmp/$fileName-ecmp-gravity-maxcong.txt`
	optimal=`cat results-optimal/$fileName-optimal-gravity-maxcong.txt`
	ratio=`echo $ecmp/$optimal | bc -l`
	echo $fileName $ratio  >> results-optimal/results.txt
done

rm draw.plot
echo "set term png" >> draw.plot
echo "set output \"results-optimal/graph-comparison-optimal-gravity.png\""  >> draw.plot
echo "set title \"ecmp over optimal - gravity model\"" >> draw.plot
echo "set auto x"  >> draw.plot
echo "set yrange [0.9:5]"  >> draw.plot
echo "set style data histogram"  >> draw.plot
echo "set style histogram cluster gap 1"  >> draw.plot
echo "set style fill solid"  >> draw.plot
echo "set xtic rotate by -45 scale 0"  >> draw.plot
echo "plot 'results-optimal/results.txt' u 2:xticlabel(1) title \"ecmp vs optimal\" " >> draw.plot
gnuplot draw.plot



