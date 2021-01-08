package obliviousrouting.mcct;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

/**
 * This class implements the derandomized version of the algorithm 
 * presented by Fakcharoenphol Rao Talwar that is used to compute
 * a decomposition tree such that the cost of routing a set of demands
 * through that tree is only a factor lg(n) away from the optimal
 * solution.
 * 
 * @author mosfet
 *
 */

public class MCCTDerandomizedWeightedSolver implements MCCTSolver{

	private Tree tree;
	//private Map<Pair<Vertex>,List<Arc>> treeEdge2path;
	private Graph graph;
	private Set<Double> betas;
	private double bestB;
	private List<Vertex> verticesPermutation;
	private Map<Integer, Map<Integer, Double>> idVertex2idVertex2demand; // set of demands
	private Map<Pair<Integer>,Integer> demand2levelIncluded;

	public MCCTDerandomizedWeightedSolver() {
		this.betas = new TreeSet<Double>();
		this.verticesPermutation = new LinkedList<Vertex>();
		this.idVertex2idVertex2demand = new HashMap<Integer, Map<Integer, Double>>();
		this.demand2levelIncluded = new HashMap<Pair<Integer>,Integer>();
	}


	public void addDemand(Vertex v1, Vertex v2, Double demand){
		Map<Integer, Double> idVertex2Demand = this.idVertex2idVertex2demand.get(v1.getId());
		if(idVertex2Demand == null){
			idVertex2Demand = new HashMap<Integer, Double>();
			this.idVertex2idVertex2demand.put(v1.getId(), idVertex2Demand);
		}
		idVertex2Demand.put(v2.getId(), demand);
	}



	public Tree getTree() {
		return tree;
	}

	public void setTree(Tree tree) {
		this.tree = tree;
	}

