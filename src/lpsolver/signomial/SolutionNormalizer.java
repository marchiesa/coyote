package lpsolver.signomial;

import java.util.Map;

import model.Arc;
import model.Graph;
import model.Vertex;

public class SolutionNormalizer {
	
	public void normalizeSolution(Map<Integer,Graph> destination2ht, Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value){
		
		for(Integer destId : destination2ht.keySet()){
			Graph ht = destination2ht.get(destId);
			Map<Integer,Map<Integer,Double>> phi_vertex2edge2value  = phi_destination2vertex2edge2value.get(destId);
			for(Vertex v : ht.getVertices()){
				if(v.getId() == destId)
					continue;
				double sum=0;
				Map<Integer,Double> phi_edge2value  = phi_vertex2edge2value.get(v.getId());
				
				for(Arc arc: v.getArcs()){
					Double splitArc = phi_edge2value.get(arc.getId());
					if(splitArc==null)
						splitArc=0d;
					sum += splitArc;
				}
				
				for(Arc arc: v.getArcs()){
					Double splitArc = phi_edge2value.get(arc.getId());
					if(splitArc==null)
						splitArc=0d;
					phi_edge2value.put(arc.getId(),splitArc/sum);
				}
			}
		}
		
		//compute the reversed ht
		/*Map<Integer, Graph> destination2htReversed = MainMosekFormulationGeneratorAmpl.computeReversedDestination2Ht(destination2ht);
		Map<Integer, Map<Integer,Integer>> destination2order2vertexId = new HashMap<Integer, Map<Integer,Integer>>(); 
		for(Integer destId : destination2ht.keySet()){
			destination2order2vertexId.put(destId, computeTopologicalOrder(destination2ht.get(destId),destination2htReversed.get(destId),destId));
		}

		for(Integer destId : destination2ht.keySet()){
			Map<Integer,Integer> order2vertexId = destination2order2vertexId.get(destId);
			for(int i=order2vertexId.size()-1;i>0;i--){
				
			}
		}*/

	}

}
