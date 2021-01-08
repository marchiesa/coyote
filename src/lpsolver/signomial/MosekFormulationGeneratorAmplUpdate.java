package lpsolver.signomial;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MosekFormulationGeneratorAmplUpdate {

	private Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value= new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();

	public void updateFormulation(String graphNameFile){
		String path =graphNameFile+"-splitting-ratio-formulation-data.txt";
		BufferedReader br = null;

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(path));

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




			PrintWriter writerSplittingRatio = new PrintWriter(graphNameFile+"-splitting-ratio-formulation.mod", "UTF-8");
			for(Integer destId : phi_destination2vertex2edge2value.keySet()){
				for(Integer vertexId : phi_destination2vertex2edge2value.get(destId).keySet()){
					Map<Integer,Double> edge2value = this.getA(destId, vertexId);
					//System.out.print("subject to splitting_ratio_constraint_"+destId+"_"+vertexId+": ");
					writerSplittingRatio.print("subject to splitting_ratio_constraint_"+destId+"_"+vertexId+": ");
					for(Integer arcId : phi_destination2vertex2edge2value.get(destId).get(vertexId).keySet()){

						//System.out.print(" + " +edge2value.get(arcId) + " * phi_"+destId+"_"+vertexId+"_"+arcId+" ");
						//System.out.println(destId+" "+v.getId() + " "+ arc.getId()+ " " + 1d/v.getArcs().size());
						writerSplittingRatio.print(" + " +edge2value.get(arcId) + " * phi_"+destId+"_"+vertexId+"_"+arcId+" ");
						//vertex2index.put(v.getId(), indexConstraint);
					}
					//blc.add(-Math.log(currentSolution.getK(destId,v.getId())));
					//System.out.println(" >= -"+Math.log(this.getK(destId,vertexId))+";");
					writerSplittingRatio.println(" >= -"+Math.log(this.getK(destId,vertexId))+";");
					//System.out.println(" >= - k_"+destId+"_"+v.getId()+" ;");

				}
			}
			writerSplittingRatio.close();


		}catch(Exception e){
			System.out.println(e.getMessage());
		}

	}

	public Map<Integer,Double> getA(Integer destId, int vertexId) {
		double sum = 0;
		Map<Integer,Double> edge2value = new HashMap<Integer,Double>();
		for(Double value : this.phi_destination2vertex2edge2value.get(destId).get(vertexId).values()){
			sum += value;
		}
		for(Integer arcId : this.phi_destination2vertex2edge2value.get(destId).get(vertexId).keySet()){
			edge2value.put(arcId, this.phi_destination2vertex2edge2value.get(destId).get(vertexId).get(arcId)/sum);
			this.phi_destination2vertex2edge2value.get(destId).get(vertexId).put(arcId,edge2value.get(arcId));
		}
		return edge2value;
	}

	public double getK(Integer destId,int vertexId) {
		/*double sum = 0;
		for(Double value : this.phi_destination2vertex2edge2value.get(destId).get(vertexId).values()){
			sum += value;
		}*/
		Map<Integer,Double> edge2value = getA(destId,vertexId);
		double prod=1;
		for(Integer arcId: this.phi_destination2vertex2edge2value.get(destId).get(vertexId).keySet()){
			prod *= Math.pow(this.phi_destination2vertex2edge2value.get(destId).get(vertexId).get(arcId), edge2value.get(arcId));
		}
		return 1/prod;

	}


}