package performance.trafficmatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Graph;
import util.Pair;

public class GaussianModelCalculator implements IModelCalculator {

	@Override
	public Map<Pair<Integer>, Double> computeTrafficMatrix(Graph g,
			List<Pair<Integer>> setOfDemands) {
		return this.computeTrafficMatrix(g, setOfDemands, 1);
	}
	
		public  Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands, double margin){
		Map<Pair<Integer>,Double> demand2flow = new HashMap<Pair<Integer>,Double>();


		Random random = new Random();
		for(Pair<Integer> demand: setOfDemands){
			Double flow;
			flow = (random.nextInt(401)+100)+  random.nextGaussian()*40;
			if(flow>0){
				flow  = new Random().nextDouble()*(flow*margin - flow/margin) + flow/margin;
				demand2flow.put(new Pair<Integer>(demand.getFirst(),demand.getSecond()), flow);
				demand2flow.put(demand, flow);
			}
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
