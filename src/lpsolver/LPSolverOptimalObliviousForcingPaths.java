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

public class LPSolverOptimalObliviousForcingPaths {

	private double min = Double.MIN_VALUE;
	private Integer mostCongestedIdArc = null; 
	Map<Pair<Integer>,Map<Integer,Pair<String>>> demand2path2nameAndValue;
	Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction;

	private Map<Integer,Map<Integer,Map<Integer,GRBVar>>> pi_edge2vertex2vertex2var;
	private Map<Integer,Map<Integer,GRBVar>>  w_edge2edge2var;
	private Map<Pair<Integer>,Map<Integer,GRBVar>> demand2path2var;
	private Map<Integer,Map<Pair<Integer>,List<GRBVar>>> arc2demand2pathVar;
	private GRBVar alpha;

	public void computeOptimalPerformance(Graph g, Map<Pair<Integer>,List<List<Arc>>> demand2listoOfPaths){

		try {
			GRBEnv env = new GRBEnv();
			env.set(GRB.IntParam.OutputFlag,0);
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "minimize_oblivious_performance");

			Set<Integer> sourceVertices = new TreeSet<Integer>();
			for(Pair<Integer> demand : demand2listoOfPaths.keySet()){
				sourceVertices.add(demand.getFirst());
			}

			// CREATE PI VARIABLES pi_e^(i,j)
			this.pi_edge2vertex2vertex2var = new HashMap<Integer,Map<Integer,Map<Integer,GRBVar>>>();

			for(Arc arc:g.getArcs()){
				Map<Integer,Map<Integer,GRBVar>> vertex2vertex2var = new HashMap<Integer,Map<Integer,GRBVar>>();
				pi_edge2vertex2vertex2var.put(arc.getId(), vertex2vertex2var);

				for(Integer i: sourceVertices){
					Map<Integer,GRBVar> vertex2var = new HashMap<Integer,GRBVar>();
					vertex2vertex2var.put(i, vertex2var);
					for(Vertex j: g.getVertices()){
						if(i==j.getId())
							continue;
						vertex2var.put(j.getId(), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"pi_"+arc.toStringShort()+"^("+i+","+j+")"));
						System.out.print("pi_"+arc.toStringShort()+"^("+i+","+j+")"+" ");

					}
				}
				System.out.println();
			}


			// CREATE W VARIABLES w_e^(i,j)
			this.w_edge2edge2var = new HashMap<Integer,Map<Integer,GRBVar>>();

