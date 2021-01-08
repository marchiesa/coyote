package model.mock;

import model.Arc;
import model.Graph;
import model.Vertex;

public class GraphMock extends Graph{
	
	public GraphMock(){
		Vertex v0 = new Vertex();
		v0.setId(0);
		this.addVertex(v0);
		Vertex v1 = new Vertex();
		v1.setId(1);
		this.addVertex(v1);
		Vertex v2 = new Vertex();
		v2.setId(2);
		this.addVertex(v2);
		
		Arc arc = new Arc();
		arc.setId(0);
		arc.setFirstEndPoint(v0);
		arc.setSecondEndPoint(v1);
		arc.setDistance(1);
		arc.setCapacity(1);
		this.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(1);
		arc.setFirstEndPoint(v1);
		arc.setSecondEndPoint(v0);
		arc.setDistance(1);
		arc.setCapacity(1);
		this.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(2);
		arc.setFirstEndPoint(v2);
		arc.setSecondEndPoint(v0);
		arc.setDistance(1);
		arc.setCapacity(1);
		this.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(3);
		arc.setFirstEndPoint(v0);
		arc.setSecondEndPoint(v2);
		arc.setDistance(1);
		arc.setCapacity(1);
		this.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(4);
		arc.setFirstEndPoint(v2);
		arc.setSecondEndPoint(v1);
		arc.setDistance(1);
		arc.setCapacity(1);
		this.addDirectedArc(arc);
		arc = new Arc();
		arc.setId(5);
		arc.setFirstEndPoint(v1);
		arc.setSecondEndPoint(v2);
		arc.setDistance(1);
		arc.setCapacity(1);
		this.addDirectedArc(arc);
		
	}

}
