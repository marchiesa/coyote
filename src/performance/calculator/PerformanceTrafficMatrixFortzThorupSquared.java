package performance.calculator;
/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */

import java.util.List;
import java.util.Map;

import model.Arc;
import model.Graph;
import util.Pair;

/**
 * This class is responsible of computing the oblivious performance of a routing using the compact LP formulation
 * of Applegate & Cohen.
 * @author mosfet
 *
 */
public class PerformanceTrafficMatrixFortzThorupSquared extends APerformanceTrafficMatrix {

	private double sum; 
	/**
	 * Compute the performance of the routing in input and store it in local variables.  
	 * @param g, input graph
	 * @param arc2demand2fraction, oblivious routing  
	 */
	public void computePerformanceSpecificDemand(Graph g, List<Pair<Integer>> setOfDemands, 
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction, Map<Pair<Integer>,Double> demand2estimate,
			boolean aggregateCongestion){


		sum= 0;
		for(Arc arc : g.getArcs()){
			int idArc = arc.getId();
			if(aggregateCongestion && (idArc % 2 == 1))
				continue;
			double congestion = 0;
			for(Pair<Integer> demand : demand2estimate.keySet()){
				if(arc2demand2fraction.get(idArc) != null && 
						arc2demand2fraction.get(idArc).get(demand)!=null)
					congestion+=arc2demand2fraction.get(idArc).get(demand)*demand2estimate.get(demand);
				if(aggregateCongestion){
					if(arc2demand2fraction.get(g.getReversedArc(idArc).getId())!= null && 
							arc2demand2fraction.get(g.getReversedArc(idArc).getId()).get(demand)!=null)
						congestion+=arc2demand2fraction.get(g.getReversedArc(idArc).getId()).get(demand)*demand2estimate.get(demand);
				}
			}
			congestion/=g.getArcById(idArc).getCapacity();
			sum += Math.pow(congestion, 2);
		}
	}


	public double getOpt() {
		return sum;
	}
	
	public Integer getMostCongestedIdArc(){
		return null;
	}



}