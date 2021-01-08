package lpsolver;
/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */

import experiments.MainLPHeuristic;
import gurobi.GRB;
import gurobi.GRBConstr;
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

public class LPSolverHeuristic {

	private double min = Double.MIN_VALUE;
	private Integer mostCongestedIdArc = null; 
	Map<Integer,Map<Pair<Integer>,Pair<String>>> arc2demand2nameAndValue;
	Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction;

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
			Map<Pair<Integer>,Double> setOfDemandsWithAmountOneByOne = getDemandsWithAmountOneByOne(setOfDemands,g);

			Map<Pair<Integer>,Double> setOfDemandsWithAmountAllTogether = getDemandsWithAmountAllTogether(setOfDemands,g); 

			//System.out.println("demandsAllTogehter: "+setOfDemandsWithAmountAllTogether);

			GRBEnv env = new GRBEnv();
			env.set(GRB.IntParam.OutputFlag,0);
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "minimize_oblivious_performance");


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
					////System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
					//demand2var.put(new Pair<Integer>(demand.getSecond(),demand.getFirst()), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getSecond()+","+demand.getFirst()+")"));
					////System.out.print("x_"+arc.toStringShort()+"^("+demand.getSecond()+","+demand.getFirst()+")"+" ");
				}
			}
			////System.out.println();

			// CREATE alpha VARIABLE
			this.alpha = model.addVar(0,GRB.INFINITY, 1, GRB.CONTINUOUS,"alpha");

			// The objective is to maximize congestion
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

			// Update model to integrate new variables
			model.update();



			//add flow constraints per vertex
			////System.out.println("Flow Constraints");
			for(Vertex vertex: g.getVertices()){
				////System.out.println("v:"+vertex+" ");
				for(Pair<Integer> demand : setOfDemands){
					////System.out.print("\t");
					// flow conservation constraint at vertex for demand 
					GRBLinExpr flows = new GRBLinExpr();

					if(vertex.getId()==demand.getFirst()){
						////System.out.println(" nothing.");
						continue;
					}

					for(Arc outgoingArc:vertex.getArcs()){
						Arc reversedArc = g.getReversedArc(outgoingArc.getId());
						flows.addTerm(-1, this.x_edge2demand2var.get(outgoingArc.getId()).get(demand));
						flows.addTerm(1, this.x_edge2demand2var.get(reversedArc.getId()).get(demand));
					}
					if(vertex.getId()==demand.getSecond()){
						////System.out.println(" >= 1");
						model.addConstr(flows, GRB.EQUAL, 1,null);
					}else{
						////System.out.println(" >= 0");
						model.addConstr(flows, GRB.EQUAL, 0,null);
					}

				}
			}

			// for each set of demand try to minimize the congestion over that 
			for(Pair<Integer> demand : setOfDemandsWithAmountOneByOne.keySet()){
				// flow conservation constraint at vertex for demand
				for(Arc arc:g.getArcs()){
					GRBLinExpr flows = new GRBLinExpr();
					flows.addTerm(setOfDemandsWithAmountOneByOne.get(demand), this.x_edge2demand2var.get(arc.getId()).get(demand));
					flows.addTerm(-arc.getCapacity(), this.alpha);
					model.addConstr(flows, GRB.LESS_EQUAL,0 ,null);
				}
			}

			// flow conservation constraint at vertex for demand
			for(Arc arc:g.getArcs()){
				GRBLinExpr flows = new GRBLinExpr();
				for(Pair<Integer> demand : setOfDemandsWithAmountAllTogether.keySet()){
					flows.addTerm(setOfDemandsWithAmountAllTogether.get(demand), this.x_edge2demand2var.get(arc.getId()).get(demand));
				}
				flows.addTerm(-arc.getCapacity(), this.alpha);
				model.addConstr(flows, GRB.LESS_EQUAL,0 ,null);
			}

			//System.out.println("solving...");
			model.optimize();
			//System.out.println("solved : ");

			model.get(GRB.DoubleAttr.ObjVal);

			this.min = model.get(GRB.DoubleAttr.ObjVal);
			System.out.println("OneByOne+AllTogether Oblivious performance: "+ this.min);

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
			System.out.println("Solution: "+MainLPHeuristic.print(this.arc2demand2nameAndValue,g));


			model.dispose();
			env.dispose();


		} catch (Exception e) {
			////System.out.println("Error code: " + e.getMessage());
		}
	}





	private Map<Pair<Integer>, Double> getDemandsWithAmountAllTogether(
			List<Pair<Integer>> setOfDemands, Graph g) {

		////System.out.println("ALL TOGETHER");
		Map<Pair<Integer>, Double> demands2value = new HashMap<Pair<Integer>,Double>();
		try {

			GRBEnv env = new GRBEnv();
			env.set(GRB.IntParam.OutputFlag,0);
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "find min-cut");


			// CREATE X VARIABLES x_^(i,j)^(s,t)
			this.x_edge2demand2var = new HashMap<Integer,Map<Pair<Integer>,GRBVar>>();

			Map<Pair<Integer>,GRBVar> demand2var = new HashMap<Pair<Integer>,GRBVar>();
			//System.out.print("Demands: ");
			for(Arc arc:g.getArcs()){
				demand2var = this.x_edge2demand2var.get(arc.getId());
				if(demand2var==null){
					demand2var = new HashMap<Pair<Integer>,GRBVar>();
					this.x_edge2demand2var.put(arc.getId(), demand2var);
				}
				for(Pair<Integer> demand: setOfDemands){
					demand2var.put(demand, model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"));
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
				}

			}
			//System.out.println();
			this.alpha = model.addVar(0,GRB.INFINITY,1, GRB.CONTINUOUS,"alpha");

			// The objective is to maximize congestion
			model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

			// Update model to integrate new variables
			model.update();

			Map<Pair<Integer>,Pair<GRBConstr>> constraints = new HashMap<Pair<Integer>,Pair<GRBConstr>>();
			//add flow constraints per vertex
			//System.out.println("Flow Constraints");
			for(Vertex vertex: g.getVertices()){
				for(Pair<Integer> demand:setOfDemands){
					//System.out.print("v:"+vertex+" -> ");
					////System.out.print("\t");
					// flow conservation constraint at vertex for demand 
					GRBLinExpr flow = new GRBLinExpr();


					for(Arc outgoingArc:vertex.getArcs()){
						Arc reversedArc = g.getReversedArc(outgoingArc.getId());
						flow.addTerm(-1, this.x_edge2demand2var.get(outgoingArc.getId()).get(demand));
						//System.out.print("-"+this.x_edge2demand2var.get(outgoingArc.getId()).get(demand).get(GRB.StringAttr.VarName) );
						flow.addTerm(1, this.x_edge2demand2var.get(reversedArc.getId()).get(demand));
						//System.out.print("+"+this.x_edge2demand2var.get(reversedArc.getId()).get(demand).get(GRB.StringAttr.VarName) );
					}
					if(constraints.get(demand)==null)
						constraints.put(demand, new Pair<GRBConstr>(null,null));
					//flow.addTerm(1,demand2var.get(demand));
					if(vertex.getId()==demand.getFirst()){
						flow.addTerm(1, this.alpha);
						//System.out.print(" +alpha ");
						constraints.get(demand).setFirst(model.addConstr(flow, GRB.EQUAL, 0,null));
					}else if(vertex.getId()==demand.getSecond()){
						flow.addTerm(-1, this.alpha);
						//System.out.print(" -alpha ");
						constraints.get(demand).setSecond(model.addConstr(flow, GRB.EQUAL, 0,null));
					}else{
						model.addConstr(flow, GRB.EQUAL, 0,null);
					}
					//System.out.println(" = 0");

				}
			}


			//add flow constraints per vertex
			////System.out.println("Flow Constraints");
			for(Arc arc : g.getArcs()){
				////System.out.println("v:"+vertex+" ");
				////System.out.print("\t");
				// flow conservation constraint at vertex for demand 
				GRBLinExpr flows = new GRBLinExpr();
				for(Pair<Integer> demand:setOfDemands){
					flows.addTerm(1, this.x_edge2demand2var.get(arc.getId()).get(demand));
					model.addConstr(flows, GRB.LESS_EQUAL, arc.getCapacity(),null);
				}

			}
			//compute lexicographic ordering
			while(constraints.size()>0){
				//System.out.println("constraints: "+constraints.keySet());
				//System.out.println("solving...");
				model.optimize();
				//System.out.println("solved! " +  this.alpha.get(GRB.DoubleAttr.X));
				this.arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
				this.arc2demand2nameAndValue = new HashMap<Integer,Map<Pair<Integer>,Pair<String>>>();
				for(Integer idArc2 : this.x_edge2demand2var.keySet()){
					Map<Pair<Integer>,Pair<String>>  idVertex2idVertex2nameAndValue = new HashMap<Pair<Integer>,Pair<String>>();
					Map<Pair<Integer>,Double>  idVertex2idVertex2fraction = new HashMap<Pair<Integer>,Double>();
					this.arc2demand2nameAndValue.put(idArc2, idVertex2idVertex2nameAndValue);
					this.arc2demand2fraction.put(idArc2, idVertex2idVertex2fraction);
					for(Pair<Integer> demand2: this.x_edge2demand2var.get(idArc2).keySet()){
						idVertex2idVertex2nameAndValue.put(
								demand2, new Pair<String>(""+this.x_edge2demand2var.get(idArc2).get(demand2).get(GRB.StringAttr.VarName),
										this.x_edge2demand2var.get(idArc2).get(demand2).get(GRB.DoubleAttr.X)+""));
						idVertex2idVertex2fraction.put(
								demand2, this.x_edge2demand2var.get(idArc2).get(demand2).get(GRB.DoubleAttr.X));
					}
				}
				//System.out.println("Solution: "+MainLPHeuristic.print(this.arc2demand2nameAndValue,g));


				//find strict constraints
				Map<Pair<Integer>,Pair<GRBConstr>> strictConstraints = new HashMap<Pair<Integer>,Pair<GRBConstr>>();
				Pair<GRBConstr> pairPrevious = null;				
				double alphaValue= this.alpha.get(GRB.DoubleAttr.X);

				// set every demand to alphaValue
				for(Pair<Integer> demand : constraints.keySet()){
					Pair<GRBConstr> pairConstraint= constraints.get(demand);
					model.chgCoeff(pairConstraint.getFirst(), this.alpha, 0);
					model.chgCoeff(pairConstraint.getSecond(), this.alpha, 0);
					pairConstraint.getFirst().set(GRB.DoubleAttr.RHS, -alphaValue);
					pairConstraint.getSecond().set(GRB.DoubleAttr.RHS, alphaValue);
				}

				// check which demand can be increased
				int counter = 0;
				for(Pair<Integer> demand : constraints.keySet()){
					System.out.println("   still: "+(constraints.size() - counter++));
					Pair<GRBConstr> pairConstraint = constraints.get(demand);
					if(pairPrevious!=null){
						model.chgCoeff(pairPrevious.getFirst(), this.alpha, 0);
						model.chgCoeff(pairPrevious.getSecond(), this.alpha, 0);
						pairPrevious.getFirst().set(GRB.DoubleAttr.RHS, -alphaValue);
						pairPrevious.getSecond().set(GRB.DoubleAttr.RHS, alphaValue);
					}
					model.chgCoeff(pairConstraint.getFirst(), this.alpha, 1);
					model.chgCoeff(pairConstraint.getSecond(), this.alpha, -1);
					pairConstraint.getFirst().set(GRB.DoubleAttr.RHS, 0);
					pairConstraint.getSecond().set(GRB.DoubleAttr.RHS, 0);
					//System.out.print("solving...");
					model.optimize();
					//System.out.println("solved! demand: " + demand + " "+this.alpha.get(GRB.DoubleAttr.X));
					this.arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
					this.arc2demand2nameAndValue = new HashMap<Integer,Map<Pair<Integer>,Pair<String>>>();
					for(Integer idArc2 : this.x_edge2demand2var.keySet()){
						Map<Pair<Integer>,Pair<String>>  idVertex2idVertex2nameAndValue = new HashMap<Pair<Integer>,Pair<String>>();
						Map<Pair<Integer>,Double>  idVertex2idVertex2fraction = new HashMap<Pair<Integer>,Double>();
						this.arc2demand2nameAndValue.put(idArc2, idVertex2idVertex2nameAndValue);
						this.arc2demand2fraction.put(idArc2, idVertex2idVertex2fraction);
						for(Pair<Integer> demand2: this.x_edge2demand2var.get(idArc2).keySet()){
							idVertex2idVertex2nameAndValue.put(
									demand2, new Pair<String>(""+this.x_edge2demand2var.get(idArc2).get(demand2).get(GRB.StringAttr.VarName),
											this.x_edge2demand2var.get(idArc2).get(demand2).get(GRB.DoubleAttr.X)+""));
							idVertex2idVertex2fraction.put(
									demand2, this.x_edge2demand2var.get(idArc2).get(demand2).get(GRB.DoubleAttr.X));
						}
					}
					////System.out.println("Solution: "+MainLPHeuristic.print(this.arc2demand2nameAndValue,g));
					if (this.alpha.get(GRB.DoubleAttr.X) <= alphaValue){
						strictConstraints.put(demand,pairConstraint);
						//System.out.println("  removed");
					}
					pairPrevious=pairConstraint;
				}

				//
				for(Pair<Integer> demand : strictConstraints.keySet()){
					Pair<GRBConstr> constraint= strictConstraints.get(demand);
					model.chgCoeff(constraint.getFirst(), this.alpha, 0);
					model.chgCoeff(constraint.getSecond(), this.alpha, 0);
					constraint.getFirst().set(GRB.DoubleAttr.RHS, -alphaValue);
					constraint.getSecond().set(GRB.DoubleAttr.RHS, alphaValue);
					constraints.remove(demand);
					demands2value.put(demand, alphaValue);
				}

				//
				for(Pair<Integer> demand : constraints.keySet()){
					Pair<GRBConstr> constraint= constraints.get(demand);
					model.chgCoeff(constraint.getFirst(), this.alpha, 1);
					model.chgCoeff(constraint.getSecond(), this.alpha, -1);
					constraint.getFirst().set(GRB.DoubleAttr.RHS, 0);
					constraint.getSecond().set(GRB.DoubleAttr.RHS, 0);
				}

			}

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
		return demands2value;
	}

	private Map<Pair<Integer>, Double> getDemandsWithAmountOneByOne(List<Pair<Integer>> setOfDemands, Graph g) {

		Map<Pair<Integer>, Double> demands2value = new HashMap<Pair<Integer>, Double>();
		for(Pair<Integer> demand: setOfDemands){
			try {

				GRBEnv env = new GRBEnv();
				env.set(GRB.IntParam.OutputFlag,0);
				GRBModel model = new GRBModel(env);
				model.set(GRB.StringAttr.ModelName, "find min-cut");


				// CREATE X VARIABLES x_^(i,j)^(s,t)
				this.x_edge2demand2var = new HashMap<Integer,Map<Pair<Integer>,GRBVar>>();
				////System.out.print("Variable demands: ");
				for(Arc arc:g.getArcs()){

					Map<Pair<Integer>,GRBVar> demand2var = this.x_edge2demand2var.get(arc.getId());
					if(demand2var==null){
						demand2var = new HashMap<Pair<Integer>,GRBVar>();
						this.x_edge2demand2var.put(arc.getId(), demand2var);
					}
					if(demand.getFirst().equals(arc.getFirstEndPoint().getId())){
						demand2var.put(demand, model.addVar(0,GRB.INFINITY, 1, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"));
						////System.out.print("1*x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
					}else if(demand.getFirst().equals(arc.getSecondEndPoint().getId())){
						demand2var.put(demand, model.addVar(0,GRB.INFINITY, -1, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"));
						////System.out.print("-1*x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
					}else {
						demand2var.put(demand, model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"));
					} 
					////System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");

				}
				////System.out.println();


				// The objective is to maximize congestion
				model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

				// Update model to integrate new variables
				model.update();

				//add flow constraints per vertex
				////System.out.println("Flow Constraints");
				for(Vertex vertex: g.getVertices()){
					////System.out.print("v:"+vertex+" ");
					////System.out.print("\t");
					// flow conservation constraint at vertex for demand 
					GRBLinExpr flows = new GRBLinExpr();

					if(vertex.getId()==demand.getSecond()){
						////System.out.println(" nothing.");
						continue;
					}
					if(vertex.getId()==demand.getFirst()){
						////System.out.println(" nothing.");
						continue;
					}

					for(Arc outgoingArc:vertex.getArcs()){
						Arc reversedArc = g.getReversedArc(outgoingArc.getId());
						flows.addTerm(-1, this.x_edge2demand2var.get(outgoingArc.getId()).get(demand));
						////System.out.print("-"+this.x_edge2demand2var.get(outgoingArc.getId()).get(demand).get(GRB.StringAttr.VarName) );
						flows.addTerm(1, this.x_edge2demand2var.get(reversedArc.getId()).get(demand));
						////System.out.print("+"+this.x_edge2demand2var.get(reversedArc.getId()).get(demand).get(GRB.StringAttr.VarName) );
					}
					////System.out.println(" = 0");
					model.addConstr(flows, GRB.EQUAL, 0,null);
				}

				//add flow constraints per vertex
				////System.out.println("Flow Constraints");
				for(Arc arc : g.getArcs()){
					////System.out.println("v:"+vertex+" ");
					////System.out.print("\t");
					// flow conservation constraint at vertex for demand 
					GRBLinExpr flows = new GRBLinExpr();

					flows.addTerm(1, this.x_edge2demand2var.get(arc.getId()).get(demand));
					model.addConstr(flows, GRB.LESS_EQUAL, arc.getCapacity(),null);

				}



				////System.out.println("solving...");
				model.optimize();

				model.get(GRB.DoubleAttr.ObjVal);

				this.min = model.get(GRB.DoubleAttr.ObjVal);
				////System.out.println("solved! Demand: " + demand + ": " + this.min);

				demands2value.put(demand, this.min);

				this.arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
				this.arc2demand2nameAndValue = new HashMap<Integer,Map<Pair<Integer>,Pair<String>>>();
				for(Integer idArc2 : this.x_edge2demand2var.keySet()){
					Map<Pair<Integer>,Pair<String>>  idVertex2idVertex2nameAndValue = new HashMap<Pair<Integer>,Pair<String>>();
					Map<Pair<Integer>,Double>  idVertex2idVertex2fraction = new HashMap<Pair<Integer>,Double>();
					this.arc2demand2nameAndValue.put(idArc2, idVertex2idVertex2nameAndValue);
					this.arc2demand2fraction.put(idArc2, idVertex2idVertex2fraction);

					idVertex2idVertex2nameAndValue.put(
							demand, new Pair<String>(""+this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.StringAttr.VarName),
									this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.DoubleAttr.X)+""));
					idVertex2idVertex2fraction.put(
							demand, this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.DoubleAttr.X));
				}
				////System.out.println("Solution: "+MainLPHeuristic.print(this.arc2demand2nameAndValue,g));
				model.dispose();
				env.dispose();


			} catch (Exception e) {
				////System.out.println("Error code: " + e.getMessage());
			}
		}
		return demands2value;
	}

	public static void printSolution(GRBModel model, GRBVar[] buy,
			GRBVar[] nutrition) throws GRBException {
		if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
			////System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
			////System.out.println("\nBuy:");
			for (int j = 0; j < buy.length; ++j) {
				if (buy[j].get(GRB.DoubleAttr.X) > 0.0001) {
					////System.out.println(buy[j].get(GRB.StringAttr.VarName) + " " +	buy[j].get(GRB.DoubleAttr.X));
				}
			}
			////System.out.println("\nNutrition:");
			for (int i = 0; i < nutrition.length; ++i) {
				////System.out.println(nutrition[i].get(GRB.StringAttr.VarName) + " " + nutrition[i].get(GRB.DoubleAttr.X));
			}
		} else {
			////System.out.println("No solution");
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
			////System.out.println("\nCost: " + model.get(GRB.DoubleAttr.ObjVal));
			////System.out.println("\nBuy:");
			for (int j = 0; j < buy.length; ++j) {
				if (buy[j].get(GRB.DoubleAttr.X) > 0.0001) {
					////System.out.println(buy[j].get(GRB.StringAttr.VarName) + " " +
							buy[j].get(GRB.DoubleAttr.X));
				}
			}
			////System.out.println("\nNutrition:");
			for (int i = 0; i < nutrition.length; ++i) {
				////System.out.println(nutrition[i].get(GRB.StringAttr.VarName) + " " +
						nutrition[i].get(GRB.DoubleAttr.X));
			}
		} else {
			////System.out.println("No solution");
		}
	}*/
}