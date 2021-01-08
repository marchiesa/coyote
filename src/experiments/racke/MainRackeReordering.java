package experiments.racke;


import gurobi.GRBException;
import inputoutput.RackeSolutionTransform;
import inputoutput.RocketFuelGraphReader;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformance;
import model.Graph;
import obliviousrouting.RackeObliviousRoutingSolver;
import obliviousrouting.mcct.Tree;
import util.MixedTriplet;
import util.Pair;

/**
 * Entry point to run Racke's algorithm
 *  - 1st argument is a path to a file containing a topology
 * @author mosfet
 *
 */
public class MainRackeReordering {

	public static void main(String[] args) throws IOException, GRBException{
//		String file="topologies/Backbone/Deltacom.lgf";
		String file=args[0];	

		long startTime = System.nanoTime();
				
		// read graph
		Graph g = new RocketFuelGraphReader().readUndirectedGraph(file);
		//Graph g = new SimpleToyGraphReader().readUndirectedGraph("input.txt");
		//Graph g = new CliqueGenerator().createClique(20);
		System.out.println(g);
		
		// compute trees and lambdas with Racke's algorithm.
		RackeObliviousRoutingSolver rors = new RackeObliviousRoutingSolver();
		rors.setGraph(g);
		System.out.println("---- COMPUTING TREES ----");
		rors.createTreesAndLambda();

		// sort lambdas by decreasing value
		sortLambdasInDecreasingOrder(rors);
		
		// this is a map that stores the oblivious performance of the first 'key' trees
		Map<Integer,Double> numberOfTrees2obliviousPerformances = new HashMap<Integer,Double>(); 

		System.out.println("lambdas: " + rors.getLambda());
		System.out.println();
		
		
		//compute oblivious routing by combining the first i trees, with i=1,...,#number_of_trees
		System.out.println("---- OBLIVIOUS ROUTING SOLUTIONS ----");
		RackeSolutionTransform rso = new RackeSolutionTransform();
		Map<Integer,Map<Pair<Integer>,Double>> solution;
		double sumOfLambdas =0;
		for(int i=0;i<rors.getTrees().size();i++){
			System.out.println("Number of Trees: "+(i+1));
			sumOfLambdas+=rors.getLambda().get(i);
			Double newLambda= rors.getLambda().get(i)/sumOfLambdas;
			System.out.println("newLambda:"+newLambda);

			// compute new routing by adding another tree
			solution = rso.addATree(rors.getTrees().get(i), newLambda, rors.getGraphs().get(i));
			System.out.println("Mapping["+(i)+"](arcs):"+rso.getDecTree2notSpanningTrees().get(i));
			
			// compute oblivious performance
			CompactLPSolverObliviousPerformance lpSolver = new CompactLPSolverObliviousPerformance();
			lpSolver.computeObliviousPerformance(rors.getGraph(), solution);
			System.out.println("Lambdas: " + rors.getLambda());
			System.out.print("Oblivious Solution: " + print(solution,g,lpSolver.getDemand2nameAndValue()));
			System.out.println("Oblivious performance: " + lpSolver.getOpt());
			numberOfTrees2obliviousPerformances.put(i+1, lpSolver.getOpt());
			System.out.println("Most congested arc: " + rors.getGraph().getArcById(lpSolver.getMostCongestedIdArc()));
			System.out.println("Demands: " + printDemands(lpSolver.getDemand2nameAndValue()));
			System.out.println("Optimal solution for these demands: " +printFlow(lpSolver.getArc2idVertex2nameAndValue()));
			System.out.println();
		}

		// print final results
		System.out.println();
		System.out.println("---- OBLIVIOUS ROUTING PERFORMANCE WRT TO # TREES ----");
		for(int i=1;i<=numberOfTrees2obliviousPerformances.size();i++){
			System.out.println("#trees: " + i + " obl-perf: "+numberOfTrees2obliviousPerformances.get(i));
		}

		System.out.println("Execution time:"+(System.nanoTime() - startTime));

	}
	
	/*
	 * Sort all trees contained in 'rors' by decreasing values of lambda.
	 * @param rors - a routing oblivious routing solver object
	 */
	private static void sortLambdasInDecreasingOrder(RackeObliviousRoutingSolver rors) {
		int i=0;
		List<MixedTriplet<Double,Tree,Graph>> double2index = new LinkedList<MixedTriplet<Double,Tree,Graph>>();
		for(Double lambda:rors.getLambda()){
			rors.getTrees().get(i).setId(i);
			double2index.add(new MixedTriplet<Double,Tree,Graph>(lambda,rors.getTrees().get(i),rors.getGraphs().get(i)));
			i++;
		}
		Collections.sort(double2index);
		Collections.reverse(double2index);
		rors.getTrees().clear();
		rors.getLambda().clear();
		rors.getGraphs().clear();
		for(MixedTriplet<Double,Tree,Graph> triplet: double2index){
			rors.getLambda().add((Double)triplet.getFirst());
			rors.getTrees().add((Tree)triplet.getSecond());
			rors.getGraphs().add((Graph)triplet.getThird());
			System.out.println("lambda:"+triplet.getFirst());
			System.out.println("tree:"+triplet.getSecond());
		}
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
			Map<Integer, Map<Pair<Integer>, Double>> solution, Graph g, Map<Pair<Integer>,Pair<String>> demands) {
		String result="\n";
		Map<Pair<Integer>, Map<Integer, Double>> demand2arc2fraction = new HashMap<Pair<Integer>, Map<Integer, Double>>();
		for(Integer idArc : solution.keySet()){
			for(Pair<Integer> demand : solution.get(idArc).keySet()){
				if(demands.get(demand)==null || demands.get(demand).getSecond().equals("0.0"))
					continue;
				if(solution.get(idArc).get(demand)==0.0d)
					continue;
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

