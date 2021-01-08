package obliviousrouting;

import inputoutput.RackeSolutionTransform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lpsolver.CompactLPSolverObliviousPerformance;
import model.Arc;
import model.Graph;
import model.Vertex;
import obliviousrouting.mcct.MCCTShortestPath;
import obliviousrouting.mcct.MCCTSolver;
import util.Pair;

/**
 * This class implements Racke's 2008 algorithm but it does not compute a single decomposition tree. 
 * A set of per-destination trees are computed at each iteration.
 * The relative load for each edge is computed by computing the oblivious performance of routing through that tree.
 * 
 * @author mosfet
 */
public class RackeObliviousRoutingSolverShortestPath {

	private Graph graph;
	private Map<Integer,Map<Integer,Map<Integer,Set<Arc>>>> idTree2destination2source2path;
	private Map<Integer,Map<Integer,Map<Integer,Double>>> idTree2destination2source2lambda;
	private Map<Integer,Double> numOfTrees2performance;
	private List<Graph> graphs;
	private Map<Integer,Map<Integer,Double>> idTree2arc2rLoad;
	private MCCTSolver mcct;

	public MCCTSolver getMcct() {
		return mcct;
	}
	public List<Graph> getGraphs() {
		return graphs;
	}
	public void setGraphs(List<Graph> graphs) {
		this.graphs = graphs;
	}
	public void setMcct(MCCTSolver mcct) {
		this.mcct = mcct;
	}
	public Graph getGraph() {
		return graph;
	}
	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	public RackeObliviousRoutingSolverShortestPath(){
		this.idTree2destination2source2lambda = new HashMap<Integer,Map<Integer,Map<Integer,Double>>>(); 
		this.idTree2destination2source2path = new HashMap<Integer,Map<Integer,Map<Integer,Set<Arc>>>>();
		this.graphs = new ArrayList<Graph>();
		this.idTree2arc2rLoad = new HashMap<Integer,Map<Integer,Double>>(); 
		this.numOfTrees2performance = new HashMap<Integer,Double>();
	}

	/**
	 * Computes trees and lambdas based on the graph that has been set 
	 */
	public void createTreesAndLambda(List<Pair<Integer>> setOfDemands){
		Map<Integer,Set<Integer>> destination2sources = transformSetOfDemandsToMap(setOfDemands);
		if(this.mcct==null){
			this.mcct = new MCCTShortestPath();
			mcct.setGraph(this.graph);
			for(Pair<Integer> demand : setOfDemands){
				mcct.addDemand(this.graph.getVertexById(demand.getFirst()), this.graph.getVertexById(demand.getSecond()),0d);
			}
		}
		
		// iterate until sum of lambdas is less than 1
		double lambdaSum = 0;
		int treeIndex = 0;
		while(lambdaSum<1){
			System.out.println("i:"+treeIndex); 
			lambdaSum+=iterate(treeIndex,lambdaSum,destination2sources);
			//System.out.println("Tree:"+treeIndex);
			//System.out.println(this.trees.get(treeIndex));
			//System.out.println("rLoad: "+this.idTree2arc2rLoad );
			//System.out.println("lambda:"+this.lambdas.get(treeIndex)+" sumOfLambdas:"+lambdaSum+"\n");
			treeIndex++;
		}
	} 

	/**
	 * Transform a set of demands into a map data structure.
	 * @param setOfDemands
	 * @return a map vertex:{set{vertex}}
	 */
	private Map<Integer, Set<Integer>> transformSetOfDemandsToMap(List<Pair<Integer>> setOfDemands) {
		Map<Integer,Set<Integer>> destination2sources = new HashMap<Integer,Set<Integer>>();
		for(Pair<Integer> demand: setOfDemands){
			Set<Integer> sources =destination2sources.get(demand.getSecond()); 
			if(sources==null){
				sources = new TreeSet<Integer>();
				destination2sources.put(demand.getSecond(), sources);
			}
			sources.add(demand.getFirst());
		}
		return destination2sources;
	}
	
	/**
	 * An iteration of the Racke's algorithm.
	 * 
	 * @param treeIndex, index of the current tree
	 * @param lambdaSum, sum of lambdas
	 * @param destination2sources,
	 * @return the coefficient lambda of the computed tree
	 */
	public double iterate(int treeIndex, double lambdaSum,Map<Integer, Set<Integer>> destination2sources) {
		Graph copyGraph = this.graph.createCopy();
		this.graphs.add(copyGraph);
		Map<Integer,Map<Integer,Set<Arc>>> destination2source2path = getTrees(copyGraph,destination2sources);
		mcct.reset();
		this.idTree2destination2source2path.put(treeIndex, destination2source2path);
		this.computeRLoads(treeIndex,destination2source2path);
		double l = getMaxRLoad(treeIndex);
		double delta = Math.min(1/l, 1 - lambdaSum);
		addDeltaToAllPahts(treeIndex,delta);
		return delta;
	}

	private void addDeltaToAllPahts(int treeIndex, double delta) {
		this.idTree2destination2source2lambda.put(treeIndex, new HashMap<Integer,Map<Integer,Double>>());
		for(Integer idDestination : this.idTree2destination2source2path.get(treeIndex).keySet()){
			this.idTree2destination2source2lambda.get(treeIndex).put(idDestination,new HashMap<Integer,Double>());
			for(Integer idSource : this.idTree2destination2source2path.get(treeIndex).get(idDestination).keySet()){
				this.idTree2destination2source2lambda.get(treeIndex).get(idDestination).put(idSource, delta);
			}
		}
	}
	
