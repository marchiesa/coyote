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

public class LPSolverOptimalOblivious {

	private boolean AGGREGATE_CONGESTION = true;
	private double min = Double.MIN_VALUE;
	private Integer mostCongestedIdArc = null; 
	Map<Integer,Map<Pair<Integer>,Pair<String>>> arc2demand2nameAndValue;
	Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction;

	private Map<Integer,Map<Integer,Map<Integer,GRBVar>>> pi_edge2vertex2vertex2var;
	private Map<Integer,Map<Integer,GRBVar>>  w_edge2edge2var;
	private Map<Integer,Map<Pair<Integer>,GRBVar>> x_edge2demand2var;
	private GRBVar alpha;

	public void computeOptimalPerformance(Graph g){
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		for(Vertex v : g.getVertices()){
			for(Vertex w : g.getVertices()){
				if(v.getId()!=w.getId()){
					setOfDemands.add(new Pair<Integer>(v.getId(),w.getId()));
				}
			}
		}
		//setOfDemands.add(new Pair<Integer>(0,5));
		//setOfDemands.add(new Pair<Integer>(4,2));
		this.computeOptimalPerformance(g,setOfDemands);
	}
	
	public void computeOptimalPerformance(Graph g,List<Pair<Integer>> setOfDemands ){
		this.computeOptimalPerformance(g, setOfDemands,null);
	}

	public void computeOptimalPerformance(Graph g,Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction ){
		Set<Pair<Integer>> setOfDemands = new TreeSet<Pair<Integer>>();
		for(Integer idArc:arc2demand2fraction.keySet()){
			setOfDemands.addAll(arc2demand2fraction.get(idArc).keySet());
		}
		this.computeOptimalPerformance(g, new LinkedList<Pair<Integer>>(setOfDemands),arc2demand2fraction);
	}


	public void computeOptimalPerformance(Graph g,List<Pair<Integer>> setOfDemands, Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction ){

		try {
			GRBEnv env = new GRBEnv();
			env.set(GRB.IntParam.OutputFlag,0);
			env.set(GRB.IntParam.Method, 2);
			env.set(GRB.IntParam.Crossover, 0);
			env.set(GRB.DoubleParam.BarConvTol, 0.01d);
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "minimize_oblivious_performance");
			
			//System.out.println("bella");

			Set<Integer> sourceVertices = new TreeSet<Integer>();
			for(Pair<Integer> demand : setOfDemands){
				sourceVertices.add(demand.getFirst());
			}

			// CREATE PI VARIABLES pi_e^(i,j)
			this.pi_edge2vertex2vertex2var = new HashMap<Integer,Map<Integer,Map<Integer,GRBVar>>>();

			for(Arc arc:g.getArcs()){
				if(AGGREGATE_CONGESTION && (arc.getId() % 2 == 1))
					continue;
				Map<Integer,Map<Integer,GRBVar>> vertex2vertex2var = new HashMap<Integer,Map<Integer,GRBVar>>();
				pi_edge2vertex2vertex2var.put(arc.getId(), vertex2vertex2var);

				for(Integer i: sourceVertices){
					Map<Integer,GRBVar> vertex2var = new HashMap<Integer,GRBVar>();
					vertex2vertex2var.put(i, vertex2var);
					for(Vertex j: g.getVertices()){
						if(i==j.getId())
							continue;
						vertex2var.put(j.getId(), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"pi_"+arc.toStringShort()+"^("+i+","+j+")"));
						//System.out.print("pi_"+arc.toStringShort()+"^("+i+","+j+")"+" ");

					}
				}
				//System.out.println();
			}


			// CREATE W VARIABLES w_e^(i,j)
			this.w_edge2edge2var = new HashMap<Integer,Map<Integer,GRBVar>>();

