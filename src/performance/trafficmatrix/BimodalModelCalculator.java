package performance.trafficmatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Graph;
import util.Pair;

public class BimodalModelCalculator implements IModelCalculator{
	
	public  Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands){
		return this.computeTrafficMatrix(g, setOfDemands, 1);
	}
	
		
	public  Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands, double margin){
		return this.computeTrafficMatrix(g, setOfDemands, -1l);
	}


		@Override
		public Map<Pair<Integer>, Double> computeTrafficMatrix(Graph g,
				List<Pair<Integer>> setOfDemands, long seed) {
			Map<Pair<Integer>,Double> demand2flow = new HashMap<Pair<Integer>,Double>();
			
			Random random = null;
			if(seed==-1l)
				random = new Random();
			else
				random = new Random(seed);
			for(Pair<Integer> demand: setOfDemands){
				Double flow;
				if(random.nextFloat()<0.95)
					flow = random.nextGaussian()*20+10;
				else
					flow = random.nextGaussian()*20+400;
				if(flow<0)
					flow=0d;
				//flow  = new Random().nextDouble()*(flow*margin - flow/margin) + flow/margin;
					demand2flow.put(demand, flow);
				
			}
			
			return demand2flow;
		}

}
