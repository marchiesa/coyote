package lpsolver.signomial;

import java.util.List;
import java.util.Map;

import model.Graph;
import util.Pair;

public interface IMosekFormulationGeneratorAmpl {
	
	public void computeOptimalPerformance(String graphName, Graph g,	List<Pair<Integer>> setOfDemands,Map<Integer,Graph> destination2ht );
	
	public void computeOptimalPerformance(String graphName, Graph g,	List<Pair<Integer>> setOfDemands,Map<Integer,Graph> destination2ht , boolean aggregateCongestion);

	public void computeOptimalPerformance(String graphName, Graph g,	List<Pair<Integer>> setOfDemands,Map<Integer,Graph> destination2ht, Map<Pair<Integer>,Double> demand2estimate, double w );
	
	public void computeOptimalPerformance(String graphName, Graph g,	List<Pair<Integer>> setOfDemands,Map<Integer,Graph> destination2ht, Map<Pair<Integer>,Double> demand2estimate, double w , boolean aggregateCongestion);
	
	
}
