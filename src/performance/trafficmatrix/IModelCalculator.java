package performance.trafficmatrix;

import java.util.List;
import java.util.Map;

import model.Graph;
import util.Pair;

public interface IModelCalculator {
	
	public Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands);
	
	public Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands, long seed);

	public  Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands, double margin);
}
