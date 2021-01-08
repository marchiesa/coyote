package experiments.racke;



import gurobi.GRBException;
import inputoutput.RackeSolutionTransform;
import inputoutput.RocketFuelGraphReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformance;
import model.Graph;
import model.Vertex;
import obliviousrouting.RackeObliviousRoutingSolverShortestPath;
import util.Pair;

/**
 * Entry point to run Racke's algorithm based on shortest path trees. 
 *  - 1st argument is a path to the topology
 * @author mosfet
 *
 */
public class MainRackeShortestPath {

	public static void main(String[] args) throws IOException, GRBException{
		String file=args[0];	

		// read graph
		Graph g = new RocketFuelGraphReader().readUndirectedGraph(file);
		System.out.println(g);
		
		// create traffic demands among all vertices
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		for(Vertex v : g.getVertices()){
			for(Vertex w : g.getVertices()){
				if(v.getId()!=w.getId()){
					setOfDemands.add(new Pair<Integer>(v.getId(),w.getId()));
				}
			}
		} 

		// compute trees and lambdas with Racke's algorithm.
		RackeObliviousRoutingSolverShortestPath rors = new RackeObliviousRoutingSolverShortestPath();
		rors.setGraph(g);
		System.out.println("---- COMPUTING TREES ----");
		rors.createTreesAndLambda(setOfDemands);


		System.out.print("lambdas: " );
		for(Integer idTree: rors.getIdTree2destination2source2lambda().keySet()){
			System.out.print(rors.getLambdaByIdTree(idTree)+" ");
		}
		System.out.println();

		RackeSolutionTransform rso = new RackeSolutionTransform();

		//compute oblivious routing by combining the first i trees, with i=1,...,#number_of_trees
		System.out.println("---- OBLIVIOUS ROUTING SOLUTIONS ----");
		Map<Integer,Map<Pair<Integer>,Double>> solution=null;
		double sumOfLambdas =0;
		long startTime = System.nanoTime();
		for(int i=0;i<rors.getIdTree2destination2source2path().size();i++){
			System.out.println("Number of Trees: "+(i+1));
			sumOfLambdas+=rors.getLambdaByIdTree(i);
			Double newLambda= rors.getLambdaByIdTree(i)/sumOfLambdas;
			System.out.println("newLambda:"+newLambda);
			solution = rso.addASetOfTreesPerDestination(rors.getIdTree2destination2source2path().get(i), newLambda);
			//System.out.println("Mapping["+(i)+"](arcs):"+rso.getDecTree2notSpanningTrees().get(i));
			CompactLPSolverObliviousPerformance lpSolver = new CompactLPSolverObliviousPerformance();
			lpSolver.computeObliviousPerformance(rors.getGraph(), solution);
			//System.out.println("Graph: " + rors.getGraph());
			//System.out.println("Lambdas: " + rors.getLambda());
			//System.out.print("Oblivious Solution: " + print(solution,g,lpSolver.getDemand2nameAndValue()));
			System.out.println("Oblivious performance: " + lpSolver.getOpt());
			//numberOfTrees2obliviousPerformances.put(i+1, lpSolver.getMax());
			//System.out.println("Most congested arc: " + rors.getGraph().getArcById(lpSolver.getMostCongestedIdArc()));
			//System.out.println("Demands: " + printDemands(lpSolver.getDemand2nameAndValue()));
			//System.out.println("Optimal solution for these demands: " +printFlow(lpSolver.getArc2idVertex2nameAndValue()));

		}
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

