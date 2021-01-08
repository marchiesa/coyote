#!/bin/bash

mkdir solutions
mkdir results
mkdir nl

x="TOPOLOGY"
j=MARGIN

#for x in `ls -l topologies/Backbone | sort -nk5 | awk '{print $9}'`
echo $x 
java -jar signomial-formulation.jar topologies/Backbone/$x --augment --aggregate
mv $x-destination2ht-augment.txt solutions/$x-destination2ht-augment.txt
#mv destination2ht.txt solutions/$x-destination2ht.txt
last=100000
for i in `seq 1 10`;
do
    echo $i 
    java -jar signomial-formulation.jar -adjustsolution topologies/Backbone/$x --augment
    java -jar signomial-formulation.jar -update topologies/Backbone/$x
    cat $x-formulation.mod > $x-formulation2.mod
    cat $x-splitting-ratio-formulation.mod >> $x-formulation2.mod
	
	cat create-nl.sh | sed "s/FILE/$x/g" > create-nl-$x.sh
	sbatch --mem=16GB -c2 --time=1-0 -w sed-15 "create-nl-$x.sh" #generate $x.nl
	while [ ! -f $x.ready ] # wait until $x.nl is created
	do
		sleep 2
	done
	rm $x.ready
	echo solving
	mosek $x.nl outlev=10 -AMPL MSK_IPAR_INTPNT_ORDER_METHOD=MSK_ORDER_METHOD_APPMINLOC #solve $x.nl
	mv $x.nl nl/$x.nl
	#extract a solution from $x.sol  and put it in solution.txt
	java -jar signomial-formulation.jar -readsolution topologies/Backbone/$x
	    cat $x-alpha.txt
    alpha=`cat $x-alpha.txt | sed "s#\ ##g"| sed "s#alpha##g" | sed "s#=#\ #g"`
    diff=`echo "$last - $alpha" | bc -l`
    if (( $(echo "$diff < 0.05" | bc -l) )); then break; fi
    last=$alpha
done    
java -jar signomial-formulation.jar -adjustsolution topologies/Backbone/$x --augment
#mv fractional-flows-data.txt $x-fractional-flows-data.txt
cp $x-splitting-ratio-formulation-data.txt solutions/$x-splitting-ratio-formulation-data.txt
mv $x-alpha.txt results/$x-alpha.txt

