package obliviousrouting.mcct;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;

/**
 * This class returns a set of per-destination shortest path trees.
 * @author mosfet
 *
 */
public class MCCTShortestPath implements MCCTSolver {

	private Map<Integer,Map<Integer,Set<Arc>>> destination2source2path;
	//private Map<Pair<Vertex>,List<Arc>> treeEdge2path;
	private Graph graph;
	private Map<Integer, Map<Integer, Double>> idVertex2idVertex2demand;

	public MCCTShortestPath(){
		this.idVertex2idVertex2demand = new HashMap<Integer, Map<Integer, Double>>();
	}
	
	public Graph getGraph() {
		return graph; 
	}
 
	@Override
	public Map<Integer,Map<Integer,Set<Arc>>> getTrees() {
		this.destination2source2path = new HashMap<Integer,Map<Integer,Set<Arc>>>();
		for(Integer source: this.idVertex2idVertex2demand.keySet()){
			
			for(Integer destination: this.idVertex2idVertex2demand.get(source).keySet()){
				Map<Integer,Set<Arc>> source2path = this.destination2source2path.get(destination);
				if(source2path == null){
					source2path = new HashMap<Integer,Set<Arc>>();
					this.destination2source2path.put(destination, source2path);
				}
				source2path.put(source, new TreeSet<Arc>(this.graph.getShortestPath(this.graph.getVertexById(source), this.graph.getVertexById(destination))));
			}
		}
		return this.destination2source2path;
	}

	@Override
	public void addDemand(Vertex v1, Vertex v2, Double demand) {
		Map<Integer, Double> idVertex2Demand = this.idVertex2idVertex2demand.get(v1.getId());
		if(idVertex2Demand == null){
			idVertex2Demand = new HashMap<Integer, Double>();
			this.idVertex2idVertex2demand.put(v1.getId(), idVertex2Demand);
		}
		idVertex2Demand.put(v2.getId(), demand);
	}

	@Override
	public void reset() {
	}


	@Override
	public void setGraph(Graph g) {
		this.graph = g;
	}

	@Override
	public Tree getBestTree() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
