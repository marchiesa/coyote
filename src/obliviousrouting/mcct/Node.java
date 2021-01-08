package obliviousrouting.mcct;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import model.Vertex;

/**
 * A node of a decomposition tree.
 * @author mosfet
 *
 */
public class Node {
	
	public static int nextId=0;
	private int id;
	private Node parent;
	private List<Node> children;
	private List<Vertex> vertices;
	private Vertex center;
	
	public Node(){
		this.children = new LinkedList<Node>();
		this.vertices = new LinkedList<Vertex>();
	}
	
	
	
	public Vertex getCenter() {
		return center;
	}



	public void setCenter(Vertex center) {
		this.center = center;
	}



	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Node getParent() {
		return parent;
	}
	public void setParent(Node parent) {
		this.parent = parent;
	}
	public List<Node> getChildren() {
		return children;
	}
	public void setChildren(List<Node> children) {
		this.children = children;
	}

	public List<Vertex> getVertices() {
		return vertices;
	}

	public void setVertices(List<Vertex> vertices) {
		this.vertices = vertices;
	}

	public void addVertex(Vertex vertex){
		this.vertices.add(vertex);
	}
	
	public void addVertices(Collection<Vertex> vertices){
		this.vertices.addAll(vertices);
	}
	
	
	public boolean containsVertex(int id){
		for(Vertex vertex: this.vertices)
			if(vertex.getId()==id)
				return true;
		return false;
	}
	
	public boolean containsVertex(Vertex vertex){
		return this.vertices.contains(vertex);
	}
	
	public Vertex getVertexById(int id){
		for(Vertex vertex: this.vertices)
			if(vertex.getId()==id)
				return vertex;
		return null;
	}
	
	public void addChild(Node child){
		this.children.add(child);
	}
	
	public String toString(int levels){
		String result = "";
		for (int i=0;i<levels;i++)
			result+=".";
		result+=this.getCenter()+" -  " + this.getVertices() +"\n";
		for(Node child:this.getChildren())
			result+=child.toString(levels+1);
		return result;
	}
	
	public String toString(){
		String result = "c:"+this.getCenter()+ " ver:"+this.getVertices();
		return result;
	}

}
