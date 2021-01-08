package model.mock;

import java.util.HashMap;
import java.util.Map;

import model.Arc;
import model.Graph;
import model.Vertex;

public class Destination2HTMock {
	
	public Map<Integer,Graph> destination2ht;
	
	public Destination2HTMock(){
		this.destination2ht = new HashMap<Integer,Graph>();
		
		Graph g0 = new Graph();
		Vertex v0 = new Vertex();
		v0.setId(0);
		Vertex v1 = new Vertex();
		v1.setId(1);
		Vertex v2 = new Vertex();
		v2.setId(2);
		g0.addVertex(v0);
		g0.addVertex(v1);
		g0.addVertex(v2);
		Arc arc = new Arc();
		arc.setId(1);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v1);
		arc.setSecondEndPoint(v0);
		g0.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(2);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v2);
		arc.setSecondEndPoint(v0);
		g0.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(5);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v1);
		arc.setSecondEndPoint(v2);
		g0.addDirectedArc(arc);
		this.destination2ht.put(0, g0);
		
		Graph g1 = new Graph();
		v0 = new Vertex();
		v0.setId(0);
		v1 = new Vertex();
		v1.setId(1);
		v2 = new Vertex();
		v2.setId(2);
		g1.addVertex(v0);
		g1.addVertex(v1);
		g1.addVertex(v2);
		arc = new Arc();
		arc.setId(0);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v0);
		arc.setSecondEndPoint(v1);
		g1.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(3);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v0);
		arc.setSecondEndPoint(v2);
		g1.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(4);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v2);
		arc.setSecondEndPoint(v1);
		g1.addDirectedArc(arc);
		this.destination2ht.put(1, g1);
		
		Graph g2 = new Graph();
		v0 = new Vertex();
		v0.setId(0);
		v1 = new Vertex();
		v1.setId(1);
		v2 = new Vertex();
		v2.setId(2);
		g2.addVertex(v0);
		g2.addVertex(v1);
		g2.addVertex(v2);
		arc = new Arc();
		arc.setId(0);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v0);
		arc.setSecondEndPoint(v1);
		g2.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(3);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v0);
		arc.setSecondEndPoint(v2);
		g2.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(5);
		arc.setCapacity(1);
		arc.setDistance(1);
		arc.setFirstEndPoint(v1);
		arc.setSecondEndPoint(v2);
		g2.addDirectedArc(arc);
		this.destination2ht.put(2, g2);	
	}

	public Map<Integer, Graph> getDestination2ht() {
		return destination2ht;
	}

	public void setDestination2ht(Map<Integer, Graph> destination2ht) {
		this.destination2ht = destination2ht;
	}	
}
