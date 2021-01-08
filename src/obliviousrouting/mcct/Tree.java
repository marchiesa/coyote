package obliviousrouting.mcct;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Tree implements Comparable<Tree>{
	
	private Node root;
	private int id; //unused
	private Map<Integer,List<Node>> idLayer2nodes;

	public Node getRoot() {
		return root;
	}

	public void setRoot(Node root) {
		this.root = root;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void createRoot(int layers) {
		this.root = new Node();
		this.root.setId(Node.nextId++);
		this.idLayer2nodes = new HashMap<Integer,List<Node>>();
		List<Node> l = new LinkedList<Node>();
		l.add(this.root);
		this.idLayer2nodes.put(layers, l);
	}

	public boolean isLayerWithAllSingletons(int i) {
		boolean isAllSingletons = true;
		for(Node node : this.idLayer2nodes.get(i)){
			if(node.getVertices().size()!=1)
				isAllSingletons = false;
		}
		return isAllSingletons;
	}
	
	public List<Node> getLayerNodes(int layer){
		return this.idLayer2nodes.get(layer);
	}

	public void addNodeToLayer(int i, Node childNode) {
		List<Node> nodes = this.idLayer2nodes.get(i);
		if(nodes == null){
			nodes = new LinkedList<Node>();
			this.idLayer2nodes.put(i, nodes);
		}
		nodes.add(childNode);
	}

	public String toString(){
		return this.root.toString(0);
	}
	
	public Node getLeafVertex(int id){
		for(Node node : this.idLayer2nodes.get(0))
		if(node.getVertexById(id)!=null)
			return node;
		return null;
	}

	@Override
	public int compareTo(Tree o) {
		return -this.id+o.getId();
	}
	
	/*public boolean equals(Object o){
		Tree t = (Tree)o;
		if()
		return false;
	}*/
	

}
