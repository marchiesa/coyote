package inputoutput;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import obliviousrouting.mcct.Node;
import obliviousrouting.mcct.Tree;
import util.Pair;

/**
 * This class is responsible of computing a routing given a set of trees. 
 * There are two main methods:
 *  - addASetOfTreesPerDestination, which can be used to add per-destination trees, and
 *  - addATree, which can be used to add a decomposition tree
 * @author mosfet
 *
 */
public class RackeSolutionTransform {

	private List<Set<Arc>> decTree2notSpanningTrees;
	private Map<Integer,Map<Pair<Integer>,Double>> arc2demand2cumulativeFraction;

	public RackeSolutionTransform(){
		this.decTree2notSpanningTrees = new LinkedList<Set<Arc>>();
		this.arc2demand2cumulativeFraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
	}

	/**
	 *  
	 * @param destination2source2path, is a per-destination set of trees
	 * @param lambda, is the lambda coefficient of the above tree
	 * @param graph, is the original graph.
	 * @return a new routing based on the newly added tree
	 */
	public Map<Integer,Map<Pair<Integer>,Double>> addASetOfTreesPerDestination(Map<Integer,Map<Integer,Set<Arc>>> destination2source2path, Double lambda){

		this.normalizeOldSolutionBasedOnNewLambda(lambda);

		for(Integer idDest : destination2source2path.keySet()){
			for(Integer idSource : destination2source2path.get(idDest).keySet()){
				Pair<Integer> demand = new Pair<Integer>(idSource,idDest);
				for(Arc arc : destination2source2path.get(idDest).get(idSource)){
					Map<Pair<Integer>,Double> demand2fraction = this.arc2demand2cumulativeFraction.get(arc.getId());
					if(demand2fraction==null){
						demand2fraction = new HashMap<Pair<Integer>,Double>();
						this.arc2demand2cumulativeFraction.put(arc.getId(), demand2fraction);
					}
					Double fraction = demand2fraction.get(demand);
					if(fraction==null)
						fraction = 0d;
					this.arc2demand2cumulativeFraction.get(arc.getId()).put(demand,fraction+lambda);
				}
			}
		}

		return this.arc2demand2cumulativeFraction;
	}


	public void removeCycles(Graph g){
		Set<Pair<Integer>> setOfDemands = new TreeSet<Pair<Integer>>();
		for(Map<Pair<Integer>,Double> demands2fraction:arc2demand2cumulativeFraction.values()){
			setOfDemands.addAll(demands2fraction.keySet());
		}


		for(Pair<Integer> demand:setOfDemands){

			List<Arc> cycle = this.findCycle(arc2demand2cumulativeFraction,demand,g);
			while(cycle!=null){
				double min = Double.MAX_VALUE;
				for(Arc arc: cycle){
					Double fraction = arc2demand2cumulativeFraction.get(arc.getId()).get(demand);
					if(fraction < min){
						min = fraction;
					}
				}

				Pair<Integer> reversedDemand = new Pair<Integer>(demand.getSecond(),demand.getFirst());
				for(Arc arc: cycle){
					if( arc2demand2cumulativeFraction.get(arc.getId())!=null){
						Double fraction = arc2demand2cumulativeFraction.get(arc.getId()).get(demand);
						if(fraction-min==0){
							arc2demand2cumulativeFraction.get(arc.getId()).remove(demand);
							arc2demand2cumulativeFraction.get(g.getReversedArc(arc.getId()).getId()).remove(reversedDemand);
							if(arc2demand2cumulativeFraction.get(arc.getId()).size()==0){
								arc2demand2cumulativeFraction.remove(arc.getId());
								arc2demand2cumulativeFraction.remove(g.getReversedArc(arc.getId()).getId());
							}
						}else{
							arc2demand2cumulativeFraction.get(arc.getId()).put(demand, fraction-min);
							arc2demand2cumulativeFraction.get(g.getReversedArc(arc.getId()).getId()).put(reversedDemand, fraction-min);
						}
					}
				}
				cycle = this.findCycle(arc2demand2cumulativeFraction,demand,g);
			}
		}
	}

	private List<Arc> findCycle(Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction,Pair<Integer> demand, Graph g) {
		List<Vertex> remainingVertices = new LinkedList<Vertex>(g.getVertices());
		List<Arc> cycle = null;
		while(remainingVertices.size()>0 && cycle==null){
			Vertex v = remainingVertices.get(0);
			cycle = this.findCycleRic(v,new LinkedList<Integer>(),new LinkedList<Integer>(),arc2demand2fraction,demand,remainingVertices);
		}
		return cycle;
	}

