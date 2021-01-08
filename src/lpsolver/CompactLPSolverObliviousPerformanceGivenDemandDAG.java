package lpsolver;
/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

/**
 * This class is responsible of computing the oblivious performance of a routing using the compact LP formulation
 * of Applegate & Cohen.
 * @author mosfet
 *
 */
public class CompactLPSolverObliviousPerformanceGivenDemandDAG {

	private double max = Double.MIN_VALUE;
	private double lambdaVal = Double.MIN_VALUE;
	private boolean AGGREGATE_CONGESTION = false;
	private Integer mostCongestedIdArc = null;
	private Map<Pair<Integer>,Pair<String>> demand2nameAndValue;
	Map<Integer,Map<Integer,Pair<String>>> arc2idVertex2nameAndValue;
	private Map<Integer,Double> arc2rLoad;
	private GRBVar lambda;

	public double getLambda() {
		return lambdaVal;
	}

	public void computeObliviousPerformance(Graph g, List<Pair<Integer>> setOfDemands,Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction, Map<Integer,Graph> destination2ht){
		this.computeObliviousPerformance(g, setOfDemands,arc2demand2fraction, null,1, destination2ht, false);
	}

	public void computeObliviousPerformance(Graph g, List<Pair<Integer>> setOfDemands, Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction,
			Map<Pair<Integer>,Double> demand2estimate, double w, Map<Integer,Graph> destination2ht){
		this.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, demand2estimate, w, destination2ht, false);
	}

	/**
	 * Compute the oblivious performance of the routing in input and store it in local variables.  
	 * @param g, input graph
	 * @param arc2demand2fraction, oblivious routing  
	 */
	public void computeObliviousPerformance(Graph g, List<Pair<Integer>> setOfDemands, Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction,
			Map<Pair<Integer>,Double> demand2estimate, double w, Map<Integer,Graph> destination2ht, boolean AGGREGATE_CONGESTION){
		this.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, demand2estimate, w, destination2ht, AGGREGATE_CONGESTION, null,0d);
	}
		
	public void computeObliviousPerformance(Graph g,
				List<Pair<Integer>> setOfDemands,
				Map<Integer, Map<Pair<Integer>, Double>> arc2demand2fraction,
				Map<Pair<Integer>, Double> demand2estimate, double w,
				Map<Integer, Graph> destination2ht, boolean AGGREGATE_CONGESTION,
				Arc arcWReducedCapacity, double scalingFactor) {
	

		this.arc2rLoad= new HashMap<Integer,Double>();
		this.max = Double.MIN_VALUE;
		try {
			/*if(arc2demand2fraction==null){
				arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
				for(Arc arc:g.getArcs())
					arc2demand2fraction.put(arc.getId(), null);
			}*/
			for(Integer idArc : arc2demand2fraction.keySet()){
				if(AGGREGATE_CONGESTION && (idArc % 2 == 1))
					continue;
				Arc arc = g.getArcById(idArc);
				//if(arc.getFirstEndPoint().getId()>arc.getSecondEndPoint().getId())
				//	continue;
				GRBEnv env = new GRBEnv();
				env.set(GRB.IntParam.OutputFlag,0);
				GRBModel model = new GRBModel(env);
				model.set(GRB.StringAttr.ModelName, "maximize_edge_"+"("+arc.getId()+"-"+arc.getFirstEndPoint()+","+arc.getSecondEndPoint()+")");

				Set<Integer> destinationVertices = new TreeSet<Integer>();
				//if(arc.getFirstEndPoint().getId()>arc.getSecondEndPoint().getId())
				//	continue;

				// CREATE DEMANDS VARIABLES d_(s,t)
				//Map<Pair<Integer>,Double> demand2cost = new HashMap<Pair<Integer>,Double>();
				Map<Pair<Integer>,GRBVar> demand2var = new HashMap<Pair<Integer>,GRBVar>();
				//maximize congestion at a specific arc
				Map<Pair<Integer>,Double> demand2fractionB = arc2demand2fraction.get(arc.getId());
				if(demand2fractionB==null){
					this.arc2rLoad.put(arc.getId(), 0d);
					continue;
				}
				boolean print = false;
				//print = true;
				Map<Pair<Integer>,Double> demand2fractionBReversed=null;
				if(AGGREGATE_CONGESTION){
					demand2fractionBReversed = arc2demand2fraction.get(g.getReversedArc(arc.getId()).getId());
					if(demand2fractionBReversed==null){
						this.arc2rLoad.put(g.getReversedArc(arc.getId()).getId(), 0d);
						continue;
					}
				}
				//if(idArc == 0)
				//print = true;
				if(print)System.out.print("Demand variablesarc:"+arc+":[");
				for(Pair<Integer> demand: setOfDemands){
					//for(Vertex vertex: g.getVertices()){
					//	for(Vertex vertex2: g.getVertices()){
					//		if(vertex.getId()==vertex2.getId())
					//			continue;

					//		Pair<Integer> demand = new Pair<Integer>(vertex.getId(),vertex2.getId());
					Double cost = demand2fractionB.get(demand);
					if(cost == null){
						//cost = demand2fractionC.get(demand);
						//if(cost == null)
						cost =0d;
					}
					if(AGGREGATE_CONGESTION && cost==0){
						cost = demand2fractionBReversed.get(demand);
						if(cost == null){
							//cost = demand2fractionC.get(demand);
							//if(cost == null)
							cost =0d;
						}
					}

					//if(cost>0){
					if(print)System.out.print("("+getString(demand)+","+(cost)/arc.getCapacity()+") ");

					demand2var.put(demand,model.addVar(0,GRB.INFINITY, (cost)/arc.getCapacity(), GRB.CONTINUOUS,getString(demand)));
					destinationVertices.add(demand.getSecond());
					//}

				}
				if(print)System.out.println();
				/*for(Pair<Integer> demand: demand2fractionB.keySet()){
					if(demand.getFirst()>demand.getSecond())
						continue;
					Double cost = demand2fractionB.get(demand);
					//demand2cost.put(demand, cost);
					demand2var.put(demand,model.addVar(0,GRB.INFINITY, cost/arc.getCapacity(), GRB.CONTINUOUS,getString(demand)));
					System.out.print("("+getString(demand)+","+cost/arc.getCapacity()+") ");
				}*/
				//System.out.println("]");

				Map<Integer,Map<Integer,GRBVar>> arc2idVertex2var = new HashMap<Integer,Map<Integer,GRBVar>>();

				// CREATE FLOW VARIABLE g_e^i
				//Map<Vertex,Map<Pair<Integer>,Map<Arc,>>>
				//System.out.println("flow variables:[");
				for(Vertex source: g.getVertices()){
					for(Vertex destination: g.getVertices()){
						if(!destinationVertices.contains(destination.getId()))
							continue;
						for(Arc outgoingArc : source.getArcs()){
							if(destination2ht.get(destination.getId()).getArcById(outgoingArc.getId())!=null){
								Map<Integer,GRBVar> idVertex2varB = arc2idVertex2var.get(outgoingArc.getId());
								if(idVertex2varB==null){
									idVertex2varB = new HashMap<Integer,GRBVar>();
									arc2idVertex2var.put(outgoingArc.getId(), idVertex2varB);
								}
								idVertex2varB.put(destination.getId(),model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,getString(outgoingArc,destination.getId())));
							}
							Arc reversedArc = g.getReversedArc(outgoingArc.getId());
							if(destination2ht.get(destination.getId()).getArcById(reversedArc.getId())!=null){
								Map<Integer,GRBVar> idVertex2varB = arc2idVertex2var.get(reversedArc.getId());
								if(idVertex2varB==null){
									idVertex2varB = new HashMap<Integer,GRBVar>();
									arc2idVertex2var.put(reversedArc.getId(), idVertex2varB);
								}

								idVertex2varB.put(destination.getId(),model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,getString(reversedArc,destination.getId())));
							}
						} 
					}
				}

				// CREATE alpha VARIABLE
				this.lambda= model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"lambda");


				// The objective is to maximize congestion
				model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

				// Update model to integrate new variables
				model.update();

				//add flow constraints per vertex
				if(print)System.out.println("flow constraints:[");
				for(Vertex source: g.getVertices()){
					for(Vertex destination: g.getVertices()){
						if(!destinationVertices.contains(destination.getId()))
							continue;

						//if(source.getId()>destination.getId())
						//	continue;
						if(destination.getId()==source.getId())
							continue;
						if(print)System.out.print("\t"+source.getId()+" -> "+destination.getId()+" [");

						// flow conservation constraint at vertex for demand 
						GRBLinExpr flows = new GRBLinExpr();
						//System.out.print("\t");
						for(Arc outgoingArc : source.getArcs()){
							if(destination2ht.get(destination.getId()).getArcById(outgoingArc.getId())!=null){
								flows.addTerm(-1, arc2idVertex2var.get(outgoingArc.getId()).get(destination.getId()));
								if(print)
									System.out.print("- "+arc2idVertex2var.get(outgoingArc.getId()).get(destination.getId()).get(GRB.StringAttr.VarName)+" ");
							}
							Arc reversedArc = g.getReversedArc(outgoingArc.getId());
							if(destination2ht.get(destination.getId()).getArcById(reversedArc.getId())!=null){
								flows.addTerm(+1, arc2idVertex2var.get(reversedArc.getId()).get(destination.getId()));
								if(print)
									System.out.print(" + "+arc2idVertex2var.get(reversedArc.getId()).get(destination.getId()).get(GRB.StringAttr.VarName)+" ");
							}
						} 
						// flow conservation constraint at vertex for demand 
						if(demand2var.get(new Pair<Integer>(source.getId(),destination.getId()))!=null){
							flows.addTerm(1, demand2var.get(new Pair<Integer>(source.getId(),destination.getId())));
							if(print)System.out.print("+"+demand2var.get(new Pair<Integer>(source.getId(),destination.getId())).get(GRB.StringAttr.VarName));
						}
						model.addConstr(flows, GRB.EQUAL, 0,null);
						if(print)System.out.println(" = 0");
					}
				}

				//add capacity constraints per edge
				//System.out.println("edge constraints:[");
				for(Arc arc2 : g.getArcs()){
					if(AGGREGATE_CONGESTION && (arc2.getId() % 2 == 1))
						continue;	
					//if(arc2.getFirstEndPoint().getId()>arc2.getSecondEndPoint().getId())
					//	continue;
					if(print)System.out.print("\t ("+arc2.getFirstEndPoint().getId()+","+arc2.getSecondEndPoint().getId()+") [");

					GRBLinExpr flows = new GRBLinExpr();
					boolean atLeastOne = false;
					for(Integer destinationId: destinationVertices){
//						Vertex destination = g.getVertexById(destinationId);
						if(destination2ht.get(destinationId).getArcById(arc2.getId())!=null){
							atLeastOne = true;
							flows.addTerm(1, arc2idVertex2var.get(arc2.getId()).get(destinationId));
							//Arc reversedArc = g.getReversedArc(arc2.getId());
							//flows.addTerm(1, arc2idVertex2var.get(reversedArc.getId()).get(source.getId()));
							if(print)System.out.print("+ "+arc2idVertex2var.get(arc2.getId()).get(destinationId).get(GRB.StringAttr.VarName)+
									"  ");
						}
						if(AGGREGATE_CONGESTION && destination2ht.get(destinationId).getArcById(g.getReversedArc(arc2.getId()).getId())!=null){
							atLeastOne = true;
							flows.addTerm(1, arc2idVertex2var.get(g.getReversedArc(arc2.getId()).getId()).get(destinationId));
							//Arc reversedArc = g.getReversedArc(arc2.getId());
							//flows.addTerm(1, arc2idVertex2var.get(reversedArc.getId()).get(source.getId()));
							if(print)System.out.print("+ " +  arc2idVertex2var.get(g.getReversedArc(arc2.getId()).getId()).get(destinationId).get(GRB.StringAttr.VarName)+
									"  ");
						}

					}
					if(atLeastOne){
						double capacity = arc2.getCapacity();
						//if(arcWReducedCapacity==null)
						//	capacity = capacity/scalingFactor;
						if(print)System.out.println(" <= "+capacity);
						model.addConstr(flows, GRB.LESS_EQUAL, capacity,null);
					}else{
						if(print)System.out.println(" nothing" );
					}
				}

				for(Pair<Integer> demand: setOfDemands){
					GRBLinExpr flows = new GRBLinExpr();

					flows.addTerm(1, demand2var.get(demand));

					flows.addTerm(-demand2estimate.get(demand)*w, lambda);

					model.addConstr(flows, GRB.LESS_EQUAL, 0,null);
					if(print)System.out.println("\t "+demand + " -> d_{" + demand.getFirst() +","+demand.getSecond()+"} -" + demand2estimate.get(demand)*w + "L <=0");

					flows = new GRBLinExpr();

					flows.addTerm(-1, demand2var.get(demand));

					flows.addTerm(demand2estimate.get(demand)/w, lambda);

					model.addConstr(flows, GRB.LESS_EQUAL, 0,null);
					if(print)System.out.println("\t "+demand + " -> - d_{" + demand.getFirst() +","+demand.getSecond()+"} +" + demand2estimate.get(demand)/w + "L <=0");
				}

				// Solve 
				if(print)System.out.println("Solving...");
				model.optimize();
				if(print)System.out.println("Solved!" + (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) + " value:"+model.get(GRB.DoubleAttr.ObjVal));


				if(model.get(GRB.IntAttr.Status) != GRB.OPTIMAL){
					continue;
				}
				model.get(GRB.DoubleAttr.ObjVal);

				this.arc2rLoad.put(idArc, model.get(GRB.DoubleAttr.ObjVal));

				// stores the output of the lp program
				if(this.max<model.get(GRB.DoubleAttr.ObjVal)){
					this.max = model.get(GRB.DoubleAttr.ObjVal);
					this.mostCongestedIdArc = arc.getId();
					this.lambdaVal = this.lambda.get(GRB.DoubleAttr.X);
					/*this.demand2nameAndValue = new HashMap<Pair<Integer>,Pair<String>>();
					for(Pair<Integer> demand : demand2var.keySet()){
						this.demand2nameAndValue.put(demand, new Pair<String>(demand2var.get(demand).get(GRB.StringAttr.VarName),
								demand2var.get(demand).get(GRB.DoubleAttr.X)+""));
					}
					this.arc2idVertex2nameAndValue = new HashMap<Integer,Map<Integer,Pair<String>>>();
					for(Integer idArc2 : arc2idVertex2var.keySet()){
						Map<Integer,Pair<String>>  idVertex2nameAndValue = new HashMap<Integer,Pair<String>>();
						this.arc2idVertex2nameAndValue.put(idArc2, idVertex2nameAndValue);
						for(Vertex vertex : g.getVertices()){
							idVertex2nameAndValue.put(vertex.getId(), new Pair<String>(arc2idVertex2var.get(idArc2).get(vertex.getId()).get(GRB.StringAttr.VarName),
									arc2idVertex2var.get(idArc2).get(vertex.getId()).get(GRB.DoubleAttr.X)+""));
						}
					}
					this.lambdaVal = this.lambda.get(GRB.DoubleAttr.X);*/
				}


				model.dispose();
				env.dispose();
			}


		} catch (Exception e) {
			System.out.println("Error code: " + e.getMessage());
		}
	}



	private String getString(Arc arc, Integer idVertex) {
		return "g_("+arc.getFirstEndPoint()+","+arc.getSecondEndPoint()+")^("+idVertex+")";
	}



	private String getString(Pair<Integer> pair) {
		return "d_("+pair.getFirst()+","+pair.getSecond()+")";
	}


	public static void printSolution(GRBModel model, GRBVar[] buy,
			GRBVar[] nutrition) throws GRBException {
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
			System.out.println("\nBuy:");
			for (int j = 0; j < buy.length; ++j) {
				if (buy[j].get(GRB.DoubleAttr.X) > 0.0001) {
					System.out.println(buy[j].get(GRB.StringAttr.VarName) + " " +
							buy[j].get(GRB.DoubleAttr.X));
				}
			}
			System.out.println("\nNutrition:");
			for (int i = 0; i < nutrition.length; ++i) {
				System.out.println(nutrition[i].get(GRB.StringAttr.VarName) + " " +
						nutrition[i].get(GRB.DoubleAttr.X));
			}
		} else {
			System.out.println("No solution");
		}
	}

	public double getOpt() {
		return max;
	}


	public void setMax(double max) {
		this.max = max;
	}


	public Integer getMostCongestedIdArc() {
		return mostCongestedIdArc;
	}


	public void setMostCongestedIdArc(Integer mostCongestedIdArc) {
		this.mostCongestedIdArc = mostCongestedIdArc;
	}


	public Map<Pair<Integer>, Pair<String>> getDemand2nameAndValue() {
		return demand2nameAndValue;
	}


	public void setDemand2nameAndValue(
			Map<Pair<Integer>, Pair<String>> demand2nameAndValue) {
		this.demand2nameAndValue = demand2nameAndValue;
	}


	public Map<Integer, Map<Integer, Pair<String>>> getArc2idVertex2nameAndValue() {
		return arc2idVertex2nameAndValue;
	}


	public void setArc2idVertex2nameAndValue(
			Map<Integer, Map<Integer, Pair<String>>> arc2demand2nameAndValue) {
		this.arc2idVertex2nameAndValue = arc2demand2nameAndValue;
	}

	public Map<Integer, Double> getArc2rLoad() {
		return arc2rLoad;
	}

	public double getLambdaVal() {
		return lambdaVal;
	}






	/*	public void printSolution(GRBModel model)  {
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
			System.out.println("\nBuy:");
			for (int j = 0; j < buy.length; ++j) {
				if (buy[j].get(GRB.DoubleAttr.X) > 0.0001) {
					System.out.println(buy[j].get(GRB.StringAttr.VarName) + " " +
							buy[j].get(GRB.DoubleAttr.X));
				}
			}
			System.out.println("\nNutrition:");
			for (int i = 0; i < nutrition.length; ++i) {
				System.out.println(nutrition[i].get(GRB.StringAttr.VarName) + " " +
						nutrition[i].get(GRB.DoubleAttr.X));
			}
		} else {
			System.out.println("No solution");
		}
	}*/
}