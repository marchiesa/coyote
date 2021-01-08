package performance.trafficmatrix;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class GravityModelCalculator implements IModelCalculator{
	
	public  Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands){
		return this.computeTrafficMatrix(g, setOfDemands,1d);
	}
	
	public  Map<Pair<Integer>,Double> computeTrafficMatrix(Graph g, List<Pair<Integer>> setOfDemands, double margin){
		Map<Pair<Integer>,Double> demand2flow = new HashMap<Pair<Integer>,Double>();
		
		Map<Integer,Double> vertexId2capacity= new HashMap<Integer,Double>();
		
		Set<Integer> egressPoints = new TreeSet<Integer>();
		for(Pair<Integer> demand: setOfDemands){
			egressPoints.add(demand.getFirst());
			egressPoints.add(demand.getSecond());
		}
		
		double sumOfCapacities=0d;
		for(Integer vertexId: egressPoints){
			Vertex v = g.getVertexById(vertexId);
			double sum =0d;
			for(Arc arc : v.getArcs()){
				sum+=arc.getCapacity();
			}
			vertexId2capacity.put(vertexId, sum);
			sumOfCapacities+=sum;
		}
		
		//double squaredCapacities = sumOfCapacities*sumOfCapacities;
		for(Pair<Integer> demand : setOfDemands){
			//double flow = vertexId2capacity.get(demand.getFirst())*vertexId2capacity.get(demand.getSecond())/squaredCapacities;
			double flow = vertexId2capacity.get(demand.getFirst())*vertexId2capacity.get(demand.getSecond())/sumOfCapacities;
			flow  = new Random().nextDouble()*(flow*margin - flow/margin) + flow/margin;
			demand2flow.put(new Pair<Integer>(demand.getFirst(),demand.getSecond()), flow);
		}
	
		return demand2flow;
	}

	@Override
	public Map<Pair<Integer>, Double> computeTrafficMatrix(Graph g,
			List<Pair<Integer>> setOfDemands, long seed) {
		return this.computeTrafficMatrix(g, setOfDemands);
	}

}