	private List<Arc> findCycleRic(Vertex v, LinkedList<Integer> analyzed,LinkedList<Integer> currentPath,
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction,Pair<Integer> demand, Collection<Vertex> remainingVertices) {
		List<Arc> path = null;
		if(currentPath.contains(v.getId())){
			return new LinkedList<Arc>();
		}else if(!analyzed.contains(v.getId())){
			analyzed.add(v.getId());
			remainingVertices.remove(v);
			currentPath.add(v.getId());
			for(Arc arc:v.getArcs()){
				if(arc2demand2fraction.get(arc.getId())!=null && arc2demand2fraction.get(arc.getId()).get(demand) != null){
					path = findCycleRic(arc.getSecondEndPoint(),analyzed,currentPath,arc2demand2fraction,demand,remainingVertices);
					if(path!=null){
						if(path.size() ==0 || !path.get(0).getFirstEndPoint().equals(path.get(path.size()-1).getSecondEndPoint())){
							path.add(0, arc);
						}
						return path;
					}
				}
			}
			currentPath.remove((Integer)v.getId());
		}
		return path;
	}

	public List<Set<Arc>> getDecTree2notSpanningTrees() {
		return decTree2notSpanningTrees;
	}

	public void setDecTree2notSpanningTrees(List<Set<Arc>> decTree2notSpanningTrees) {
		this.decTree2notSpanningTrees = decTree2notSpanningTrees;
	}

	/**
	 * 
	 * @param t, which is a per-destination set of trees
	 * @param lambda, which is the lambda coefficient of the above tree
	 * @param graph, which is the original graph
	 * @return a new routing based on the newly added tree
	 */
	public Map<Integer,Map<Pair<Integer>,Double>> addATree(Tree t, Double lambda,Graph graph){

		normalizeOldSolutionBasedOnNewLambda(lambda);

		Set<Arc> tree = new TreeSet<Arc>();
		this.decTree2notSpanningTrees.add(tree);
		//System.out.println("Dec.Tree: "+i);
		List<Node> queue = new LinkedList<Node>(t.getRoot().getChildren());
		
		// BFS visit of the decomposition tree 
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
			List<Vertex> remainingVertices = new LinkedList<Vertex>(graph.getVertices());
			for(Vertex vertex : verticesNode)
				remainingVertices.remove(vertex);
			//System.out.println("ver(node):" + verticesNode + " ver(parent):" + remainingVertices);
			Vertex toVertex = parent.getCenter();
			Vertex fromVertex = node.getCenter();;
			//System.out.println("toVertex:" + toVertex + " fromVertex:" + fromVertex);
			//System.out.println(graphs.get(i).getShortestPath(fromVertex, toVertex));

			// for each demand (vertex, vertex2) 
			for(Vertex vertex : verticesNode){
				for(Vertex vertex2 : remainingVertices){
					Pair<Integer> pair = new Pair<Integer>(vertex.getId(),vertex2.getId());
					Pair<Integer> reversedPair = new Pair<Integer>(vertex2.getId(),vertex.getId());
					//System.out.println("cut-size(node,parent):");
					//System.out.println( vertex +"-"+vertex2+" --> sp("+fromVertex+","+toVertex+"): "+graphs.get(i).getShortestPath(fromVertex, toVertex));

					// increase the relative load on each edge along the shortest path
					for(Arc arc : graph.getShortestPath(fromVertex, toVertex)){
						tree.add(arc); 
						Map<Pair<Integer>,Double> demand2cumulativeFraction = arc2demand2cumulativeFraction.get(arc.getId());
						//System.out.println("g.getReversedArc(arc.getId())="+g.getReversedArc(arc.getId()));
						Map<Pair<Integer>,Double> reversedDemand2fraction = arc2demand2cumulativeFraction.get(graph.getReversedArc(arc.getId()).getId());
						if(demand2cumulativeFraction==null){
							demand2cumulativeFraction = new HashMap<Pair<Integer>,Double>();
							arc2demand2cumulativeFraction.put(arc.getId(), demand2cumulativeFraction);
							reversedDemand2fraction = new HashMap<Pair<Integer>,Double>();
							arc2demand2cumulativeFraction.put(graph.getReversedArc(arc.getId()).getId(), reversedDemand2fraction);
						}
						Double amount = demand2cumulativeFraction.get(pair);
						if(amount == null)
							amount = 0d;
						demand2cumulativeFraction.put(pair, amount +lambda);
						reversedDemand2fraction.put(reversedPair, amount+lambda);
					}
				}
			}
			queue.addAll(node.getChildren());
		}
		//System.out.println("Tree["+i+"]:"+tree + "\n");
		//System.out.println("Graph: "+graphs.get(i));
		this.removeCycles(graph);
		return arc2demand2cumulativeFraction;
	}

	private void normalizeOldSolutionBasedOnNewLambda(Double lambda) {

		for (Integer idArc : this.arc2demand2cumulativeFraction.keySet()){
			Map<Pair<Integer>,Double> demand2fraction  = this.arc2demand2cumulativeFraction.get(idArc);
			for(Pair<Integer> demand : demand2fraction.keySet())
				demand2fraction.put(demand, demand2fraction.get(demand)*(1-lambda));
		}
	}


}
