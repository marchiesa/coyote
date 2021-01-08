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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class LPSolverOptimalObliviousClosNetworkComparison {

	private double min = Double.MIN_VALUE;
	private Integer mostCongestedIdArc = null; 
	Map<Integer,Map<Pair<Integer>,Pair<String>>> arc2demand2nameAndValue;
	Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction;

	private Map<Integer,Map<Integer,GRBVar>>  w_edge2vertex2var;
	private Map<Integer,Map<Pair<Integer>,GRBVar>> x_edge2demand2var;
	private GRBVar alpha;


	public void computeOptimalPerformance(Graph g,List<Pair<Integer>> setOfDemands, Map<Integer,Integer> servers){

		try {
			GRBEnv env = new GRBEnv();
			env.set(GRB.IntParam.OutputFlag,0);
			env.set(GRB.IntParam.Method, 2);
			env.set(GRB.IntParam.Crossover, 0);
			env.set(GRB.DoubleParam.BarConvTol, 0.01d);
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "minimize_oblivious_performance");
		

			boolean print = false;
			//System.out.println("bella");

			Set<Integer> sourceVertices = new TreeSet<Integer>();
			Set<Integer> destinationVertices = new TreeSet<Integer>();
			Set<Integer> sourceAndDestinationVertices = new TreeSet<Integer>();
			for(Pair<Integer> demand : setOfDemands){
				sourceVertices.add(demand.getFirst());
				destinationVertices.add(demand.getSecond());
			}
			sourceAndDestinationVertices.addAll(sourceVertices);
			sourceAndDestinationVertices.addAll(destinationVertices);

			int counter2=0;
			int counter3=0;
			
			// CREATE X VARIABLES x_^(i,j)^(s,t)
			this.x_edge2demand2var = new HashMap<Integer,Map<Pair<Integer>,GRBVar>>();

			for(Arc arc:g.getArcs()){
				Map<Pair<Integer>,GRBVar> demand2var =  new HashMap<Pair<Integer>,GRBVar>();
				this.x_edge2demand2var.put(arc.getId(), demand2var);

				for(Pair<Integer> demand:setOfDemands){
					GRBVar demandVar = model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")");
					demand2var.put(demand, demandVar);
					counter2++;
					counter3++;
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")" + "\n");
					//demand2var.put(new Pair<Integer>(demand.getSecond(),demand.getFirst()), demandVar);
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getSecond()+","+demand.getFirst()+")"+" ");
				}
			}
			//System.out.println();


			// CREATE W VARIABLES w_e^(i,j)
			this.w_edge2vertex2var = new HashMap<Integer,Map<Integer,GRBVar>>();
			for(Arc arc:g.getArcs()){
				if(arc.getSecondEndPoint().getId() > arc.getFirstEndPoint().getId())
					continue;
				Map<Integer,GRBVar> vertex2var = new HashMap<Integer,GRBVar>();
				this.w_edge2vertex2var.put(arc.getId(), vertex2var);

				for(Integer vertexId: sourceAndDestinationVertices){
					GRBVar demandVar = model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"w_"+arc.toStringShort()+"^"+vertexId);
					vertex2var.put(vertexId, demandVar);
					counter2++;
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
				}
			}
			//createWVariables(this.w_edge2position2var,g,position2id,model);

			// CREATE alpha VARIABLE
			this.alpha = model.addVar(0,GRB.INFINITY, 1, GRB.CONTINUOUS,"alpha");

			// The objective is to minimize congestion
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

			// Update model to integrate new variables
			model.update();

			int counter = 0;
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
					counter++;

				}
			}

			if(print)System.out.println("First Dual Constraints");
			// first dual constraint from flow constraint
			for(Arc arcE: g.getArcs()){
				if(this.w_edge2vertex2var.get(arcE.getId())==null)
					continue;
				if(print)System.out.println("e:"+arcE.toStringShort()+" ");
				Map<Integer,GRBVar> vertex2var = this.w_edge2vertex2var.get(arcE.getId());
				for(Pair<Integer> demand : setOfDemands){
					if(print)System.out.print("\t (s,t):("+demand.getFirst()+","+demand.getSecond()+") ");

					GRBLinExpr flows = new GRBLinExpr();
					if(print)System.out.print(" + "+vertex2var.get(demand.getFirst()).get(GRB.StringAttr.VarName));
					flows.addTerm(1, vertex2var.get(demand.getFirst()));

					if(print)System.out.print(" + "+vertex2var.get(demand.getSecond()).get(GRB.StringAttr.VarName));
					flows.addTerm(1, vertex2var.get(demand.getSecond()));

					if(print)System.out.print(" - "+this.x_edge2demand2var.get(arcE.getId()).get(demand).get(GRB.StringAttr.VarName) + "/" + arcE.getCapacity());
					flows.addTerm(-1d/arcE.getCapacity(), this.x_edge2demand2var.get(arcE.getId()).get(demand));

					if(print)System.out.print(" - "+this.x_edge2demand2var.get(g.getReversedArc(arcE.getId()).getId()).get(demand).get(GRB.StringAttr.VarName) + "/" + g.getReversedArc(arcE.getId()).getCapacity());
					flows.addTerm(-1d/g.getReversedArc(arcE.getId()).getCapacity(), this.x_edge2demand2var.get(g.getReversedArc(arcE.getId()).getId()).get(demand));

					if(print)System.out.println(" >= 0" );
					model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
					counter++;
				}
			}

			if(print)System.out.println("Second Dual Constraints");
			// first dual constraint from flow constraint
			for(Arc arcE: g.getArcs()){
				if(this.w_edge2vertex2var.get(arcE.getId())==null)
					continue;

				if(print)System.out.print("e:"+arcE.toStringShort()+" ");
				Map<Integer,GRBVar> vertex2var = this.w_edge2vertex2var.get(arcE.getId());

				GRBLinExpr flows = new GRBLinExpr();

				for(Integer vertexId : this.w_edge2vertex2var.get(arcE.getId()).keySet()){
					if(print)System.out.print(" + "+servers.get(vertexId)+"*"+vertex2var.get(vertexId).get(GRB.StringAttr.VarName));
					flows.addTerm(servers.get(vertexId), vertex2var.get(vertexId));
				}

				if(print)System.out.print(" - alpha");
				flows.addTerm(-1, this.alpha);

				if(print)System.out.println(" <= 0" );
				model.addConstr(flows, GRB.LESS_EQUAL, 0,null);
				counter++;
			}

			System.out.println("solving... number of constraints: " + counter + " number of variables: "+ counter2+ " number of variables(flow): "+ counter3);
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
					//System.out.println("value: " + ""+this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.StringAttr.VarName) +", " +
					//		this.x_edge2demand2var.get(idArc2).get(demand).get(GRB.DoubleAttr.X)+"");
				}
			}

			model.dispose();
			env.dispose();


		} catch (Exception e) {
			System.out.println("Error code: " + e.getMessage());
		}
	}





	/*private void createWVariables(
			Map<Integer, NDimensionalMap<GRBVar>> w_edge2vertex2var2, Graph g,
			NDimensionalMap<Integer> position2id, GRBModel model) throws GRBException {
		for(Arc arc:g.getArcs()){
			NDimensionalMap<GRBVar> position2var = w_edge2vertex2var2.get(arc.getId());
			if(position2var==null){
				position2var = new NDimensionalMap<GRBVar>();
				this.w_edge2vertex2var.put(arc.getId(), position2var);
			}
			createWVariablesRic(position2var,g,position2id,model,position2id.getDegree(),arc);
			//System.out.println();
		}

	}

	private void createWVariablesRic(
			NDimensionalMap<GRBVar> vertex2var, Graph g,
			NDimensionalMap<Integer> position2id,GRBModel model, int degree, Arc arc) throws GRBException {
		if(position2id.getHeight()==0){
			List<Integer> indices = this.reconstructHierarchy(position2id);
			vertex2var.setValue(model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"w_"+arc.toStringShort()+"^"+toString(indices)));			
		}
		else{
			int numberOfIndices = degree;
			if(position2id.isTop())
				numberOfIndices *= 2;
			for(int i=0;i<numberOfIndices;i++){
				createWVariablesRic(vertex2var.get(i),g,position2id.get(i), model, degree, arc);
			}
		}
	}

	private String toString(List<Integer> indices) {
		int size = indices.size();
		String result="(";
		int counter=0;
		for(Integer i: indices){
			result+=i;
			counter++;
			if(counter <size)
				result+=",";
		}
		result+=")";
		return result;
	}


	private List<Integer> reconstructHierarchy(
			NDimensionalMap<Integer> position2id) {
		LinkedList<Integer> hierarchy = new LinkedList<Integer>();
		do{
			hierarchy.addLast(position2id.getIndex());
		}while(position2id.getFather() != null);
		return hierarchy;
	}*/

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