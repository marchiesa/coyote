package daggenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Graph;
import util.Pair;

public abstract class IDagGenerator {
	
	public abstract Map<Integer,Graph> computeDag(Graph g, List<Pair<Integer>> setOfDemands);
	
	protected Map<Integer, Set<Integer>> computeDestination2Sources(
			List<Pair<Integer>> setOfDemands) {
		Map<Integer, Set<Integer>> destination2sources = new HashMap<Integer,Set<Integer>>();
		for(Pair<Integer> demand : setOfDemands){
			if(destination2sources.get(demand.getSecond())==null)
				destination2sources.put(demand.getSecond(), new TreeSet<Integer>());
			destination2sources.get(demand.getSecond()).add(demand.getFirst());
		}
		return destination2sources;
	}

}
