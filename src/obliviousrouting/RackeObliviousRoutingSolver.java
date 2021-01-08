package obliviousrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.Arc;
import model.Graph;
import model.Vertex;
import obliviousrouting.mcct.MCCTDerandomizedWeightedSolver;
import obliviousrouting.mcct.MCCTSolver;
import obliviousrouting.mcct.Node;
import obliviousrouting.mcct.Tree;


/**
 * This class implements Racke's algorithm as explained in his STOC 2008 paper.
 * 
 * @author mosfet
 */
public class RackeObliviousRoutingSolver {

	private Graph graph;
	private List<Tree> trees;
	private List<Double> lambdas;
	private List<Graph> graphs;

	private Map<Integer,Map<Arc,Double>> idTree2arc2rLoad;
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
	public List<Tree> getTrees() {
		return trees;
	}
	public void setTrees(List<Tree> trees) {
		this.trees = trees;
	}
	public List<Double> getLambda() {
		return lambdas;
	}
	public void setLambda(List<Double> lambda) {
		this.lambdas = lambda;
	}
	public Graph getGraph() {
		return graph;
	}
	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	public RackeObliviousRoutingSolver(){
		this.trees = new ArrayList<Tree>();
		this.lambdas = new ArrayList<Double>();
		this.graphs = new ArrayList<Graph>();
		this.idTree2arc2rLoad = new HashMap<Integer,Map<Arc,Double>>(); 
	}

	/**
	 * Computes trees and lambdas based on the graph that has been set 
	 */
	public void createTreesAndLambda(){
		if(this.mcct==null){
			this.mcct = new MCCTDerandomizedWeightedSolver();
			mcct.setGraph(this.graph);
		}
		double lambdaSum = 0;

		// iterate until sum of lambdas is less than 1
		int treeIndex = 0;
		while(lambdaSum<1){
			System.out.println("i:"+treeIndex); 
			lambdaSum+=iterate(treeIndex,lambdaSum);
			System.out.println("Tree:"+treeIndex);
		    System.out.println(this.trees.get(treeIndex));
			System.out.println("rLoad: "+this.idTree2arc2rLoad );
			System.out.println("lambda:"+this.lambdas.get(treeIndex)+" sumOfLambdas:"+lambdaSum+"\n");
			treeIndex++;
		}
	} 

	private double iterate(int treeIndex, double lambdaSum) {
		// create a copy of the graph 
		Graph copyGraph = this.graph.createCopy();
		//compute new distances and get a decomposition tree
		Tree t = getTree(copyGraph);
		mcct.reset();
		this.trees.add(t);
		// compute the relative load of the computed tree
		this.computeRLoads(treeIndex,t,copyGraph);
		double l = getMaxRLoad(treeIndex,t);
		double delta = Math.min(1/l, 1 - lambdaSum);
		this.lambdas.add(delta);
		this.graphs.add(copyGraph);
		return delta;
		
	}
	
	private double getMaxRLoad(int treeIndex, Tree t) {
		double max = Double.MIN_VALUE;
		for(Double rLoad : this.idTree2arc2rLoad.get(treeIndex).values()){
			if(max < rLoad)
				max = rLoad;
		}
		return max;
	}
	
	private void computeRLoads(int treeIndex, Tree t,Graph copyGraph) {
		// BSF tree visit
		List<Node> queue = new LinkedList<Node>(t.getRoot().getChildren());
		Map<Arc,Double> arc2rLoad = new HashMap<Arc,Double>();
		this.idTree2arc2rLoad.put(treeIndex, arc2rLoad);
		while(!queue.isEmpty()){
			Node node = queue.remove(0);
			Node parent = node.getParent();
			
			// for each tree edge (u,v), with v parent
			//	let V and U be a partition of v's vertices
			if(node.getCenter().equals(parent.getCenter())){
				queue.addAll(node.getChildren());
				continue;
			}
			List<Vertex> verticesNode = node.getVertices();
			List<Vertex> remainingVertices = new LinkedList<Vertex>(copyGraph.getVertices());
			for(Vertex vertex : verticesNode)
				remainingVertices.remove(vertex);
			
			// compute cut between V and U
			//System.out.println("ver(node):" + verticesNode + " ver(parent):" + remainingVertices);
			double cut = 0;
			for(Vertex vertex: verticesNode){
				for(Arc arc:vertex.getArcs()){
					Vertex vertex2 = arc.getFirstEndPoint();
					if(vertex2==vertex)
						vertex2=arc.getSecondEndPoint();
					if(remainingVertices.contains(vertex2))
						cut+=arc.getCapacity();
				}
			}
			//System.out.println("cut-size(node,parent):" + cut);
			
			//  add to each edge in the shortest path the cut and divide by the capacity of the edge
			Vertex centerParent = parent.getCenter();
			Vertex currentVertex = node.getCenter();;
			for(Arc arc : copyGraph.getShortestPath(currentVertex, centerParent)){
				Double rLoad = arc2rLoad.get(arc);
				Arc reversedArc = copyGraph.getReversedArc(arc.getId());
				if(rLoad == null)
					rLoad = 0d;
				rLoad+= cut/arc.getCapacity();
				arc2rLoad.put(arc, rLoad);
				arc2rLoad.put(reversedArc, rLoad);
			}
			queue.addAll(node.getChildren());
		}
	}
	
	private Tree getTree(Graph g) {
		mcct.setGraph(g);

		//System.out.println(g);
		this.setRequirements(g);		

		this.computeNewDistances(g);
		
		System.out.println("Graph with new distances. "+g);
		return mcct.getBestTree();
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
		System.out.print("totalRLoad:(");
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				double totalRLoad=0;
				for(int i=0; i < this.trees.size();i++){
					Double rLoad = this.idTree2arc2rLoad.get(i).get(arc);
					if(rLoad == null)
						rLoad = 0d;
					totalRLoad += rLoad * this.lambdas.get(i);
				}
				System.out.print(totalRLoad+ " ");
				double num = Math.pow(Math.E, totalRLoad)/arc.getCapacity();
				double newDistance = num /totalRLoadAllEdges;
				arc2newDistance.put(arc, newDistance);
			}
		}
		System.out.println();
		this.normalizeDistance(g,arc2newDistance);
		return arc2newDistance;
	}
	
	private double getRloadAllEdges(Graph g) {
		double totalRLoadAllEdges = 0;
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				double totalPerEdge = 0d;
				for(int i=0; i < this.trees.size();i++){
					Double rLoad = this.idTree2arc2rLoad.get(i).get(arc);
					if(rLoad == null)
						rLoad = 0d; 
					totalPerEdge+= rLoad * this.lambdas.get(i);
				}
				totalRLoadAllEdges += Math.pow(Math.E, totalPerEdge);
			}
		}
		return totalRLoadAllEdges;
	}
	
	private void setRequirements(Graph g) {
		//set requirements equal to capacities
		for(Vertex vertex: g.getVertices()){
			for(Arc arc: vertex.getArcs()){
				Vertex vertex2 = arc.getSecondEndPoint();
				mcct.addDemand(vertex, vertex2, arc.getCapacity());
			}
		}

	}




}
