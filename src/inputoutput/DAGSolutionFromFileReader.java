package inputoutput;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
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

public class DAGSolutionFromFileReader {

	public Map<Integer,Map<Pair<Integer>,Double>> getSolutionForSignomialApproach( Graph g, 
			String file, List<Pair<Integer>> setOfDemands){
		return this.getSolutionForSignomialApproach(g, file, setOfDemands, null,null,0d);
	}

	public Map<Integer, Map<Pair<Integer>, Double>> getSolutionForSignomialApproach(
			Graph g, String file, List<Pair<Integer>> setOfDemands, Arc arc, double scalingFactor) {
		return this.getSolutionForSignomialApproach(g, file, setOfDemands, null,arc,scalingFactor);
	}

	public Map<Integer, Map<Pair<Integer>, Double>> getSolutionForSignomialApproach(
			Graph g, String file, List<Pair<Integer>> setOfDemands, Double wReal) {
		return this.getSolutionForSignomialApproach(g, file, setOfDemands, wReal,null,0d);
	}

	public Map<Integer,Map<Pair<Integer>,Double>> getSolutionForSignomialApproach( Graph g, String file, List<Pair<Integer>> setOfDemands, Double w ,Arc arcWReducedCapacity, double scalingFactor){
		DecimalFormat df = new DecimalFormat("#.00"); 

		Map<Integer, Map<Pair<Integer>, Double>> arc2demand2fraction=new HashMap<Integer, Map<Pair<Integer>, Double>>();
		Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value= new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();
		String splittingRatioFile = null;
		if(w!=null){
			splittingRatioFile =file+"-splitting-ratio-formulation-data-"+df.format(w)+".txt";
			//System.out.println("Reading from: " + splittingRatioFile );
		}
		else
			splittingRatioFile =file+"-splitting-ratio-formulation-data.txt";
		BufferedReader br = null;


		Map<Integer,Graph> destination2ht = new HashMap<Integer,Graph>();

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(splittingRatioFile));

			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split(" ");
				//System.out.println(split[0] + " " +split[1] + " " + split[2] + " " + split[3] + " " );
				Integer destId = Integer.parseInt(split[0]);
				Map<Integer,Map<Integer,Double>> vertex2arc2value = phi_destination2vertex2edge2value.get(destId);
				if(vertex2arc2value == null){
					vertex2arc2value = new HashMap<Integer,Map<Integer,Double>>();
					phi_destination2vertex2edge2value.put(destId, vertex2arc2value);
				}
				Integer vertexId = Integer.parseInt(split[1]);
				Map<Integer,Double> arc2value = vertex2arc2value.get(vertexId);
				if(arc2value == null){
					arc2value = new HashMap<Integer,Double>();
					vertex2arc2value.put(vertexId, arc2value);
				}
				Integer arcId = Integer.parseInt(split[2]);
				Double value = Double.parseDouble(split[3]);
				if(value==Double.NEGATIVE_INFINITY)
					arc2value.put(arcId, 0d);
				else
					arc2value.put(arcId, Math.exp(value));

				Graph ht = destination2ht.get(destId);
				if(ht==null){
					ht = new Graph(); 
					destination2ht.put(destId, ht);
				}

				Vertex v = ht.getVertexById(vertexId);
				if(v == null){
					v = new Vertex();
					v.setId(vertexId);
					ht.addVertex(v);
				}

