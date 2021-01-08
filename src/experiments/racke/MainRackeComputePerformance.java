package experiments.racke;


import gurobi.GRBException;
import inputoutput.RocketFuelGraphReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformanceGivenDemand;
import model.Graph;
import model.Vertex;
import obliviousrouting.RackeObliviousRoutingSolver;
import obliviousrouting.mcct.Tree;
import performance.trafficmatrix.GravityModelCalculator;
import util.MixedTriplet;
import util.Pair;

/**
 * Entry point to run Racke's algorithm
 *  - 1st argument is a path to a file containing a topology
 * @author mosfet
 *
 */
public class MainRackeComputePerformance {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, GRBException, ClassNotFoundException{
		//		String file="topologies/Backbone/Deltacom.lgf";
		String file=args[0];
		Double gap=Double.parseDouble(args[1]);
		Double howmany=Double.parseDouble(args[2]);

		long startTime = System.nanoTime();

		// read graph
		Graph g = new RocketFuelGraphReader().readUndirectedGraph(file);
		//Graph g = new SimpleToyGraphReader().readUndirectedGraph("input.txt");
		//Graph g = new CliqueGenerator().createClique(20);
		System.out.println(g);
		
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		for(Vertex from: g.getVertices())
			for(Vertex to : g.getVertices())
				if(from.getId()!=to.getId())
					setOfDemands.add(new Pair<Integer>(from.getId(),to.getId()));

		FileInputStream fileIn = new FileInputStream(file.split("\\.")[0].split("/")[2]+"-solution");
		ObjectInputStream in = new ObjectInputStream(fileIn);
		Map<Integer,Map<Pair<Integer>,Double>> solution= (Map<Integer,Map<Pair<Integer>,Double>>) in.readObject();
		in.close();
		fileIn.close();

		CompactLPSolverObliviousPerformanceGivenDemand lpSolver = new CompactLPSolverObliviousPerformanceGivenDemand();
		//CompactLPSolverObliviousPerformance lpSolver = new CompactLPSolverObliviousPerformance();
		Map<Pair<Integer>,Double> trafficMatrix = new GravityModelCalculator().computeTrafficMatrix(g, setOfDemands);
		System.out.println(file.split("\\.")[0].split("/")[2]+"-gravity.txt");
		File file2 = new File(file.split("\\.")[0].split("/")[2]+"-gravity.txt");
		PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
		for(int w=1;w<=howmany;w+=gap){
			lpSolver.computeObliviousPerformance(g,setOfDemands, solution,trafficMatrix,w);
			//lpSolver.computeObliviousPerformance(g, solution);
			writer.println(w+" "+lpSolver.getOpt());
			System.out.println(w+" "+lpSolver.getOpt());
		}
		writer.flush();
		writer.close();
		System.out.println("Execution time:"+(System.nanoTime() - startTime));

		
	}

	/*
	 * Sort all trees contained in 'rors' by decreasing values of lambda.
	 * @param rors - a routing oblivious routing solver object
	 */
	@SuppressWarnings("unused")
	private static void sortLambdasInDecreasingOrder(RackeObliviousRoutingSolver rors) {
		int i=0;
		List<MixedTriplet<Double,Tree,Graph>> double2index = new LinkedList<MixedTriplet<Double,Tree,Graph>>();
		for(Double lambda:rors.getLambda()){
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

