package daggenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class ShortestPathDagGenerator extends IDagGenerator{

	@Override
	public Map<Integer, Graph> computeDag(Graph g,
			List<Pair<Integer>> setOfDemands) {
		Map<Integer,Graph> destination2ht = new HashMap<Integer,Graph>();
		Map<Integer,Set<Integer>> destination2sources = this.computeDestination2Sources(setOfDemands);
		if(g.getIdVertex2idVertex2distance() == null)
			g.createDistanceMatrixAll();

		//compute shortest path DAG
		for(Integer destId : destination2sources.keySet()){
			Set<Integer> analyzed = new HashSet<Integer>();
			Graph ht = new Graph();
			destination2ht.put(destId, ht);
			for(Integer sourceId : destination2sources.get(destId)){
				if(analyzed.contains(sourceId))
					continue;
				List<Integer> queue = new LinkedList<Integer>();
				Set<Integer> queued = new HashSet<Integer>();
				queue.add(sourceId);
				queued.add(sourceId);
				Vertex source = new Vertex();
				source.setId(sourceId);
				ht.addVertex(source);
				while(!queue.isEmpty()){
					Vertex v = g.getVertexById(queue.remove(0));
					if(analyzed.contains(v.getId()))
						continue;
					analyzed.add(v.getId());
					for(Arc arc : g.getIdVertex2idVertex2allNextArc().get(v.getId()).get(destId)){
						Arc arcCopy = new Arc();
						arcCopy.setCapacity(arc.getCapacity());
						arcCopy.setDistance(arc.getDistance());
						arcCopy.setId(arc.getId());
						arcCopy.setFirstEndPoint(ht.getVertexById(v.getId()));
						Vertex head = ht.getVertexById(arc.getSecondEndPoint().getId());
						if(head==null){
							head = new Vertex();
							head.setId(arc.getSecondEndPoint().getId());
							ht.addVertex(head);
						}
						arcCopy.setSecondEndPoint(head);
						ht.addDirectedArc(arcCopy);
						queue.add(head.getId());
					}
				}	
			}			
		}

		return destination2ht;
	}

}
