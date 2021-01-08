package model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Vertex implements Comparable<Vertex>,Serializable{

	private int id;
	private Map<Integer,Arc> id2arc;

	public Vertex(){
		this.id2arc = new HashMap<Integer,Arc>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void removeArc(Integer id){
		this.id2arc.remove(id);
	}

	public Map<Integer, Arc> getId2arc() {
		return id2arc;
	}

	public void setId2arc(Map<Integer, Arc> id2arc) {
		this.id2arc = id2arc;
	}

	public Arc getArcById(int id){
		return this.id2arc.get(id);
	}

	public void addArc(Arc arc){
		this.id2arc.put(arc.getId(), arc);
	}

	public Collection<Arc> getArcs(){
		return this.id2arc.values();
	}

	public Vertex createCopy() {
		Vertex vertex = new Vertex();
		vertex.setId(this.getId());
		return vertex;
	}

	public String toString(){
		return this.getId()+"";
	}

	public String toStringVerbose(){
		return this.getId() + " - edges:"+this.getArcs();
	}

	@Override
	public int compareTo(Vertex arg0) {
		return this.getId() - arg0.getId();
	}
	
	public boolean equals(Object o){
		Vertex v = (Vertex)o;
		return v.getId() == this.getId();
	}
	
	public int hashCode(){
		return this.id;
	}
}
