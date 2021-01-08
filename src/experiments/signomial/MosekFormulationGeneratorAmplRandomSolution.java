package experiments.signomial;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.Arc;
import model.Graph;
import model.Vertex;

public class MosekFormulationGeneratorAmplRandomSolution {

	
	public void writeRandomSolution(Map<Integer,Graph> destination2ht) throws FileNotFoundException, UnsupportedEncodingException{
		
		List<Integer> destinationVertices = new LinkedList<Integer>(destination2ht.keySet());
		PrintWriter writerSplittingRatio = new PrintWriter("splitting-ratio-formulation-data.txt", "UTF-8");
		//fourth type of constraint
		for(Integer destId : destinationVertices){
			//Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
			//fourthConstraintDestination2vertex2index.put(destId, vertex2index);
			for(Vertex v: destination2ht.get(destId).getVertices()){
				if(v.getId()!=destId){
					//System.out.print("subject to splitting_ratio_constraint_"+destId+"_"+v.getId()+": ");
					//Map<Integer,Double> edge2value = currentSolution.getA(destId, v.getId());
					double sum = 0;
					Map<Integer,Double> arcId2splittingRatio = new HashMap<Integer,Double>();
					for(Arc arc: v.getArcs()){
						arcId2splittingRatio.put(arc.getId(), 1-Math.random());
						sum += arcId2splittingRatio.get(arc.getId());
					}
					for(Arc arc: v.getArcs()){
						//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(destId).get(v.getId()).get(arc.getId()),edge2value.get(arc.getId())));
						//System.out.print(" + a_"+destId+"_"+arc.getId() +" * phi_"+destId+"_"+arc.getId()+" ");
						//System.out.println(destId+" "+v.getId() + " "+ arc.getId()+ " " + 1d/v.getArcs().size());
						writerSplittingRatio.println(destId+" "+v.getId() + " "+ arc.getId() + " " + Math.log(arcId2splittingRatio.get(arc.getId())/sum));
						//vertex2index.put(v.getId(), indexConstraint);
					}
					//blc.add(-Math.log(currentSolution.getK(destId,v.getId())));
					//System.out.println(" >= -"+Math.log(currentSolution.getK(destId,v.getId()))+";");
					//System.out.println(" >= - k_"+destId+"_"+v.getId()+" ;");
				}
			}
		}
		writerSplittingRatio.close();

	}
}

