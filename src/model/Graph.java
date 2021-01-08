package model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class model a Graph structure.
 * 
 * It contains several maps:
 *  - id2vertex, which maps vertex ids to Vertex objects
 *  - id2arc, which maps arc ids to Arc objects
 *  - idVertex2idVertex2distance, which stores distances between vertices
 *  - idVertex2idVertex2nextArc, which stores the the next-hop Arc object on the path from a vertex to another one.
 *  
 * @author mosfet
 *
 */
public class Graph implements Comparable<Graph>, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int UNDIRECTED = 0;
	public static final int DIRECTED = 1;

	private int type = Graph.UNDIRECTED;
	private Map<Integer,Vertex> id2vertex;
	private Map<Integer, Map<Integer, Double>> idVertex2idVertex2distance;
	private Map<Integer, Map<Integer, Arc>> idVertex2idVertex2nextArc;
	private Map<Integer, Map<Integer, Set<Arc>>> idVertex2idVertex2allNextArc;
	private Map<Integer,Arc> id2arc;
	private int idArc = 0;

	public Graph(){
		this.id2vertex = new HashMap<Integer,Vertex>();
		this.id2arc = new HashMap<Integer,Arc>();
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void removeArcById(Integer id){
		Arc arc = this.id2arc.remove(id);
		arc.getFirstEndPoint().removeArc(id);
	}

	public Map<Integer, Map<Integer, Double>> getIdVertex2idVertex2distance() {
		return idVertex2idVertex2distance;
	}

	public void setIdVertex2idVertex2distance(
			Map<Integer, Map<Integer, Double>> idVertex2idVertex2distance) {
		this.idVertex2idVertex2distance = idVertex2idVertex2distance;
	}

	public Map<Integer, Map<Integer, Arc>> getIdVertex2idVertex2nextArc() {
		return idVertex2idVertex2nextArc;
	}

	public void setIdVertex2idVertex2nextArc(
			Map<Integer, Map<Integer, Arc>> idVertex2idVertex2nextArc) {
		this.idVertex2idVertex2nextArc = idVertex2idVertex2nextArc;
	}

	public Map<Integer, Vertex> getId2vertex() {
		return id2vertex;
	}

	public void setId2vertex(Map<Integer, Vertex> id2vertex) {
		this.id2vertex = id2vertex;
	}

	public Vertex getVertexById(int id){
		return this.id2vertex.get(id);
	}

	public void addVertex(Vertex vertex){
		this.id2vertex.put(vertex.getId(), vertex);
	}

	public Collection<Vertex> getVertices(){
		return this.id2vertex.values();
	}

	public Graph createCopy(){
		Graph copyGraph = new Graph();
		copyGraph.setType(this.type);
		for(Vertex vertex : this.id2vertex.values()){
			Vertex copyVertex = vertex.createCopy();
			copyGraph.addVertex(copyVertex);
		}

		for(Vertex vertex : this.id2vertex.values()){
			for(Arc arc : vertex.getArcs()){
				Arc copyArc = arc.createCopy();
				copyArc.setFirstEndPoint(copyGraph.getVertexById(arc.getFirstEndPoint().getId()));
				copyArc.setSecondEndPoint(copyGraph.getVertexById(arc.getSecondEndPoint().getId()));
				copyGraph.addDirectedArc(copyArc);
			}
		}
		return copyGraph;
	}


	public Arc getArbitraryArc(){
		for(Vertex vertex : this.id2vertex.values())
			for(Arc arc : vertex.getArcs())
				return arc;
		return null;
	}

	public String toString(){
		String graph = "graph{\n";
		for(Vertex v : this.getVertices()){
			graph+="\t"+v.toStringVerbose()+"\n";
		}
		graph+="}";
		return graph;
	}

	public void addUndirectedArc(Arc arc) {
		arc.setId(idArc++);
		arc.getFirstEndPoint().addArc(arc);
		Arc reversedArc = arc.createCopy();
		reversedArc.setId(idArc++);
		reversedArc.setFirstEndPoint(arc.getSecondEndPoint());
		reversedArc.setSecondEndPoint(arc.getFirstEndPoint());
		reversedArc.getFirstEndPoint().addArc(reversedArc);
		this.id2arc.put(arc.getId(), arc);
		this.id2arc.put(reversedArc.getId(), reversedArc);
	}

	public void addDirectedArc(Arc arc) {
		//arc.setId(idArc++);
		arc.getFirstEndPoint().addArc(arc);
		this.id2arc.put(arc.getId(), arc);
	}

	public Arc getArcByPosition(int id) {
		return this.id2arc.get(new LinkedList<Integer>(this.id2arc.keySet()).get(id));
	}


	/**
	 * 
	 * @return the longest shortest path in the graph
	 */
	public double getGraphDiameter() {
		double max = 0;
		for (Vertex vertex : this.getVertices())
			for (Vertex vertex2 : this.getVertices()){
				if (max < this.idVertex2idVertex2distance.get(vertex.getId()).get(vertex2.getId()))
					max = this.idVertex2idVertex2distance.get(vertex.getId()).get(vertex2.getId());
			}
		return max;
	}

	/**
	 * Compute distances between all-pair vertices using Floyd-Warshall's algorithm.
	 */
	public void createDistanceMatrix() { // floyd warshall
		Map<Integer, Map<Integer, Double>> dist = new HashMap<Integer, Map<Integer, Double>>();
		Map<Integer, Map<Integer, Arc>> nextArc = new HashMap<Integer, Map<Integer, Arc>>();
		int n = this.getVertices().size();
		for (Vertex vertex : this.getVertices()) {
			Map<Integer, Double> idVertex2distance = new HashMap<Integer, Double>();
			Map<Integer, Arc> idVertex2nextArc = new HashMap<Integer, Arc>();
			dist.put(vertex.getId(), idVertex2distance);
			nextArc.put(vertex.getId(), idVertex2nextArc);
			idVertex2distance.put(vertex.getId(), 0d);
		}

		for (Vertex vertex : this.getVertices()) {
			for (Arc arc : vertex.getArcs()) {
				dist.get(arc.getFirstEndPoint().getId()).put(
						arc.getSecondEndPoint().getId(), arc.getDistance());
				//dist.get(arc.getSecondEndPoint().getId()).put(
				//		arc.getFirstEndPoint().getId(), arc.getDistance());
				nextArc.get(arc.getFirstEndPoint().getId()).put(
						arc.getSecondEndPoint().getId(),
						arc);
				//nextArc.get(arc.getSecondEndPoint().getId()).put(
				//		arc.getFirstEndPoint().getId(),
				//		arc);
			}
		}

		for (int k = 0; k < n; k++)
			for (int i = 0; i < n; i++){
				//System.out.println("k:"+k+"\t i:"+i);
				for (int j = 0; j < n; j++) {
					if (dist.get(i).get(j) == null)
						dist.get(i).put(j, Double.MAX_VALUE);
					if (dist.get(i).get(k) == null)
						dist.get(i).put(k, Double.MAX_VALUE);
					if (dist.get(k).get(j) == null)
						dist.get(k).put(j, Double.MAX_VALUE);
					if (dist.get(i).get(j) > dist.get(i).get(k)
							+ dist.get(k).get(j)) {
						dist.get(i).put(j,
								dist.get(i).get(k) + dist.get(k).get(j));
						nextArc.get(i).put(j, nextArc.get(i).get(k));
					}
				}
			}

		this.idVertex2idVertex2distance = dist;
		this.idVertex2idVertex2nextArc = nextArc;
	}

	/**
	 * 
	 * @param from, a Vertex object
	 * @param to, a Vertex object
	 * @return the set of Arc that belong to the shortest path between 'from' and 'to'
	 */
	public List<Arc> getShortestPath(Vertex from, Vertex to){
		if(this.idVertex2idVertex2nextArc==null)
			this.createDistanceMatrix();
		List<Arc> path = new LinkedList<Arc>();
		while(!from.equals(to)){				
			Arc nextArc = this.getIdVertex2idVertex2nextArc().get(from.getId()).get(to.getId());
			path.add(nextArc);
			Vertex next = nextArc.getFirstEndPoint();
			if(!next.equals(from))
				from = next;
			else
				from = nextArc.getSecondEndPoint();
		}
		return path;
	}

	/**
	 * Compute distances between all-pair vertices using Floyd-Warshall's algorithm.
	 */
	public void createDistanceMatrixAll() { // floyd warshall
		Map<Integer, Map<Integer, Double>> dist = new HashMap<Integer, Map<Integer, Double>>();
		Map<Integer, Map<Integer, Set<Arc>>> nextArc = new HashMap<Integer, Map<Integer, Set<Arc>>>();
		int n = this.getVertices().size();
		for (Vertex vertex : this.getVertices()) {
			Map<Integer, Double> idVertex2distance = new HashMap<Integer, Double>();
			Map<Integer, Set<Arc>> idVertex2nextArc = new HashMap<Integer, Set<Arc>>();
			dist.put(vertex.getId(), idVertex2distance);
			nextArc.put(vertex.getId(), idVertex2nextArc);
			idVertex2distance.put(vertex.getId(), 0d);
			idVertex2nextArc.put(vertex.getId(), new TreeSet<Arc>());
		}

		for (Vertex vertex : this.getVertices()) {
			for (Arc arc : vertex.getArcs()) {
				Set<Arc> arcs = new TreeSet<Arc>();
				arcs.add(arc);
				dist.get(arc.getFirstEndPoint().getId()).put(
						arc.getSecondEndPoint().getId(), arc.getDistance());
				//dist.get(arc.getSecondEndPoint().getId()).put(
				//		arc.getFirstEndPoint().getId(), arc.getDistance());
				nextArc.get(arc.getFirstEndPoint().getId()).put(
						arc.getSecondEndPoint().getId(),
						arcs);
				//nextArc.get(arc.getSecondEndPoint().getId()).put(
				//		arc.getFirstEndPoint().getId(),
				//		arc);
			}
		}

		for (int k = 0; k < n; k++)
			for (int i = 0; i < n; i++){
				//System.out.println("k:"+k+"\t i:"+i);
				for (int j = 0; j < n; j++) {
					if (dist.get(i).get(j) == null)
						dist.get(i).put(j, Double.MAX_VALUE);
					if (dist.get(i).get(k) == null)
						dist.get(i).put(k, Double.MAX_VALUE);
					if (dist.get(k).get(j) == null)
						dist.get(k).put(j, Double.MAX_VALUE);
					if ((dist.get(i).get(j) == dist.get(i).get(k)
							+ dist.get(k).get(j)) && dist.get(i).get(j)!=Double.MAX_VALUE) {
						dist.get(i).put(j,
								dist.get(i).get(k) + dist.get(k).get(j));
						//System.out.println("dist.get("+i+").get("+j+")="+dist.get(i).get(j));
						//System.out.println("dist.get("+i+").get("+k+") + dist.get("+k+").get("+j+") + ="+(dist.get(i).get(k)+ dist.get(k).get(j)));

						nextArc.get(i).get(j).addAll(nextArc.get(i).get(k));
					}
					if (dist.get(i).get(j) > dist.get(i).get(k)
							+ dist.get(k).get(j)) {
						dist.get(i).put(j,
								dist.get(i).get(k) + dist.get(k).get(j));
						nextArc.get(i).put(j, new TreeSet<Arc>(nextArc.get(i).get(k)));
					}
				}
			}

		this.idVertex2idVertex2distance = dist;
		this.idVertex2idVertex2allNextArc = nextArc;
	}


	public Map<Integer, Map<Integer, Set<Arc>>> getIdVertex2idVertex2allNextArc() {
		return idVertex2idVertex2allNextArc;
	}

	public Collection<Arc> getArcs(){
		return this.id2arc.values();
	}

	public Arc getArcById(Integer idArc) {
		return this.id2arc.get(idArc);
	}

	//this function works only if arcs ids have been assigned correctly.
	// if an arc has id 2i, then the reversed arc must have id 2i+1.
	public Arc getReversedArc(int id) {
		if(id %2 == 0){
			return this.id2arc.get(id+1);
		}else{
			return this.id2arc.get(id-1);			
		}
	}

	@Override
	public int compareTo(Graph o) {
		return this.hashCode()-o.hashCode();
	}

	public int getMaxDegree(){
		int max = -1;
		for(Vertex v:this.getVertices())
			if(max < v.getArcs().size())
				max = v.getArcs().size();
		return max;
	}

	public int getSumDegree(){
		int sum = 0;
		for(Vertex v:this.getVertices())
			sum+=v.getArcs().size();
		return sum;
	}

	public Graph getReversedCopy() {
		Graph reversed =new Graph();
		for(Vertex v : this.getVertices()){
			Vertex copyVertex = v.createCopy();
			reversed.addVertex(copyVertex);
		}

		for(Arc arc : this.getArcs()){
			Arc copyArc = arc.createCopy();
			copyArc.setFirstEndPoint(reversed.getVertexById(arc.getSecondEndPoint().getId()));
			copyArc.setSecondEndPoint(reversed.getVertexById(arc.getFirstEndPoint().getId()));
			reversed.addDirectedArc(copyArc);
		}

		return reversed;
	}

	public Arc getRandomArc(){
		return new LinkedList<Arc>(this.id2arc.values()).get((int)(Math.random()*this.id2arc.values().size()));
	}


	public void removeVertexById(int id) {
		this.id2vertex.remove(id);
		int oldId = this.getVertices().size();
		if(oldId!=id){
			Vertex v = this.getVertexById(oldId);
			v.setId(id);
			this.id2vertex.remove(oldId);
			this.id2vertex.put(id, v);
			//System.out.println(oldId+" -> " + id);
		}
	}
}
