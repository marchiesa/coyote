package inputoutput;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class SplittingRatioWriter {

	public void writeToFile(String graphNameFile, Map<Integer,Graph> destination2ht,List<Pair<Integer>> setOfDemands, Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value){

		Set<Integer> destinationVertices = new TreeSet<Integer>();
		Set<Integer> sourceVertices = new TreeSet<Integer>();
		for(Pair<Integer> demand : setOfDemands){
			destinationVertices.add(demand.getSecond());
			sourceVertices.add(demand.getFirst());
		}

		PrintWriter writerSplittingRatio;
		try {
			writerSplittingRatio = new PrintWriter(graphNameFile+"-splitting-ratio-formulation-data.txt", "UTF-8");
			//fourth type of constraint
			for(Integer destId : destinationVertices){
				//Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				//fourthConstraintDestination2vertex2index.put(destId, vertex2index);
				for(Vertex v: destination2ht.get(destId).getVertices()){
					if(v.getId()!=destId){
						//System.out.print("subject to splitting_ratio_constraint_"+destId+"_"+v.getId()+": ");
						//Map<Integer,Double> edge2value = currentSolution.getA(destId, v.getId());
						for(Arc arc: v.getArcs()){
							//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(destId).get(v.getId()).get(arc.getId()),edge2value.get(arc.getId())));
							//System.out.print(" + a_"+destId+"_"+arc.getId() +" * phi_"+destId+"_"+arc.getId()+" ");
							//System.out.println(destId+" "+v.getId() + " "+ arc.getId()+ " " + 1d/v.getArcs().size());
							writerSplittingRatio.println(destId+" "+v.getId() + " "+ arc.getId() + " " + Math.log(phi_destination2vertex2edge2value.get(destId).get(v.getId()).get(arc.getId())));
							//vertex2index.put(v.getId(), indexConstraint);
						}
						//blc.add(-Math.log(currentSolution.getK(destId,v.getId())));
						//System.out.println(" >= -"+Math.log(currentSolution.getK(destId,v.getId()))+";");
						//System.out.println(" >= - k_"+destId+"_"+v.getId()+" ;");
					}
				}
			}
			writerSplittingRatio.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void writeToFile(String graphNameFile, Graph g ,List<Pair<Integer>> setOfDemands, Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value){

		Set<Integer> destinationVertices = new TreeSet<Integer>();
		Set<Integer> sourceVertices = new TreeSet<Integer>();
		for(Pair<Integer> demand : setOfDemands){
			destinationVertices.add(demand.getSecond());
			sourceVertices.add(demand.getFirst());
		}

		PrintWriter writerSplittingRatio;
		try {
			writerSplittingRatio = new PrintWriter(graphNameFile+"-splitting-ratio-formulation-data.txt", "UTF-8");
			//fourth type of constraint
			for(Integer destId : destinationVertices){
				//Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				//fourthConstraintDestination2vertex2index.put(destId, vertex2index);
				for(Vertex v: g.getVertices()){
					if(v.getId()!=destId){
						//System.out.print("subject to splitting_ratio_constraint_"+destId+"_"+v.getId()+": ");
						//Map<Integer,Double> edge2value = currentSolution.getA(destId, v.getId());
						for(Arc arc: v.getArcs()){
							//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(destId).get(v.getId()).get(arc.getId()),edge2value.get(arc.getId())));
							//System.out.print(" + a_"+destId+"_"+arc.getId() +" * phi_"+destId+"_"+arc.getId()+" ");
							//System.out.println(destId+" "+v.getId() + " "+ arc.getId()+ " " + 1d/v.getArcs().size());
							writerSplittingRatio.println(destId+" "+v.getId() + " "+ arc.getId() + " " + Math.log(phi_destination2vertex2edge2value.get(destId).get(v.getId()).get(arc.getId())));
							//vertex2index.put(v.getId(), indexConstraint);
						}
						//blc.add(-Math.log(currentSolution.getK(destId,v.getId())));
						//System.out.println(" >= -"+Math.log(currentSolution.getK(destId,v.getId()))+";");
						//System.out.println(" >= - k_"+destId+"_"+v.getId()+" ;");
					}
				}
			}
			writerSplittingRatio.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