			for(Arc arc:g.getArcs()){
				Map<Integer,GRBVar> edge2var = this.w_edge2edge2var.get(arc.getId());
				if(edge2var==null){
					edge2var = new HashMap<Integer,GRBVar>();
					this.w_edge2edge2var.put(arc.getId(), edge2var);
				}
				for(Arc arc2:g.getArcs()){
					edge2var.put(arc2.getId(), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"w_"+arc.toStringShort()+"^"+arc2.toStringShort()));
					System.out.print("w_"+arc.toStringShort()+"^("+arc2.toStringShort()+" ");
				}
				System.out.println();
			}

			// CREATE X VARIABLES x_^(i,j)^(s,t)
			/*this.x_edge2demand2var = new HashMap<Integer,Map<Pair<Integer>,GRBVar>>();

			for(Arc arc:g.getArcs()){
				Map<Pair<Integer>,GRBVar> demand2var = this.x_edge2demand2var.get(arc.getId());
				if(demand2var==null){
					demand2var = new HashMap<Pair<Integer>,GRBVar>();
					this.x_edge2demand2var.put(arc.getId(), demand2var);
				}
				for(Pair<Integer> demand:setOfDemands){
					demand2var.put(demand, model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"));
					System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
					demand2var.put(new Pair<Integer>(demand.getSecond(),demand.getFirst()), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getSecond()+","+demand.getFirst()+")"));
					System.out.print("x_"+arc.toStringShort()+"^("+demand.getSecond()+","+demand.getFirst()+")"+" ");
				}
			}
			System.out.println();*/

			//Create path variables
			this.demand2path2var = new HashMap<Pair<Integer>,Map<Integer,GRBVar>>();
			arc2demand2pathVar = new HashMap<Integer,Map<Pair<Integer>,List<GRBVar>>>();

			System.out.print("paths: ");
			for(Pair<Integer> demand: demand2listoOfPaths.keySet()){
				Map<Integer,GRBVar> path2var = new HashMap<Integer,GRBVar>();
				this.demand2path2var.put(demand, path2var);
				int i=0;
				for(List<Arc> path:demand2listoOfPaths.get(demand)){
					System.out.print("path_"+i+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
					path2var.put(i, model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"path_"+i+"^("+demand.getFirst()+","+demand.getSecond()+")"));

					for(Arc arcInPath: path){
						if(arc2demand2pathVar.get(arcInPath.getId())==null){
							arc2demand2pathVar.put(arcInPath.getId(), new HashMap<Pair<Integer>,List<GRBVar>>());
						}
						if(arc2demand2pathVar.get(arcInPath.getId()).get(demand)==null){
							arc2demand2pathVar.get(arcInPath.getId()).put(demand, new LinkedList<GRBVar>());
						}
						arc2demand2pathVar.get(arcInPath.getId()).get(demand).add(this.demand2path2var.get(demand).get(i));
					}
					i++;
				}
			}
			System.out.println();

			// CREATE alpha VARIABLE
			this.alpha = model.addVar(0,GRB.INFINITY, 1, GRB.CONTINUOUS,"alpha");

			// The objective is to maximize congestion
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

			// Update model to integrate new variables
			model.update();

			//add path constraints
			System.out.println("Paths Constraints");
			for(Pair<Integer> demand: this.demand2path2var.keySet()){
				//if(arc2.getFirstEndPoint().getId()>arc2.getSecondEndPoint().getId())
				//	continue;
				//System.out.print("\t ("+arc2.getFirstEndPoint().getId()+","+arc2.getSecondEndPoint().getId()+") [");

				System.out.print("\t");
				GRBLinExpr flows = new GRBLinExpr();
				for(Integer pathId : this.demand2path2var.get(demand).keySet()){
					System.out.print("+ "+this.demand2path2var.get(demand).get(pathId).get(GRB.StringAttr.VarName)+" ");
					flows.addTerm(1, this.demand2path2var.get(demand).get(pathId));
					//System.out.print("+ "+arc2idVertex2var.get(arc2.getId()).get(source.getId()).get(GRB.StringAttr.VarName)+
					//		" + "+arc2idVertex2var.get(reversedArc.getId()).get(source.getId()).get(GRB.StringAttr.VarName)+" ");
				}
				System.out.println(" >= 1");
				model.addConstr(flows, GRB.GREATER_EQUAL, 1,null);
			}
			//add flow constraints per vertex
			/*System.out.println("Flow Constraints");
			for(Vertex vertex: g.getVertices()){
				System.out.println("v:"+vertex+" ");
				for(Pair<Integer> demand : setOfDemands){
					System.out.print("\t");
					// flow conservation constraint at vertex for demand 
					GRBLinExpr flows = new GRBLinExpr();

					if(vertex.getId()==demand.getFirst()){
						System.out.println(" nothing.");
						continue;
					}

					for(Arc outgoingArc:vertex.getArcs()){
						System.out.print(" - "+this.x_edge2demand2var.get(outgoingArc.getId()).get(demand).get(GRB.StringAttr.VarName));
						flows.addTerm(-1, this.x_edge2demand2var.get(outgoingArc.getId()).get(demand));
						Arc reversedArc = g.getReversedArc(outgoingArc.getId());
						System.out.print(" + "+this.x_edge2demand2var.get(reversedArc.getId()).get(demand).get(GRB.StringAttr.VarName));
						flows.addTerm(1, this.x_edge2demand2var.get(reversedArc.getId()).get(demand));
					}
					if(vertex.getId()==demand.getSecond()){
						System.out.println(" >= 1");
						model.addConstr(flows, GRB.GREATER_EQUAL, 1,null);
					}else{
						System.out.println(" >= 0");
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
					}
				}
			}*/

			System.out.println("First Dual Constraints");
			// first dual constraint from flow constraint
			for(Arc arcE: g.getArcs()){
				System.out.println("e:"+arcE.toStringShort()+" ");
				for(Integer i: sourceVertices){
					System.out.println("\t i:"+i+" ");
					Map<Integer,GRBVar> vertex2var = this.pi_edge2vertex2vertex2var.get(arcE.getId()).get(i);
					for(Arc arc2: g.getArcs()){
						System.out.print("\t\t a:"+arc2.toStringShort()+" ");
						//pi_e^(ij) - pi_e^ik + w_e >= 0
						GRBLinExpr flows = new GRBLinExpr();
						if(vertex2var.get(arc2.getFirstEndPoint().getId())!=null){
							System.out.print(" + "+vertex2var.get(arc2.getFirstEndPoint().getId()).get(GRB.StringAttr.VarName));
							flows.addTerm(1, vertex2var.get(arc2.getFirstEndPoint().getId()));
						}
						if(vertex2var.get(arc2.getSecondEndPoint().getId())!=null){
							System.out.print(" - "+vertex2var.get(arc2.getSecondEndPoint().getId()).get(GRB.StringAttr.VarName));
							flows.addTerm(-1, vertex2var.get(arc2.getSecondEndPoint().getId()));
						}
						System.out.print(" + "+this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()).get(GRB.StringAttr.VarName));
						flows.addTerm(1, this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()));

						System.out.println(" >= 0");
						model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
					}
				}
			}

			System.out.println("Second Dual Constraints");
			// second dual constraint from flow constraint
			for(Arc arcE: g.getArcs()){
				System.out.println("e:"+arcE.toStringShort()+" ");
				Map<Integer,Map<Integer,GRBVar>> vertex2vertex2var = this.pi_edge2vertex2vertex2var.get(arcE.getId());
				for(Pair<Integer> demand: demand2listoOfPaths.keySet()){
					System.out.print("\t d:"+demand+" ");
					Map<Integer,GRBVar> vertex2var = vertex2vertex2var.get(demand.getFirst());
					GRBLinExpr flows = new GRBLinExpr();
					System.out.print(" + "+vertex2var.get(demand.getSecond()).get(GRB.StringAttr.VarName));
					flows.addTerm(1, vertex2var.get(demand.getSecond()));
					//System.out.print(" - "+this.x_edge2demand2var.get(arcE.getId()).get(new Pair<Integer>(demand.getFirst(),demand.getSecond())).get(GRB.StringAttr.VarName));
					//flows.addTerm(-1, this.x_edge2demand2var.get(arcE.getId()).get(new Pair<Integer>(demand.getFirst(),demand.getSecond())));

					if(arc2demand2pathVar.get(arcE.getId())!=null && arc2demand2pathVar.get(arcE.getId()).get(demand)!=null){
						for(GRBVar var : arc2demand2pathVar.get(arcE.getId()).get(demand)){
							System.out.print(" - "+var.get(GRB.StringAttr.VarName));
							flows.addTerm(-1, var);
						}
					}

					System.out.println(" >= 0");
					model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);

				}
			}

			System.out.println("Dual Optimization Constraints");
			// dual optimization function via constraint 
			for(Arc arcE: g.getArcs()){
				System.out.print("e:"+arcE.toStringShort()+" ");
				GRBLinExpr flows = new GRBLinExpr();
				for(Arc arc2:g.getArcs()){
					System.out.print(" - "+arc2.getCapacity()+"*"+this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()).get(GRB.StringAttr.VarName));
					flows.addTerm(-arc2.getCapacity(), this.w_edge2edge2var.get(arcE.getId()).get(arc2.getId()));
				}
				System.out.print(" + "+arcE.getCapacity()+"*"+this.alpha.get(GRB.StringAttr.VarName));
				flows.addTerm(arcE.getCapacity(), this.alpha);

				System.out.println(" >= 0");
				model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
			}

			model.optimize();

			model.get(GRB.DoubleAttr.ObjVal);

			this.min = model.get(GRB.DoubleAttr.ObjVal);

			this.arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
			for(Integer arcId: this.arc2demand2pathVar.keySet()){
				Map<Pair<Integer>,Double>  path2fraction = new HashMap<Pair<Integer>,Double>();
				this.arc2demand2fraction.put(arcId, path2fraction);
				for(Pair<Integer> demand: this.arc2demand2pathVar.get(arcId).keySet()){
					Double sum = 0d;
					for(GRBVar var : this.arc2demand2pathVar.get(arcId).get(demand)){
						sum +=var.get(GRB.DoubleAttr.X);
					}
					path2fraction.put(
							demand, sum);
				}
			}


			model.dispose();
			env.dispose();


		} catch (Exception e) {
			System.out.println("Error code: " + e.getMessage());
		}
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

	public double getMin() {
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
	public Map<Integer, Map<Pair<Integer>, Double>> getArc2demand2fraction() {
		return arc2demand2fraction;
	}



	public void setArc2demand2fraction(
			Map<Integer, Map<Pair<Integer>, Double>> arc2demand2fraction) {
		this.arc2demand2fraction = arc2demand2fraction;
	}



}