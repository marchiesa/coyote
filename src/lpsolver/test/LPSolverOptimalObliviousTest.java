package lpsolver.test;

import experiments.racke.MainRacke;
import gurobi.GRBException;
import inputoutput.RocketFuelGraphReader;

import java.io.IOException;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformance;
import lpsolver.LPSolverOptimalOblivious;
import model.Graph;
import util.Pair;
public class LPSolverOptimalObliviousTest {

	public static void main(String[] args) throws IOException, GRBException{
//		String file="topologies/Backbone/Deltacom.lgf";
		String file=args[0];	

		//Graph g = new SimpleToyGraphReader().readUndirectedGraph(file);
		Graph g = new RocketFuelGraphReader().readUndirectedGraph(file);
		System.out.println(g);

		LPSolverOptimalOblivious ls = new LPSolverOptimalOblivious();
		//setOfDemands.add(new Pair<Integer>(0,1));
		ls.computeOptimalPerformance(g);
		CompactLPSolverObliviousPerformance lps = new CompactLPSolverObliviousPerformance();
		lps.computeObliviousPerformance(g, ls.getArc2demand2fraction());
		System.out.print("Oblivious Solution: " + MainRacke.print(ls.getArc2demand2fraction(),g,lps.getDemand2nameAndValue()));
		System.out.println("Oblivious performance: " + lps.getOpt());
		System.out.println("Demands: " + MainRacke.printDemands(lps.getDemand2nameAndValue()));
		//System.out.println("Sol: " + printObliviousFlow(lsoo.getArc2demand2nameAndValue()));
		
		
		System.out.println("Min:"+ ls.getOpt());
		
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
