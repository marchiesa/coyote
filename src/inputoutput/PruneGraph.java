package inputoutput;

import java.util.LinkedList;
import java.util.List;

import model.Arc;
import model.Graph;
import model.Vertex;

public class PruneGraph {
	
	public void pruneDegreeOne(Graph g){
		
		List<Vertex> border = new LinkedList<Vertex>();
		for(Vertex vertex: g.getVertices()){
			if(vertex.getArcs().size()==1)
				border.add(vertex);
		}
		
		//int counter=0;
		while(border.size()>0){
			Vertex v = border.get(0);
			//System.out.println("remove"+v.getId() + " counter="+counter++);
			border.remove(v);
			for(Arc arc:v.getArcs()){
				if(arc.getSecondEndPoint().getArcs().size()==2)
					border.add(arc.getSecondEndPoint());
				g.removeArcById(arc.getId());
				g.removeArcById(g.getReversedArc(arc.getId()).getId());
			}
			g.removeVertexById(v.getId());
		}
		//System.out.println(g);
		
	}

}