				Arc arc = new Arc();
				arc.setCapacity(g.getArcById(arcId).getCapacity());
				arc.setDistance(g.getArcById(arcId).getDistance());
				arc.setFirstEndPoint(v);
				arc.setId(arcId);
				Vertex head = ht.getVertexById(g.getArcById(arcId).getSecondEndPoint().getId());
				if(head == null){
					head = new Vertex();
					head.setId(g.getArcById(arcId).getSecondEndPoint().getId());
					ht.addVertex(head);
				}
				arc.setSecondEndPoint(head);
				ht.addDirectedArc(arc);

			}

			br.close();

		}catch(Exception e){
			System.out.println(e.getMessage());
			return null;
		}

		if(arcWReducedCapacity != null){
			for(Integer destId : phi_destination2vertex2edge2value.keySet())
				for(Integer vertexId : phi_destination2vertex2edge2value.get(destId).keySet()){
					boolean reduceSplit = false;
					double splitRatioArcReduced = 0d;
					double sum = 0d;
					for(Integer arcId : phi_destination2vertex2edge2value.get(destId).get(vertexId).keySet()){				
						if(2*(arcId/2) == 2*(arcWReducedCapacity.getId()/2)){ //FIXME: fix for directed graphs
							reduceSplit = true;
							splitRatioArcReduced = phi_destination2vertex2edge2value.get(destId).get(vertexId).get(arcId) * scalingFactor;
							sum += splitRatioArcReduced;
						}
						else
							sum += phi_destination2vertex2edge2value.get(destId).get(vertexId).get(arcId);
					}
					//System.out.println("destId: " + destId + " vertexId: " + vertexId+ " sum: "+sum+" reduced: "+splitRatioArcReduced);
					if(reduceSplit){
						for(Integer arcId : phi_destination2vertex2edge2value.get(destId).get(vertexId).keySet()){
							if(2*(arcId/2) == 2*(arcWReducedCapacity.getId()/2)){ //FIXME: fix for directed graphs 
								phi_destination2vertex2edge2value.get(destId).get(vertexId).put(arcId, 
										splitRatioArcReduced/sum);
							}else{
								phi_destination2vertex2edge2value.get(destId).get(vertexId).put(arcId, 
										phi_destination2vertex2edge2value.get(destId).get(vertexId).get(arcId)/sum);
							}
						}
					}
				}
		}


		//compute the reversed ht
		Map<Integer, Graph> destination2htReversed = MainMosekFormulationGeneratorAmpl.computeReversedDestination2Ht(destination2ht);
		Map<Integer, Map<Integer,Integer>> destination2order2vertexId = new HashMap<Integer, Map<Integer,Integer>>(); 
		for(Integer destId : destination2ht.keySet()){
			destination2order2vertexId.put(destId, computeTopologicalOrder(destination2ht.get(destId),destination2htReversed.get(destId),destId));
		}

		Map<Integer,Map<Pair<Integer>,Double>> vertex2demand2value = new HashMap<Integer,Map<Pair<Integer>,Double>>();
		for(Pair<Integer> demand : setOfDemands){
			Map<Integer,Integer> order2vertexId = destination2order2vertexId.get(demand.getSecond());

			Graph ht = destination2ht.get(demand.getSecond());
			//find order of demand.getFirst()
			int orderDemandFirst = -1;
			for(Integer order: order2vertexId.keySet()){
				if(order2vertexId.get(order)==demand.getFirst()){
					orderDemandFirst = order;
					break;
				}
			}

			if(orderDemandFirst == -1)
				throw new RuntimeException();

			//topological search
			Set<Integer> analyzedPlusQueue = new TreeSet<Integer>();
			analyzedPlusQueue.add(demand.getFirst());
			Map<Pair<Integer>,Double> demand2value = vertex2demand2value.get(demand.getFirst());
			if(demand2value==null){
				demand2value = new HashMap<Pair<Integer>,Double>();
				vertex2demand2value.put(demand.getFirst(), demand2value);
			}
			demand2value.put(demand, 1d);
			for(int i=orderDemandFirst;i>0;i--){
				Vertex v = ht.getVertexById(order2vertexId.get(i));
				if(!analyzedPlusQueue.contains(v.getId()))
					continue;
				demand2value = vertex2demand2value.get(v.getId());
				for(Arc arc: v.getArcs()){
					analyzedPlusQueue.add(arc.getSecondEndPoint().getId());
					Map<Pair<Integer>,Double> demand2valueNext = vertex2demand2value.get(arc.getSecondEndPoint().getId());
					if(demand2valueNext==null){
						demand2valueNext = new HashMap<Pair<Integer>,Double>();
						vertex2demand2value.put(arc.getSecondEndPoint().getId(), demand2valueNext);
					}
					if(demand2valueNext.get(demand)==null){
						demand2valueNext.put(demand, 0d);
					}
					demand2valueNext.put(demand, demand2valueNext.get(demand)+demand2value.get(demand)*phi_destination2vertex2edge2value.get(demand.getSecond()).get(v.getId()).get(arc.getId()));
					Map<Pair<Integer>,Double> demand2valueArc = arc2demand2fraction.get(arc.getId());
					if(demand2valueArc==null){
						demand2valueArc = new HashMap<Pair<Integer>,Double>();
						arc2demand2fraction.put(arc.getId(), demand2valueArc);
					}
					demand2valueArc.put(demand, demand2value.get(demand)*phi_destination2vertex2edge2value.get(demand.getSecond()).get(v.getId()).get(arc.getId()));						
				}
			}

		}

		return arc2demand2fraction;
	}


	private static Map<Integer, Integer> computeTopologicalOrder( Graph g, Graph reversed, Integer destId) {
		Map<Integer,Integer> order2vertexId = new HashMap<Integer,Integer>();

		Set<Integer> analyzed = new TreeSet<Integer>();
		List<Integer> queue = new LinkedList<Integer>();
		queue.add(destId);
		int counter =0;
		while(!queue.isEmpty()){
			Vertex v = reversed.getVertexById(queue.remove(0));
			analyzed.add(v.getId());
			order2vertexId.put(counter++,v.getId());
			outerloop:
				for(Arc arc: v.getArcs()){
					Vertex nextReversed = arc.getSecondEndPoint(); //reversed graph
					Vertex next = g.getVertexById(nextReversed.getId());
					for(Arc arc2 : next.getArcs())
						if(!analyzed.contains(arc2.getSecondEndPoint().getId()))
							continue outerloop;
					queue.add(arc.getSecondEndPoint().getId());
				}

		}

		return order2vertexId;
	}

	public Map<Integer, Map<Integer, Map<Integer, Double>>> getPhiForSignomialApproach(String graphNameFile) {
		Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value= new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();
		String splittingRatioFile =graphNameFile+"-splitting-ratio-formulation-data.txt";
		BufferedReader br = null;

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(splittingRatioFile));

			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split(" ");
				Integer destId = Integer.parseInt(split[0]);
				Map<Integer,Map<Integer,Double>> vertex2arc2value = phi_destination2vertex2edge2value.get(destId);
				if(vertex2arc2value == null){
					vertex2arc2value = new HashMap<Integer,Map<Integer,Double>>();
					phi_destination2vertex2edge2value.put(destId, vertex2arc2value);
				}
				Integer vertexId = Integer.parseInt(split[1]);
				Map<Integer,Double> arc2value = vertex2arc2value.get(vertexId);
				if(arc2value == null){
					arc2value = new HashMap<Integer,Double>();
					vertex2arc2value.put(vertexId, arc2value);
				}
				Integer arcId = Integer.parseInt(split[2]);
				Double value = Double.parseDouble(split[3]);
				arc2value.put(arcId, Math.exp(value));
			}

			br.close();

		}catch(Exception e){
			System.out.println(e.getMessage());
		}


		return phi_destination2vertex2edge2value;

	}



}
