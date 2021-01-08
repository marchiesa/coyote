package inputoutput;

import model.Arc;
import model.Graph;
import model.Vertex;

/**
 * Generates cliques of any size
 * 
 * @author mosfet
 *
 */
public class CliqueGenerator {


	public Graph createClique(int size){

		Graph g = new Graph();
		int idArc=0;
		for(int i=0;i<size;i++){
			Vertex v1 = g.getVertexById(i);
			if(v1==null){
				v1 = new Vertex();
				v1.setId(i);
				g.addVertex(v1);
			}
			for(int j=i+1;j<size;j++){
				Vertex v2 = g.getVertexById(j);
				if(v2==null){
					v2 = new Vertex();
					v2.setId(j);
					g.addVertex(v2);
				}

				Arc arc = new Arc();
				arc.setFirstEndPoint(v1);
				arc.setSecondEndPoint(v2);
				arc.setCapacity(1d);
				arc.setId(idArc++);
				g.addDirectedArc(arc); 

				Arc reversedArc = new Arc();
				reversedArc.setFirstEndPoint(v2);
				reversedArc.setSecondEndPoint(v1);
				reversedArc.setCapacity(1d);
				reversedArc.setId(idArc++);
				g.addDirectedArc(reversedArc);
			}
		}
		return g;
	}



}
