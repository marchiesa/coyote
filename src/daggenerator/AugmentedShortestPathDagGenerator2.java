package daggenerator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;
import experiments.signomial.MainMosekFormulationGeneratorAmpl;

/**
 * It creates a topological DAG starting from the 
 * @author mosfet
 *
 */

public class AugmentedShortestPathDagGenerator2 extends IDagGenerator{

	@Override
	public Map<Integer, Graph> computeDag(Graph g,
			List<Pair<Integer>> setOfDemands) {
		Map<Integer,Graph> destination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
		Map<Integer, Graph> destination2htReversed = MainMosekFormulationGeneratorAmpl.computeReversedDestination2Ht(destination2ht);
		for(Integer destId : destination2ht.keySet()){
			Graph ht = destination2ht.get(destId);
			Map<Integer,Integer> order2vertexId = computeTopologicalOrder(g,ht,destination2htReversed.get(destId),destId);
			Set<Integer> analyzed = new TreeSet<Integer>();
			for(int i=order2vertexId.size()-1;i>0;i--){
				Vertex vFromG =g.getVertexById(order2vertexId.get(i));
				Vertex vFromHt =ht.getVertexById(order2vertexId.get(i));
				if(vFromHt == null){
					vFromHt = new Vertex();
					vFromHt.setId(order2vertexId.get(i));
					ht.addVertex(vFromHt);
				}
				analyzed.add(order2vertexId.get(i));
				for(Arc arc : vFromG.getArcs()){
					if(ht.getArcById(arc.getId())!=null)
						continue;
					if(!analyzed.contains(arc.getSecondEndPoint().getId())){
						Arc arcHt = new Arc();
						arcHt.setCapacity(arc.getCapacity());
						arcHt.setDistance(arc.getDistance());
						arcHt.setFirstEndPoint(vFromHt);
						arcHt.setId(arc.getId());
						Vertex head = ht.getVertexById(arc.getSecondEndPoint().getId());
						if(head== null){
							head = new Vertex();
							head.setId(arc.getSecondEndPoint().getId());
							ht.addVertex(head);
						}
						arcHt.setSecondEndPoint(head);
						ht.addDirectedArc(arcHt);
					}
				}
			}
		}

		return destination2ht;
	}

	private static Map<Integer, Integer> computeTopologicalOrder( Graph g, Graph ht, Graph htReversed, Integer destId) {
		Map<Integer,Integer> order2vertexId = new HashMap<Integer,Integer>();

		Set<Integer> analyzed = new TreeSet<Integer>();
		List<Integer> queue = new LinkedList<Integer>();
		queue.add(destId);
		int counter =0;
		while(!queue.isEmpty()){
			Integer id = queue.remove(0);
			Vertex v = htReversed.getVertexById(id);
			if(v==null){
				v = new Vertex();
				v.setId(id);
				ht.addVertex(v);
			}
			analyzed.add(v.getId());
			order2vertexId.put(counter++,v.getId());
			outerloop:
				for(Arc arc: v.getArcs()){
					Vertex nextReversed = arc.getSecondEndPoint(); //reversed graph
					Vertex next = ht.getVertexById(nextReversed.getId());
					for(Arc arc2 : next.getArcs())
						if(!analyzed.contains(arc2.getSecondEndPoint().getId())){
							continue outerloop;
						}
					queue.add(arc.getSecondEndPoint().getId());
				}
			Vertex vFromG = g.getVertexById(v.getId());
			for(Arc arc: vFromG.getArcs()){
				if(ht.getVertexById(arc.getSecondEndPoint().getId())==null)
					queue.add(arc.getSecondEndPoint().getId());
			}

		}

		return order2vertexId;
	}

}