			for(Arc arc:g.getArcs()){
				if(AGGREGATE_CONGESTION && (arc.getId() % 2 == 1))
					continue;
				Map<Integer,GRBVar> edge2var = this.w_edge2edge2var.get(arc.getId());
				if(edge2var==null){
					edge2var = new HashMap<Integer,GRBVar>();
					this.w_edge2edge2var.put(arc.getId(), edge2var);
				}
				for(Arc arc2:g.getArcs()){
					if(AGGREGATE_CONGESTION && (arc2.getId() % 2 == 1))
						continue;
					edge2var.put(arc2.getId(), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"w_"+arc.toStringShort()+"^"+arc2.toStringShort()));
					//System.out.print("w_"+arc.toStringShort()+"^("+arc2.toStringShort()+" ");
				}
				//System.out.println();
			}

			// CREATE X VARIABLES x_^(i,j)^(s,t)
			this.x_edge2demand2var = new HashMap<Integer,Map<Pair<Integer>,GRBVar>>();

			for(Arc arc:g.getArcs()){
				Map<Pair<Integer>,GRBVar> demand2var = this.x_edge2demand2var.get(arc.getId());
				if(demand2var==null){
					demand2var = new HashMap<Pair<Integer>,GRBVar>();
					this.x_edge2demand2var.put(arc.getId(), demand2var);
				}
				for(Pair<Integer> demand:setOfDemands){
					demand2var.put(demand, model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"));
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
					demand2var.put(new Pair<Integer>(demand.getSecond(),demand.getFirst()), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getSecond()+","+demand.getFirst()+")"));
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getSecond()+","+demand.getFirst()+")"+" ");
				}
			}
			//System.out.println();

			// CREATE alpha VARIABLE
			this.alpha = model.addVar(0,GRB.INFINITY, 1, GRB.CONTINUOUS,"alpha");

			// The objective is to maximize congestion
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

			// Update model to integrate new variables
			model.update();

			//add flow constraints per vertex
			//System.out.println("Flow Constraints");
			for(Vertex vertex: g.getVertices()){
				//System.out.println("v:"+vertex+" ");
				for(Pair<Integer> demand : setOfDemands){
					//System.out.print("\t");
					// flow conservation constraint at vertex for demand 
					GRBLinExpr flows = new GRBLinExpr();

					if(vertex.getId()==demand.getFirst()){
						//System.out.println(" nothing.");
						continue;
					}

					for(Arc outgoingArc:vertex.getArcs()){
						Arc reversedArc = g.getReversedArc(outgoingArc.getId());
						if(arc2demand2fraction==null || ( arc2demand2fraction.get(outgoingArc.getId())!=null && arc2demand2fraction.get(outgoingArc.getId()).get(demand)!=null)){
							//System.out.print(" - "+this.x_edge2demand2var.get(outgoingArc.getId()).get(demand).get(GRB.StringAttr.VarName));
							flows.addTerm(-1, this.x_edge2demand2var.get(outgoingArc.getId()).get(demand));
						}
						if(arc2demand2fraction==null || ( arc2demand2fraction.get(reversedArc.getId())!=null && arc2demand2fraction.get(reversedArc.getId()).get(demand)!=null)){
							//System.out.print(" + "+this.x_edge2demand2var.get(reversedArc.getId()).get(demand).get(GRB.StringAttr.VarName));
							flows.addTerm(1, this.x_edge2demand2var.get(reversedArc.getId()).get(demand));
						}
					}
					if(vertex.getId()==demand.getSecond()){
						//System.out.println(" >= 1");
						model.addConstr(flows, GRB.GREATER_EQUAL, 1,null);
					}else{
						//System.out.println(" >= 0");
						model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
					}

					/*demand=new Pair<Integer>(demand.getSecond(),demand.getFirst());
					flows = new GRBLinExpr();

					if(vertex.getId()==demand.getSecond())
						continue;

					for(Arc outgoingArc:vertex.getArcs()){
						flows.addTerm(-1, this.x_edge2demand2var.get(outgoingArc.getId()).get(demand));
						Arc reversedArc = g.getReversedArc(outgoingArc.getId());
						flows.addTerm(1, this.x_edge2demand2var.get(reversedArc.getId()).get(demand));
					}
					if(vertex.getId()==demand.getSecond()){
						model.addConstr(flows, GRB.GREATER_EQUAL, 1,null);
					}else{
						model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
					}*/
				}
			}

			//System.out.println("First Dual Constraints");
			// first dual constraint from flow constraint
			for(Arc arcE: g.getArcs()){
				if(AGGREGATE_CONGESTION && (arcE.getId() % 2 == 1))
					continue;
				//System.out.println("e:"+arcE.toStringShort()+" ");
				for(Integer i: sourceVertices){
					//System.out.println("\t i:"+i+" ");
					Map<Integer,GRBVar> vertex2var = this.pi_edge2vertex2vertex2var.get(arcE.getId()).get(i);
					for(Arc arc2: g.getArcs()){
						//System.out.print("\t\t a:"+arc2.toStringShort()+" ");
						//pi_e^(ij) - pi_e^ik + w_e >= 0
						GRBLinExpr flows = new GRBLinExpr();
						if(vertex2var.get(arc2.getFirstEndPoint().getId())!=null){
							//System.out.print(" + "+vertex2var.get(arc2.getFirstEndPoint().getId()).get(GRB.StringAttr.VarName));
							flows.addTerm(1, vertex2var.get(arc2.getFirstEndPoint().getId()));
						}
						if(vertex2var.get(arc2.getSecondEndPoint().getId())!=null){
							//System.out.print(" - "+vertex2var.get(arc2.getSecondEndPoint().getId()).get(GRB.StringAttr.VarName));
							flows.addTerm(-1, vertex2var.get(arc2.getSecondEndPoint().getId()));
						}
						
						if(AGGREGATE_CONGESTION){
							//System.out.print(" + "+this.w_edge2edge2var.get(arcE.getId()).get(2*(arc2.getId()/2)).get(GRB.StringAttr.VarName));
							flows.addTerm(1, this.w_edge2edge2var.get(arcE.getId()).get(2*(arc2.getId()/2)));
						}
						else{
							//System.out.print(" + "+this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()).get(GRB.StringAttr.VarName));
							flows.addTerm(1, this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()));
						}
						//System.out.println(" >= 0");
						model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
					}
				}
			}

			//System.out.println("Second Dual Constraints");
			// second dual constraint from flow constraint
			for(Arc arcE: g.getArcs()){
				if(AGGREGATE_CONGESTION && (arcE.getId() % 2 == 1))
					continue;
				//System.out.println("e:"+arcE.toStringShort()+" ");
				Map<Integer,Map<Integer,GRBVar>> vertex2vertex2var = this.pi_edge2vertex2vertex2var.get(arcE.getId());
				for(Pair<Integer> demand: setOfDemands){
					if((arc2demand2fraction!=null && arc2demand2fraction.get(arcE.getId())!=null && arc2demand2fraction.get(arcE.getId()).get(demand)!=null ) ||
							arc2demand2fraction==null){

					//	System.out.print("\t d:"+demand+" ");
						Map<Integer,GRBVar> vertex2var = vertex2vertex2var.get(demand.getFirst());
						GRBLinExpr flows = new GRBLinExpr();
						//System.out.print(" + "+vertex2var.get(demand.getSecond()).get(GRB.StringAttr.VarName));
						flows.addTerm(1, vertex2var.get(demand.getSecond()));
						//System.out.print(" - "+this.x_edge2demand2var.get(arcE.getId()).get(new Pair<Integer>(demand.getFirst(),demand.getSecond())).get(GRB.StringAttr.VarName));
						flows.addTerm(-1, this.x_edge2demand2var.get(arcE.getId()).get(new Pair<Integer>(demand.getFirst(),demand.getSecond())));
						if(AGGREGATE_CONGESTION){
							//System.out.print(" - "+this.x_edge2demand2var.get(g.getReversedArc(arcE.getId()).getId()).get(new Pair<Integer>(demand.getFirst(),demand.getSecond())).get(GRB.StringAttr.VarName));
							flows.addTerm(-1, this.x_edge2demand2var.get(g.getReversedArc(arcE.getId()).getId()).get(new Pair<Integer>(demand.getFirst(),demand.getSecond())));
						}
						//System.out.println(" >= 0");
						model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
					}

				}
			}

			//System.out.println("Dual Optimization Constraints");
			// dual optimization function via constraint 
			for(Arc arcE: g.getArcs()){
				if(AGGREGATE_CONGESTION && (arcE.getId() % 2 == 1))
					continue;
				//System.out.print("e:"+arcE.toStringShort()+" ");
				GRBLinExpr flows = new GRBLinExpr();
				for(Arc arc2:g.getArcs()){
					if(AGGREGATE_CONGESTION && (arc2.getId() % 2 == 1))
						continue;
					//System.out.print(" - "+arc2.getCapacity()+"*"+this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()).get(GRB.StringAttr.VarName));
					flows.addTerm(-arc2.getCapacity(), this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()));
				}
				//System.out.print(" + "+arcE.getCapacity()+"*"+this.alpha.get(GRB.StringAttr.VarName));
				flows.addTerm(arcE.getCapacity(), this.alpha);

				//System.out.println(" >= 0");
				model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
			}

			System.out.println("solving...");
			model.optimize();
			System.out.println("solved!");
			
			model.get(GRB.DoubleAttr.ObjVal);

			this.min = model.get(GRB.DoubleAttr.ObjVal);

			this.arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
			this.arc2demand2nameAndValue = new HashMap<Integer,Map<Pair<Integer>,Pair<String>>>();
			for(Integer idArc2 : this.x_edge2demand2var.keySet()){
				Map<Pair<Integer>,Pair<String>>  idVertex2idVertex2nameAndValue = new HashMap<Pair<Integer>,Pair<String>>();
				Map<Pair<Integer>,Double>  idVertex2idVertex2fraction = new HashMap<Pair<Integer>,Double>();
				this.arc2demand2nameAndValue.put(idArc2, idVertex2idVertex2nameAndValue);
				this.arc2demand2fraction.put(idArc2, idVertex2idVertex2fraction);
				for(Pair<Integer> demand: this.x_edge2demand2var.get(idArc2).keySet()){
					idVertex2idVertex2nameAndValue.put(
							demand, new Pair<String>(""+this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.StringAttr.VarName),
									this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.DoubleAttr.X)+""));
					idVertex2idVertex2fraction.put(
							demand, this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.DoubleAttr.X));
				}
			}


			model.dispose();
			env.dispose();


		} catch (Exception e) {
			//System.out.println("Error code: " + e.getMessage());
		}
	}





	public static void printSolution(GRBModel model, GRBVar[] buy,
			GRBVar[] nutrition) throws GRBException {
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			//System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
			//System.out.println("\nBuy:");
			for (int j = 0; j < buy.length; ++j) {
				if (buy[j].get(GRB.DoubleAttr.X) > 0.0001) {
					//System.out.println(buy[j].get(GRB.StringAttr.VarName) + " " +	buy[j].get(GRB.DoubleAttr.X));
				}
			}
			//System.out.println("\nNutrition:");
			for (int i = 0; i < nutrition.length; ++i) {
				//System.out.println(nutrition[i].get(GRB.StringAttr.VarName) + " " + nutrition[i].get(GRB.DoubleAttr.X));
			}
		} else {
			//System.out.println("No solution");
		}
	}

	public double getOpt() {
		return min;
	}


	public void setMax(double max) {
		this.min = max;
	}


	public Integer getMostCongestedIdArc() {
		return mostCongestedIdArc;
	}


	public void setMostCongestedIdArc(Integer mostCongestedIdArc) {
		this.mostCongestedIdArc = mostCongestedIdArc;
	}


	public Map<Integer, Map<Pair<Integer>, Double>> getArc2demand2fraction() {
		return arc2demand2fraction;
	}



	public void setArc2demand2fraction(
			Map<Integer, Map<Pair<Integer>, Double>> arc2demand2fraction) {
		this.arc2demand2fraction = arc2demand2fraction;
	}



	public Map<Integer, Map<Pair<Integer>, Pair<String>>> getArc2demand2nameAndValue() {
		return arc2demand2nameAndValue;
	}



	public void setArc2demand2nameAndValue(
			Map<Integer, Map<Pair<Integer>, Pair<String>>> arc2demand2nameAndValue) {
		this.arc2demand2nameAndValue = arc2demand2nameAndValue;
	}



	/*	public void printSolution(GRBModel model)  {
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			//System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
			//System.out.println("\nBuy:");
			for (int j = 0; j < buy.length; ++j) {
				if (buy[j].get(GRB.DoubleAttr.X) > 0.0001) {
					//System.out.println(buy[j].get(GRB.StringAttr.VarName) + " " +
							buy[j].get(GRB.DoubleAttr.X));
				}
			}
			//System.out.println("\nNutrition:");
			for (int i = 0; i < nutrition.length; ++i) {
				//System.out.println(nutrition[i].get(GRB.StringAttr.VarName) + " " +
						nutrition[i].get(GRB.DoubleAttr.X));
			}
		} else {
			//System.out.println("No solution");
		}
	}*/
}