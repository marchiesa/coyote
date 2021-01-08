package performance.trafficmatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Graph;
import util.Pair;

public class ConstantModelCalculator implements IModelCalculator {

	@Override
	public Map<Pair<Integer>, Double> computeTrafficMatrix(Graph g,
			List<Pair<Integer>> setOfDemands) {
		return this.computeTrafficMatrix(g, setOfDemands, 1);
	}
	
		public  Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands, double margin){
		Map<Pair<Integer>,Double> demand2flow = new HashMap<Pair<Integer>,Double>();

		for(Pair<Integer> demand: setOfDemands){
			demand2flow.put(demand, 300d);
		}

		return demand2flow;
	}

		@Override
		public Map<Pair<Integer>, Double> computeTrafficMatrix(Graph g,
				List<Pair<Integer>> setOfDemands, long seed) {
			// TODO Auto-generated method stub
			return null;
		}

}
