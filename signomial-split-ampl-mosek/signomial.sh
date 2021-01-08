#!/bin/bash

mkdir solutions
mkdir results
mkdir nl

#for x in `ls -l topologies/Backbone | sort -nk5 | awk '{print $9}'`
for x in triangle-equal.lgf
do
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
		mosek $x.nl -AMPL MSK_IPAR_INTPNT_ORDER_METHOD=MSK_ORDER_METHOD_APPMINLOC #solve $x.nl
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

	prev=0.50
	mv $x-splitting-ratio-formulation-data.txt $x-splitting-ratio-formulation-data-$prev.txt
	
    for j in `seq 1 0.50 5`
    do
		echo $x $j
		java -jar signomial-formulation.jar -demand topologies/Backbone/$x $j --augment
		rm $x-splitting-ratio-formulation-data.txt
		mv  $x-splitting-ratio-formulation-data-$prev.txt  $x-splitting-ratio-formulation-data.txt
		mv $x-destination2ht-augment.txt solutions/$x-destination2ht-augment.txt
		
		#mv destination2ht.txt solutions/$x-destination2ht.txt
		last=100000
		for i in `seq 1 10`;
		do
		    echo $i 
		    java -jar signomial-formulation.jar -adjustsolution topologies/Backbone/$x --augment --aggregate
		    java -jar signomial-formulation.jar -update topologies/Backbone/$x
		    cat $x-formulation.mod > $x-formulation2.mod
		    cat $x-splitting-ratio-formulation.mod >> $x-formulation2.mod
			cat create-nl.sh | sed "s/FILE/$x/g" > create-nl-$x.sh
			sbatch --mem=16GB -c1 --time=1-0 -w sed-15 "create-nl-$x.sh" #generate $x.nl
			while [ ! -f $x.ready ] # wait until $x.nl is created
			do
				sleep 2
			done
			rm $x.ready
			mosek $x.nl -AMPL MSK_IPAR_INTPNT_ORDER_METHOD=MSK_ORDER_METHOD_APPMINLOC#MSK_IPAR_OPTIMIZER=MSK_OPTIMIZER_INTPNT MSK_DPAR_INTPNT_NL_TOL_DFEAS=0.01 MSK_DPAR_INTPNT_NL_TOL_PFEAS=0.01 MSK_DPAR_INTPNT_NL_TOL_REL_GAP=0.01 #solve $x.nl
			mv $x.nl nl/$x-$j.nl
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
		cp $x-splitting-ratio-formulation-data.txt solutions/$x-splitting-ratio-formulation-data-$j.txt
		mv $x-alpha.txt results/$x-alpha-$j.txt
	    mv $x-splitting-ratio-formulation-data.txt $x-splitting-ratio-formulation-data-$j.txt
	    prev=`echo $prev + 0.50 | bc -l`

    done
    rm $x-splitting-ratio-formulation-data-5.00.txt
	rm *$x*
done


