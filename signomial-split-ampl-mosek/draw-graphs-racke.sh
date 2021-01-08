#!/bin/bash


#racke
for file in `ls solutions-racke/*solution* | sed  -e "s/-solution.*//" | uniq`
#for file in abilene_cost.lgf
do
	echo $file
		fileName=`echo $file | sed -e "s/[^\/]*\///" -e "s/\.lgf//"`
	for obj in maxcong fortz
	do
		for x in `ls -l solutions-racke/$fileName*solution* | awk '{print $9}'`
		do
			number=`echo $x | sed -e "s/.*$fileName/$fileName/" -e "s/.*solution//"`
			rm draw.plot
			echo "set term png" >> draw.plot
			echo "set output \"results-racke/graph-comparison-$fileName-racke-$number-$obj.png\""  >> draw.plot
			echo "set title \"$fileName-racke-$number-$obj\"" >> draw.plot
			echo "set auto x"  >> draw.plot
			echo "set yrange [0.5:1.6]"  >> draw.plot
			echo "set style data histogram"  >> draw.plot
			echo "set style histogram cluster gap 1"  >> draw.plot
			echo "set style fill solid"  >> draw.plot
			echo "set xtic rotate by -45 scale 0"  >> draw.plot
			plot="plot "
			for traffic in gravity bimodal gaussian uniform
			do
				plot=`echo $plot "'results-racke/$fileName-results-$number-$traffic-$obj.txt' u 2:xticlabel(1) title \"$traffic\", "`
			done
			plot=`echo $plot "'results-racke/$fileName-results-$number-constant-$obj.txt' u 2:xticlabel(1) title \"constant\" "`
			echo $plot >> draw.plot
			gnuplot draw.plot
		done
	done
done

