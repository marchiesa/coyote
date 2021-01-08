#!/bin/bash

#signomial
#for file in `ls solutions/ |  sed  -e "s/-destination2ht.*//"  -e "s/-splitting-ratio-formulation-data.*//" -e "s/-optiaml.*//" | uniq`
for file in Digex.lgf
do
	fileName=`echo $file | sed "s/\..*//"`
	echo $fileName
	rm draw.plot
	echo "set term png" >> draw.plot
	echo "set output \"results/graph-comparison-$fileName.png\""  >> draw.plot
	echo "set title \"$fileName\"" >> draw.plot
	echo "set xlabel \"margin\"" >> draw.plot
	echo "set ylabel \"oblivious performance\"" >> draw.plot
	echo "set auto x"  >> draw.plot
	echo "set yrange [1:]"  >> draw.plot
	plot="plot "
	plot=`echo $plot "'results/$fileName-ecmp-gravity-maxcong-margin.txt' using 1:2 with lines title \"ospf\", "`
	plot=`echo $plot "'results/$fileName-optimal-dag-gravity-maxcong-margin.txt' using 1:2 with lines title \"no-margin-opt\", "`
	plot=`echo $plot "'results/$fileName-signomial-gravity-maxcong-margin.txt' using 1:2 with lines title \"signomial-unbounded\" ",`
	plot=`echo $plot "'results/$fileName-margin-gravity-maxcong-margin.txt' using 1:2 with lines title \"signomial-bounded\" "`
	echo $plot >> draw.plot
	gnuplot draw.plot
done

