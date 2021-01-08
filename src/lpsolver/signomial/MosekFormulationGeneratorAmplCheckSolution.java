package lpsolver.signomial;

import inputoutput.DAGSolutionFromFileReader;
import inputoutput.ISetOfDemandsReader;
import inputoutput.RocketFuelGraphReader;
import inputoutput.SetOfDemandsReaderMockForSlides;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformance;
import lpsolver.CompactLPSolverObliviousPerformanceDAG;
import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;
import experiments.racke.MainRacke;
import experiments.racke.MainRackeWithHeuristic;

public class MosekFormulationGeneratorAmplCheckSolution {

	private String file;
	
	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public void checkSolution(String[] args){
		
		RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
		Graph g= rfgr.readDirectedGraph(args[1]);
		
		ISetOfDemandsReader sodr = new SetOfDemandsReaderMockForSlides();
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		setOfDemands = sodr.getSetOfDemands(g, null);			
		

		DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
		Map<Integer, Map<Pair<Integer>, Double>> arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "results/"+args[1].split("/")[2],setOfDemands);
		CompactLPSolverObliviousPerformance lpperf = new CompactLPSolverObliviousPerformance();
		lpperf.computeObliviousPerformance(g, arc2demand2fraction);
		System.out.println("Oblivious performance: " + lpperf.getOpt());
		System.out.println("Most congested arc: " + g.getArcById(lpperf.getMostCongestedIdArc()));
		System.out.println("Demands: " + MainRackeWithHeuristic.printDemands(lpperf.getDemand2nameAndValue()));
		System.out.println("Optimal solution for these demands: " + MainRackeWithHeuristic.printFlow(lpperf.getArc2idVertex2nameAndValue()));
	}
	
	public void checkDAGSolution(String path, Graph g, Map<Integer,Graph> destination2ht,Map<Integer, Map<Pair<Integer>, Double>> arc2demand2fraction, String algorithm, boolean aggregate) throws FileNotFoundException{
		
		
		CompactLPSolverObliviousPerformanceDAG lpperf = new CompactLPSolverObliviousPerformanceDAG();
		//CompactLPSolverObliviousPerformance lpperf = new CompactLPSolverObliviousPerformance();
		lpperf.computeObliviousPerformance(g, arc2demand2fraction,destination2ht, aggregate);
		//lpperf.computeObliviousPerformance(g, arc2demand2fraction, aggregate);
		File file2 = new File("results/"+path.split("\\.")[0].split("/")[2]+"-"+algorithm+"-oblivious.txt");
		PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
		System.out.print("Oblivious Solution: " + MainRacke.print(arc2demand2fraction,g,lpperf.getDemand2nameAndValue()));
		System.out.println("Oblivious performance: " + lpperf.getOpt());
		writer.println(lpperf.getOpt());
		System.out.println("Most congested arc: " + g.getArcById(lpperf.getMostCongestedIdArc()));
		System.out.println("Demands: " + MainRacke.printDemands(lpperf.getDemand2nameAndValue()));
		System.out.println("Optimal solution for these demands: " + MainRacke.printFlow(lpperf.getArc2idVertex2nameAndValue()));
		writer.flush();
		writer.close();
	}

	private Map<Integer, Map<Pair<Integer>, Double>> readArc2demand2fraction(Graph g) {

		Map<Integer, Map<Pair<Integer>, Double>> arc2demand2fraction=null;
		Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value= new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();
		String path ="splitting-ratio-formulation-data.txt";
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

			br.close();
			arc2demand2fraction = new HashMap<Integer, Map<Pair<Integer>, Double>>();
			path = "fractional-flows-data.txt";

			br = new BufferedReader(new FileReader(path));

			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split(" ");
				Integer demandFirst = Integer.parseInt(split[0]);
				Integer demandSecond = Integer.parseInt(split[1]);
				Integer idVertex = Integer.parseInt(split[2]);
				Double value= Double.parseDouble(split[3]);
				Vertex v = g.getVertexById(idVertex);
				for(Arc arc : v.getArcs()){
					if(phi_destination2vertex2edge2value.get(demandSecond)!=null && phi_destination2vertex2edge2value.get(demandSecond).get(idVertex) !=null ){
						Double splitRatio = phi_destination2vertex2edge2value.get(demandSecond).get(idVertex).get(arc.getId());
						Map<Pair<Integer>, Double> demand2fraction = arc2demand2fraction.get(arc);
						if(demand2fraction == null){
							demand2fraction = new HashMap<Pair<Integer>,Double>();
							arc2demand2fraction.put(arc.getId(), demand2fraction);
						}
						demand2fraction.put(new Pair<Integer>(demandFirst, demandSecond),Math.exp(value + splitRatio));
					}

				}

			}
			br.close();
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		return arc2demand2fraction;
	}
}
