package model;

import java.io.Serializable;

public class Arc  implements Comparable<Arc>,Serializable{
	
	private int id;
	private Vertex firstEndPoint;
	private Vertex secondEndPoint;
	private double capacity;
	private double distance;
	
	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Vertex getFirstEndPoint() {
		return firstEndPoint;
	}

	public void setFirstEndPoint(Vertex firstEndPoint) {
		this.firstEndPoint = firstEndPoint;
	}

	public Vertex getSecondEndPoint() {
		return secondEndPoint;
	}

	public void setSecondEndPoint(Vertex secondEndPoint) {
		this.secondEndPoint = secondEndPoint;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public Arc createCopy() {
		Arc copy = new Arc();
		copy.setId(this.getId());
		copy.setCapacity(this.getCapacity());
		copy.setDistance(this.getDistance());
		return copy;
	}
	
	public String toString(){
		return "(id:"+this.getId()+"-("+this.firstEndPoint+","+this.secondEndPoint+")-d:"+this.getDistance()+"-c:"+this.capacity+")";
		//return "(id:"+this.getId()+"-("+this.firstEndPoint+","+this.secondEndPoint+")";
		//return "("+this.firstEndPoint+","+this.secondEndPoint+")";
	}
	
	public String toStringShort(){
		//return "(id:"+this.getId()+"-("+this.firstEndPoint+","+this.secondEndPoint+")-d:"+this.getDistance()+"-c:"+this.capacity+")";
		//return "(id:"+this.getId()+"-("+this.firstEndPoint+","+this.secondEndPoint+")";
		return "("+this.firstEndPoint+","+this.secondEndPoint+")";
	}
	
	public int hashCode(){
		return this.getFirstEndPoint().getId() + this.getSecondEndPoint().getId() + this.getId();
	}
	
	public boolean equals(Object o){
		Arc arc = (Arc)o;
		return 	arc.getId() == this.getId();
	}

	@Override
	public int compareTo(Arc arc) {
		return this.getId() - arc.getId();
	}
	
	

}
