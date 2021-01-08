package performance.calculator;

import java.util.List;
import java.util.Map;

import model.Graph;
import util.Pair;

public abstract class APerformanceTrafficMatrix {
	
	public void computePerformanceSpecificDemand(Graph g, List<Pair<Integer>> setOfDemands, 
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction, Map<Pair<Integer>,Double> demand2estimate){
		this.computePerformanceSpecificDemand(g, setOfDemands, arc2demand2fraction, demand2estimate, false);
	}

	public abstract double getOpt();
	
	public abstract Integer getMostCongestedIdArc();

	public abstract void computePerformanceSpecificDemand(Graph g, List<Pair<Integer>> setOfDemands, 
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction, Map<Pair<Integer>,Double> demand2estimate, boolean aggregateCongestion);
	
}