	public Graph getGraph() {
		return graph;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	public Set<Double> getBetas() {
		return betas;
	}

	public void setBetas(Set<Double> betas) {
		this.betas = betas;
	}



	public double getBestB() {
		return bestB;
	}

	public void setBestB(double bestB) {
		this.bestB = bestB;
	}

	public List<Vertex> getVerticesPermutation() {
		return verticesPermutation;
	}

	public void setVerticesPermutation(List<Vertex> verticesPermutation) {
		this.verticesPermutation = verticesPermutation;
	}

	/**
	 * Computes the best beta and the best permutation of the vertices
	 * using a derandomized technique.
	 * This is a required step in order to compute a decomposition tree.
	 */
	public void findBestBetaAndPermutation() {

		this.graph.createDistanceMatrix();
		this.computeBetas();
		//System.out.println("betas:"+this.betas);
		//Map<Pair<Integer>,Map<Integer,List<Vertex>>> demand2level2verticesThatIncludeEndpoints = new HashMap<Pair<Integer>,Map<Integer,List<Vertex>>>();
		//Map<Pair<Integer>,Map<Integer,List<Vertex>>> demand2level2verticesThatSeparatesEndpoints = new HashMap<Pair<Integer>,Map<Integer,List<Vertex>>>();

		// compute best beta
		Double minExpectation = Double.MAX_VALUE;
		//System.out.println("betas-size:"+this.betas.size());
		Set<Vertex> unsettledVertices = new TreeSet<Vertex>(this.graph.getVertices());
		for(Double beta : this.betas){
			//this.computeDemand2level2verticesThatXEndpoints(demand2level2verticesThatIncludeEndpoints,demand2level2verticesThatSeparatesEndpoints,this.graph.getVertices(),beta);
			//System.out.println("-- candidate beta:"+beta);
			Double expectedCost = this.computeExpectation(beta,this.verticesPermutation,unsettledVertices);
			//System.out.println("-- expected cost:"+expectedCost + " mincost:"+minExpectation);
			if(expectedCost <minExpectation){
				this.bestB= beta;
				minExpectation = expectedCost;
				//System.out.println("-- new best beta:"+this.bestB);
			}
		}
		
		// compute best permutation of the vertices
		while(this.verticesPermutation.size()<graph.getVertices().size()){
			Vertex bestV = null;
			minExpectation = Double.MAX_VALUE;
			List<Vertex> candidatePermutation = new LinkedList<Vertex>(this.verticesPermutation); 
			for(Vertex candidateVertex : unsettledVertices){
				candidatePermutation.add(candidateVertex);
				//System.out.println("-- candidate vertex:"+candidateVertex);
				Double expectedCost = this.computeExpectation(this.bestB,candidatePermutation,unsettledVertices);
				//System.out.println("-- expected cost:"+expectedCost + " mincost:"+minExpectation);
				if(expectedCost <=minExpectation){
					bestV= candidateVertex;
					minExpectation = expectedCost;
				}
				candidatePermutation.remove(candidateVertex);
			}
			this.verticesPermutation.add(bestV);
			unsettledVertices.remove(bestV);
			this.removeDemands(this.bestB,bestV);
		}
	}

	/*
	 * remove demands whose cost is already settled.
	 */
	private void removeDemands(double beta, Vertex bestV) {
		Map<Integer,List<Integer>> idVertex2vertices = new HashMap<Integer,List<Integer>>(); 
		Vertex center = bestV;
		Map<Integer,Double> idVertex2distance = this.graph.getIdVertex2idVertex2distance().get(center.getId());
		for(Integer idVertex : this.idVertex2idVertex2demand.keySet()){
			Map<Integer,Double> idVertex2Demand = this.idVertex2idVertex2demand.get(idVertex);
			List<Integer> vertices= idVertex2vertices.get(idVertex);
			for(Integer idVertex2 : idVertex2Demand .keySet()){
				//System.out.println("v1:"+idVertex+ " v2:"+idVertex2+" dem:"+demand);
				double firstDistance = idVertex2distance.get(idVertex);
				double secondDistance = idVertex2distance.get(idVertex2);
				double bigger = Math.max(firstDistance, secondDistance);
				double smaller= Math.min(firstDistance, secondDistance);
				bigger/=beta;
				smaller/=beta;
				int powerBigger = (int)Math.floor(Math.log(bigger)/Math.log(2))+2;
				int powerSmaller = (int)Math.floor(Math.log(smaller)/Math.log(2))+2;
				//System.out.println("c:"+center+" d(c,v1):"+firstDistance+" d(c,v2):"+secondDistance+" bigger:"+bigger+" smaller:"+smaller+" powerB:"+powerBigger+" powerS:"+powerSmaller);
				if(powerSmaller!=powerBigger){
					if(vertices==null){
						vertices = new LinkedList<Integer>();
						idVertex2vertices.put(idVertex, vertices);
					}
					vertices.add(idVertex2);
					//System.out.println("center "+center + " separates " + idVertex + " and " + idVertex2 + " at level " + (powerBigger-1) + " and cost is " + " 2^"+(powerBigger+2)+"*"+demand+"="+Math.pow(2, powerBigger+2)*demand);
				}
			}
		}

		//remove demands
		for(Integer idVertex: idVertex2vertices.keySet()){
			for(Integer idVertex2 : idVertex2vertices.get(idVertex)){
				this.idVertex2idVertex2demand.get(idVertex).remove(idVertex2);
				if(this.idVertex2idVertex2demand.get(idVertex).size()==0){
					this.idVertex2idVertex2demand.remove(idVertex);
				}
			}
		}

	}

	/*
	 * compute the expected cost of the decomposition tree given a certain beta 
	 * and a prefix 'verticesPermutation' of the vertices.
	 */
	private Double computeExpectation(Double beta,
			List<Vertex> verticesPermutation, Set<Vertex> unsettledVertices){//, Map<Pair<Integer>, Map<Integer, List<Vertex>>> demand2level2verticesThatIncludeEndpoints, Map<Pair<Integer>, Map<Integer, List<Vertex>>> demand2level2verticesThatSeparatesEndpoints) {

		Double result = 0d;
		//compute fixed costs
		List<Pair<Integer>> cutAndSettled = new LinkedList<Pair<Integer>>();
		if(!verticesPermutation.isEmpty()){
			Vertex center = verticesPermutation.get(verticesPermutation.size()-1);
			Map<Integer,Double> idVertex2distance = this.graph.getIdVertex2idVertex2distance().get(center.getId());
			for(Integer idVertex : this.idVertex2idVertex2demand.keySet()){
				Map<Integer,Double> idVertex2Demand = this.idVertex2idVertex2demand.get(idVertex);
				for(Integer idVertex2 : idVertex2Demand .keySet()){
					Double demand = idVertex2Demand.get(idVertex2);
					//System.out.println("v1:"+idVertex+ " v2:"+idVertex2+" dem:"+demand);
					double firstDistance = idVertex2distance.get(idVertex);
					double secondDistance = idVertex2distance.get(idVertex2);
					double bigger = Math.max(firstDistance, secondDistance);
					double smaller= Math.min(firstDistance, secondDistance);
					bigger/=beta;
					smaller/=beta;
					int powerBigger = (int)Math.floor(Math.log(bigger)/Math.log(2))+2;
					int powerSmaller = (int)Math.floor(Math.log(smaller)/Math.log(2))+2;
					//System.out.println("c:"+center+" d(c,v1):"+firstDistance+" d(c,v2):"+secondDistance+" bigger:"+bigger+" smaller:"+smaller+" powerB:"+powerBigger+" powerS:"+powerSmaller);
					if(powerSmaller!=powerBigger){
						//System.out.println("center "+center + " separates " + idVertex + " and " + idVertex2 + " at level " + (powerBigger-1) + " and cost is " + " 2^"+(powerBigger+2)+"*"+demand+"="+Math.pow(2, powerBigger+2)*demand);
						result+=Math.pow(2, powerBigger+2)*demand;
						cutAndSettled.add(new Pair<Integer>(idVertex,idVertex2));
					}else{
						Integer level = this.demand2levelIncluded.get(demand);
						if(level==null)
							level=Integer.MAX_VALUE;
						if(powerBigger<level)
							this.demand2levelIncluded.put(new Pair<Integer>(idVertex,idVertex2), (Integer)powerBigger);
					}
				}
			}
			//System.out.println("candidate vertex "+ center + " settled "+ cutAndSettled);
		}

		//compute expected costs
		for(Integer idVertex : this.idVertex2idVertex2demand.keySet()){
			Map<Integer,Double> idVertex2Demand = this.idVertex2idVertex2demand.get(idVertex);
			for(Integer idVertex2 : idVertex2Demand .keySet()){
				Pair<Integer> pairDemand = new Pair<Integer>(idVertex,idVertex2);
				if(cutAndSettled.contains(new Pair<Integer>(idVertex,idVertex2)))
					continue;
				Double demand = idVertex2Demand.get(idVertex2);
				//System.out.println("v1:"+idVertex+ "v2:"+idVertex2+" dem:"+demand);
				Map<Integer,Double> center2Cost = new HashMap<Integer,Double>();
				for(Vertex center : unsettledVertices){
					if(!verticesPermutation.isEmpty() && verticesPermutation.get(verticesPermutation.size()-1)==center)
						continue;

					double firstDistance = this.graph.getIdVertex2idVertex2distance().get(center.getId()).get(idVertex);
					double secondDistance = this.graph.getIdVertex2idVertex2distance().get(center.getId()).get(idVertex2);
					double bigger = Math.max(firstDistance, secondDistance);
					double smaller= Math.min(firstDistance, secondDistance);
					bigger/=beta;
					smaller/=beta;
					int powerBigger = (int)Math.floor(Math.log(bigger)/Math.log(2))+2;
					int powerSmaller = (int)Math.floor(Math.log(smaller)/Math.log(2))+2;
					//System.out.println("c:"+center+" d(c,v1):"+firstDistance+" d(c,v2):"+secondDistance+" bigger:"+bigger+" smaller:"+smaller+" powerB:"+powerBigger+" powerS:"+powerSmaller);
					if(powerSmaller!=powerBigger){
						if(powerSmaller==Integer.MIN_VALUE+2){
							powerSmaller = 0;
						}
						for(int i=powerBigger;i>powerSmaller;i--){
							//System.out.println("I:"+i);
							this.demand2levelIncluded.get(pairDemand);
							if(this.demand2levelIncluded.get(pairDemand)!=null && this.demand2levelIncluded.get(pairDemand)<i){
								//System.out.println("center "+center + " separates " + idVertex + " and " + idVertex2 + " at level" + (powerBigger-1) + " and cost is " + " 2^"+(powerBigger+2)+"*"+demand+"="+Math.pow(2, powerBigger+2)*demand);
								center2Cost.put(center.getId(), Math.pow(2, i+2)*demand);
								break;
							}
						}
					}
				}
				for(Double cost : center2Cost.values()){
					//System.out.println("result:"+result+ " add:"+cost/center2Cost.size()+" result:"+(result+cost/center2Cost.size()));
					result+=cost/center2Cost.size();
				}
			}
		}

		return result;
	}

	/**
	 * Construct a decomposition tree once the best beta and the best 
	 * permutation of the vertices has been computed.
	 */
	public void computeBestTree(){

		if(this.graph.getIdVertex2idVertex2distance()==null)
			this.graph.createDistanceMatrix();
		//construct the best tree
		this.tree = new Tree();
		double diameter = this.graph.getGraphDiameter();
		if(diameter ==0)
			diameter =1;
		int i= (int) Math.ceil((Math.log(diameter) / Math.log(2)))+1;
		//System.out.println("i:"+i+ " diameter:"+diameter);
		this.tree.createRoot(i);
		this.tree.getRoot().addVertices(this.graph.getVertices());
		this.tree.getRoot().setCenter(this.verticesPermutation.get(0));
		i--;
		//while (!this.tree.isLayerWithAllSingletons(i)) {
		while (i>=0) {
			double betaI = Math.pow(2, i-1 ) * this.bestB;
			//System.out.println("beta("+(i)+"):"+betaI);
			Set<Integer> assignedVerticesId = new TreeSet<Integer>();
			for (Vertex vertex: this.verticesPermutation) {
				//if (assignedVerticesId.contains(vertex.getId()))
				//	continue;
				for (Node node : this.tree.getLayerNodes(i+1)) {
					Node childNode = new Node();
					for (Vertex vertexInCluster : node.getVertices()) {
						if (assignedVerticesId
								.contains(vertexInCluster.getId()))
							continue;
						if (this.graph.getIdVertex2idVertex2distance().get(vertex.getId())
								.get(vertexInCluster.getId()) < betaI) {
							childNode.addVertex(vertexInCluster);
							assignedVerticesId.add(vertexInCluster.getId());
						}
					}
					if (childNode.getVertices().size() > 0){
						node.addChild(childNode);
						childNode.setParent(node);
						childNode.setCenter(vertex);
						this.tree.addNodeToLayer(i,childNode);
						//List<Arc> path = new LinkedList<Arc>();

						//this.treeEdge2path.put(new Pair<Vertex>(node.getCenter(), childNode.getCenter()),path);
					}
				}
			}
			assignedVerticesId.clear();
			//System.out.println("------- i:"+i+" -------\n"+this.tree);
			i--;

		}
	}

	@Override
	public Tree getBestTree(){
		this.findBestBetaAndPermutation();
		this.computeBestTree();
		return this.tree;
	}

	/*
	 * computes the set of all possible meaningful betas.
	 */
	private void computeBetas() {
		List<Vertex> toBeAnalyzed = new LinkedList<Vertex>(graph.getVertices());
		for(Vertex vertex : graph.getVertices()){
			toBeAnalyzed.remove(0);
			for(Vertex vertex2:toBeAnalyzed){
				int power = (int)Math.floor(Math.log(this.graph.getIdVertex2idVertex2distance().get(vertex.getId()).get(vertex2.getId()))/Math.log(2));
				Double beta = this.graph.getIdVertex2idVertex2distance().get(vertex.getId()).get(vertex2.getId())/Math.pow(2, power);
				if(beta==1d)
					beta=2d;
				this.betas.add(beta);
			}
		}
	}


	
	@Override
	public void reset() {
		this.betas = new TreeSet<Double>();
		this.verticesPermutation = new LinkedList<Vertex>();
		this.idVertex2idVertex2demand = new HashMap<Integer, Map<Integer, Double>>();
		this.tree=null;
		this.graph=null;
	}

	@Override
	public Map<Integer, Map<Integer, Set<Arc>>> getTrees() {
		// TODO Auto-generated method stub
		return null;
	}


}