	private double getMaxRLoad(int treeIndex) {
		double max = Double.MIN_VALUE;
		for(Double rLoad : this.idTree2arc2rLoad.get(treeIndex).values()){
			if(max < rLoad)
				max = rLoad;
		}
		return max;
	}

	/**
	 * computes relative load 
	 * @param treeIndex, the index of the current iteration number
	 * @param destination2source2path, a map that contains the set of per-destination trees for 
	 */
	private void computeRLoads(int treeIndex, Map<Integer,Map<Integer,Set<Arc>>> destination2source2path) {
		
		RackeSolutionTransform rso = new RackeSolutionTransform();
		Map<Integer,Map<Pair<Integer>,Double>> solution=null;
		
		//Double newLambda= getLambdaByIdTree(treeIndex)/lambdaSum;
		solution = rso.addASetOfTreesPerDestination(this.getIdTree2destination2source2path().get(treeIndex), 1d);
		CompactLPSolverObliviousPerformance lpSolver = new CompactLPSolverObliviousPerformance ();
		lpSolver.computeObliviousPerformance(this.graph, solution);
		
		this.idTree2arc2rLoad.put(treeIndex, lpSolver.getArc2rLoad());
	}

	private Map<Integer,Map<Integer,Set<Arc>>> getTrees(Graph g, Map<Integer, Set<Integer>> destination2sources) {
		mcct.setGraph(g);

		this.computeNewDistances(g);

		System.out.println("Graph with new distances. "+g);
		return mcct.getTrees();
	}

	private void normalizeDistance(Graph g,Map<Arc,Double>  arc2newDistance) {
		double minDistance=Double.MAX_VALUE;
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				if(arc2newDistance.get(arc) < minDistance)
					minDistance = arc2newDistance.get(arc);

			}
		}
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				double newDistance = arc2newDistance.get(arc)/minDistance;
				if(newDistance < 1){
					newDistance =1;
				}
				arc.setDistance(newDistance);
			}
		}
	}

	private Map<Arc, Double> computeNewDistances(Graph g) {
		double totalRLoadAllEdges = this.getRloadAllEdges(g);
		//System.out.println("roadAllEdges="+totalRLoadAllEdges);
		Map<Arc,Double>  arc2newDistance= new HashMap<Arc,Double>();
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				double totalRLoad=0;
				for(int i=0; i < this.idTree2destination2source2path.keySet().size();i++){
					Double rLoad = this.idTree2arc2rLoad.get(i).get(arc.getId());
					if(rLoad == null)
						rLoad = 0d;
					//take a random lambda
					Integer dest = null;
					for(Integer idDest : this.idTree2destination2source2lambda.get(i).keySet()){
						dest=idDest;break;
					}
					Integer source = null;
					for(Integer idSource : this.idTree2destination2source2lambda.get(i).get(dest).keySet()){
						source=idSource;break;
					}
					totalRLoad += rLoad * this.idTree2destination2source2lambda.get(i).get(dest).get(source);
				}
				double num = Math.pow(Math.E, totalRLoad)/arc.getCapacity();
				double newDistance = num /totalRLoadAllEdges;
				arc2newDistance.put(arc, newDistance);
			}
		}
		this.normalizeDistance(g,arc2newDistance);
		return arc2newDistance;
	}

	private double getRloadAllEdges(Graph g) {
		double totalRLoadAllEdges = 0;
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				double totalPerEdge = 0d;
				for(int i=0; i < this.idTree2destination2source2path.keySet().size();i++){
					Double rLoad = this.idTree2arc2rLoad.get(i).get(arc.getId());
					if(rLoad == null)
						rLoad = 0d; 
					Integer dest = null;
					for(Integer idDest : this.idTree2destination2source2lambda.get(i).keySet()){
						dest=idDest;break;
					}
					Integer source = null;
					for(Integer idSource : this.idTree2destination2source2lambda.get(i).get(dest).keySet()){
						source=idSource;break;
					}
					totalPerEdge+= rLoad * this.idTree2destination2source2lambda.get(i).get(dest).get(source);
				}
				totalRLoadAllEdges += Math.pow(Math.E, totalPerEdge);
			}
		}
		return totalRLoadAllEdges;
	}
	public Map<Integer, Map<Integer, Map<Integer, Set<Arc>>>> getIdTree2destination2source2path() {
		return idTree2destination2source2path;
	}
	public Map<Integer, Map<Integer, Map<Integer, Double>>> getIdTree2destination2source2lambda() {
		return idTree2destination2source2lambda;
	}

	public Double getLambdaByIdTree(int idTree){
		if(this.idTree2destination2source2lambda.get(idTree)==null)
			return null;
		else{
			Integer dest = null;
			for(Integer idDest : this.idTree2destination2source2lambda.get(idTree).keySet()){
				dest=idDest;break;
			}
			Integer source = null;
			for(Integer idSource : this.idTree2destination2source2lambda.get(idTree).get(dest).keySet()){
				source=idSource;break;
			}
			return this.idTree2destination2source2lambda.get(idTree).get(dest).get(source);
		}
	}
	public Map<Integer, Double> getNumOfTrees2performance() {
		return numOfTrees2performance;
	}
	
	/*private void setRequirements(Graph g) {
		//set requirements equal to capacities
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				Vertex vertex2 = arc.getSecondEndPoint();
				mcct.addDemand(vertex, vertex2, arc.getCapacity());
			}
		}

	}*/

}