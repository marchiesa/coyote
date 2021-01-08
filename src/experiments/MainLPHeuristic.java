package experiments;


import gurobi.GRBException;
import inputoutput.RocketFuelGraphReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformance;
import lpsolver.LPSolverHeuristic;
import model.Arc;
import model.Graph;
import util.Pair;

/**
 * TODO: it does not work for networks with more than 40 nodes 
 *       because of scalability problems when the optimal 
 *       oblivious routing is computed for each tree.
 * 
 * @author mosfet
 *
 */
public class MainLPHeuristic {

	public static void main(String[] args) throws IOException, GRBException{
		//		String file="topologies/Backbone/Deltacom.lgf";
		String file=args[0];	


		/*Graph g = new SimpleToyGraphReader().readUndirectedGraph("input2.txt");
		List<Pair<Integer>> demands = new LinkedList<Pair<Integer>>();
		Pair<Integer> demand = new Pair<Integer>(1,0);
		demands.add(demand);
		demand = new Pair<Integer>(2,0);
		demands.add(demand);*/
		Graph g = new RocketFuelGraphReader().readUndirectedGraph(file);
		//Graph g = new CliqueGenerator().createClique(4);
		System.out.println(g);
		int i=0;

		Map<Integer,Double> numberOfTrees2obliviousPerformances = new HashMap<Integer,Double>(); 


		LPSolverHeuristic lps = new LPSolverHeuristic ();
		long startTime = System.nanoTime();
		lps.computeOptimalPerformance(g);
		//lps.computeOptimalPerformance(g,demands);
		numberOfTrees2obliviousPerformances.put(i+1, lps.getOpt());
		//System.out.println("Optimal solution: " + print(lps.getArc2demand2nameAndValue(),g));
		CompactLPSolverObliviousPerformance lpperf = new CompactLPSolverObliviousPerformance();
		lpperf.computeObliviousPerformance(g, lps.getArc2demand2fraction());
		System.out.println("Oblivious performance: " + lpperf.getOpt());
		System.out.println();
		System.out.println("Execution time:"+(System.nanoTime() - startTime)/1000000000d);
	}


	public static String printFlow(
			Map<Integer, Map<Integer, Pair<String>>> arc2idVertex2nameAndValue) {
		String result="";
		for(Integer idArc : arc2idVertex2nameAndValue.keySet()){
			boolean haveOnePositive = false;
			for(Integer vertex: arc2idVertex2nameAndValue.get(idArc).keySet() ){
				if(Double.parseDouble(arc2idVertex2nameAndValue.get(idArc).get(vertex).getSecond())>0){
					if(!haveOnePositive){
						haveOnePositive=true;
						result+="\n\t";
					}
					result+=arc2idVertex2nameAndValue.get(idArc).get(vertex).getFirst()+":"+arc2idVertex2nameAndValue.get(idArc).get(vertex).getSecond()+", ";
				}
			} 
		}
		return result;
	}

	public static String printDemands(
			Map<Pair<Integer>, Pair<String>> demand2nameAndValue) {
		String result = "[";
		for(Pair<Integer> demand : demand2nameAndValue.keySet()){
			if(Double.parseDouble(demand2nameAndValue.get(demand).getSecond())>0d){
				result+=demand2nameAndValue.get(demand).getFirst()+":"+demand2nameAndValue.get(demand).getSecond()+", ";
			}
		}
		result+="]";
		return result;
	}

	public static String print(
			Map<Integer, Map<Pair<Integer>, Pair<String>>> arc2demand2nameAndValue, Graph g) {
		String result="\n";
		for(Integer arcId : arc2demand2nameAndValue.keySet()){
			Arc arc = g.getArcById(arcId);
			result+="\t"+arc.toStringShort()+"{";
			for(Pair<Integer> demand:arc2demand2nameAndValue.get(arcId).keySet()){
				 if(!arc2demand2nameAndValue.get(arcId).get(demand).getSecond().equals("0.0"))
					 result+="("+arc2demand2nameAndValue.get(arcId).get(demand).getFirst()+"="+arc2demand2nameAndValue.get(arcId).get(demand).getSecond()+")";
			}
			result+="}\n";
		}
		return result;
	}


	@SuppressWarnings("unused")
	private static String print(
			Map<Integer, Map<Pair<Integer>, Double>> solution, Graph g, Map<Pair<Integer>,Pair<String>> demands) {
		String result="\n";
		Map<Pair<Integer>, Map<Integer, Double>> demand2arc2fraction = new HashMap<Pair<Integer>, Map<Integer, Double>>();
		for(Integer idArc : solution.keySet()){
			for(Pair<Integer> demand : solution.get(idArc).keySet()){
				//if(demands.get(demand)==null || demands.get(demand).getSecond().equals("0.0"))
				//	continue;
				Map<Integer, Double> arc2fraction =demand2arc2fraction.get(demand); 
				if(arc2fraction ==null){
					arc2fraction = new HashMap<Integer, Double>();
					demand2arc2fraction.put(demand, arc2fraction);
				}
				if(arc2fraction.get(idArc)==null){
					arc2fraction.put(idArc,0d);
				}
				arc2fraction.put(idArc, arc2fraction.get(idArc) + solution.get(idArc).get(demand));

			}
		}

		for(Pair<Integer> demand:demand2arc2fraction.keySet()){
			result+="\t"+demand+"={";
			for(Integer idArc:demand2arc2fraction.get(demand).keySet()){
				result+="("+g.getArcById(idArc).getFirstEndPoint()+","+g.getArcById(idArc).getSecondEndPoint()+"):"+
						demand2arc2fraction.get(demand).get(idArc)+", ";
			}
			result+="\n";
		}
		return result;
	}
}

