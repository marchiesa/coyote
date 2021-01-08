package obliviousrouting.mcct;

import java.util.Map;
import java.util.Set;

import model.Arc;
import model.Graph;
import model.Vertex;

public interface MCCTSolver {
	
	public void setGraph(Graph g);
	
	/**
	 * @return the best decomposition tree  
	 */
	public Tree getBestTree();
	
	/**
	 * @return a set of per-destination shortest path trees modeled as a map {destination : { source : {set(arc)}}}
	 */
	public Map<Integer,Map<Integer,Set<Arc>>> getTrees();
	
	/**
	 * Add a new demand
	 *  
	 * @param v1 from
	 * @param v2 to
	 * @param demand amount
	 */
	public void addDemand(Vertex v1, Vertex v2, Double demand);
	
	/**
	 * reset the status of the MCCTSolver object
	 */
	public void reset();
	
}
