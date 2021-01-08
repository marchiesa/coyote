package lpsolver.test;

import experiments.racke.MainRacke;
import gurobi.GRBException;
import inputoutput.RocketFuelGraphReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformance;
import lpsolver.LPSolverOptimalObliviousForcingPaths;
import model.Arc;
import model.Graph;
import util.Pair;

public class LPSolverOptimalObliviousForcingPahtsTest {

	public static void main(String[] args) throws IOException, GRBException{
//		String file="topologies/Backbone/Deltacom.lgf";
		String file=args[0];	

		//Graph g = new SimpleToyGraphReader().readUndirectedGraph(file);
		Graph g = new RocketFuelGraphReader().readUndirectedGraph(file);
		System.out.println(g);

		LPSolverOptimalObliviousForcingPaths lsoo = new LPSolverOptimalObliviousForcingPaths();
		Map<Pair<Integer>,List<List<Arc>>> demand2listoOfPaths = new HashMap<Pair<Integer>,List<List<Arc>>>();
		Pair<Integer> demand = new Pair<Integer>(0,1);
		demand2listoOfPaths.put(demand, new LinkedList<List<Arc>>());
		List<Arc> path1 = new LinkedList<Arc>();
		path1.add(g.getArcById(0));
		demand2listoOfPaths.get(demand).add(path1);
		/*List<Arc> path2 = new LinkedList<Arc>();
		path2.add(g.getArcById(2));
		path2.add(g.getArcById(5));
		demand2listoOfPaths.get(demand).add(path2);
		demand = new Pair<Integer>(0,2);
		demand2listoOfPaths.put(demand, new LinkedList<List<Arc>>());
		path1 = new LinkedList<Arc>();
		path1.add(g.getArcById(2));
		demand2listoOfPaths.get(demand).add(path1);
		path2 = new LinkedList<Arc>();
		path2.add(g.getArcById(0));
		path2.add(g.getArcById(4));
		demand2listoOfPaths.get(demand).add(path2);
		demand = new Pair<Integer>(2,1);
		demand2listoOfPaths.put(demand, new LinkedList<List<Arc>>());
		path1 = new LinkedList<Arc>();
		path1.add(g.getArcById(5));
		demand2listoOfPaths.get(demand).add(path1);
		path2 = new LinkedList<Arc>();
		path2.add(g.getArcById(3));
		path2.add(g.getArcById(0));
		demand2listoOfPaths.get(demand).add(path2);*/
		
		/*List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		for(Vertex v : g.getVertices()){
			for(Vertex w : g.getVertices()){
				if(v.getId()!=w.getId()){
					setOfDemands.add(new Pair<Integer>(v.getId(),w.getId()));
				}
			}
		}*/
		//setOfDemands.add(new Pair<Integer>(0,1));
		lsoo.computeOptimalPerformance(g, demand2listoOfPaths);
		CompactLPSolverObliviousPerformance lps = new CompactLPSolverObliviousPerformance();
		lps.computeObliviousPerformance(g, lsoo.getArc2demand2fraction());
		System.out.println("Oblivious performance: " + lps.getOpt());
		System.out.println("Demands: " + MainRacke.printDemands(lps.getDemand2nameAndValue()));
		//System.out.println("Sol: " + printObliviousFlow(lsoo.getArc2demand2nameAndValue()));
		
		
		System.out.println("Min:"+ lsoo.getMin());
		
	}
	
	protected static String printObliviousFlow(
			Map<Integer, Map<Pair<Integer>, Pair<String>>> arc2demand2nameAndValue) {
		String result="";
		for(Integer idArc : arc2demand2nameAndValue.keySet()){
			boolean haveOnePositive = false;
			for(Pair<Integer> demand: arc2demand2nameAndValue.get(idArc).keySet() ){
				if(Double.parseDouble(arc2demand2nameAndValue.get(idArc).get(demand).getSecond())>0){
					if(!haveOnePositive){
						haveOnePositive=true;
						result+="\n\t";
					}
					result+=arc2demand2nameAndValue.get(idArc).get(demand).getFirst()+":"+arc2demand2nameAndValue.get(idArc).get(demand).getSecond()+", ";
				}
			} 
		}
		return result;
	}
}
