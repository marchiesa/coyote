package lpsolver;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.HashMap;
import java.util.Map;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class MCFSolver {

	private double max = Double.MIN_VALUE;


	private Integer mostCongestedIdArc = null; 
	private Map<Pair<Integer>,Pair<String>> demand2nameAndValue;
	Map<Integer,Map<Integer,Pair<String>>> arc2idVertex2nameAndValue;
	private Map<Integer,Map<Integer,GRBVar>> vertex2edge2var;
	private Map<Integer,Double> arc2rLoad;
	private GRBVar alpha;
	private Map<Integer,Map<Integer,Map<Integer,Double>>> destination2vertex2edge2value;

	public double getMax() {
		return max;
	}

	public void computeOptimalCongestion(Graph g, Map<Pair<Integer>,Double> demand2estimate){
		this.computeOptimalCongestion(g, demand2estimate, false);
	}
	
	public void computeOptimalCongestion(Graph g, Map<Pair<Integer>,Double> demand2estimate, boolean aggregateCongestion){
		this.destination2vertex2edge2value = new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();
		this.arc2rLoad= new HashMap<Integer,Double>();

		try {
			GRBEnv env = new GRBEnv();
			env.set(GRB.IntParam.OutputFlag,0);
			GRBModel model = new GRBModel(env);
			model.set(GRB.StringAttr.ModelName, "minimize_congestion");
			/*if(arc2demand2fraction==null){
					arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
					for(Arc arc:g.getArcs())
						arc2demand2fraction.put(arc.getId(), null);
				}*/

			this.vertex2edge2var = new HashMap<Integer,Map<Integer,GRBVar>>();
			for(Vertex destination: g.getVertices()){
				Map<Integer,GRBVar> edge2var = new HashMap<Integer,GRBVar>();
				vertex2edge2var.put(destination.getId(), edge2var);
				for(Arc arc : g.getArcs()){
					if(arc.getFirstEndPoint().getId()==destination.getId())
						continue;
					edge2var.put(arc.getId(), model.addVar(0,GRB.INFINITY, 0.00001, GRB.CONTINUOUS,"g_"+destination.getId()+"_"+arc.getId()));
				}
			}
			this.alpha = model.addVar(0,GRB.INFINITY, 1, GRB.CONTINUOUS,"alpha");

			// The objective is to maximize congestion
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

			// Update model to integrate new variables
			model.update();

			//add flow constraints per vertex
			for(Vertex destination: g.getVertices()){

				Map<Integer, GRBVar> edge2var = this.vertex2edge2var.get(destination.getId());
				for(Vertex vertex : g.getVertices()){
					if(vertex.getId() == destination.getId())
						continue;
					GRBLinExpr flows = new GRBLinExpr();

					for(Arc outgoingArc : vertex.getArcs()){
						flows.addTerm(1, edge2var.get(outgoingArc.getId()));
						Arc incomingArc = g.getReversedArc(outgoingArc.getId());
						if(incomingArc.getFirstEndPoint().getId() != destination.getId())
							flows.addTerm(-1, edge2var.get(incomingArc.getId()));						
					}
					model.addConstr(flows, GRB.GREATER_EQUAL, demand2estimate.get(new Pair<Integer>(vertex.getId(),destination.getId())),null);
				}
			}

			//add flow constraints per vertex
			for(Arc arc: g.getArcs()){
				if(aggregateCongestion && (arc.getId() % 2 == 1))
					continue;
				GRBLinExpr flows = new GRBLinExpr();
				flows.addTerm(arc.getCapacity(),this.alpha);
				for(Vertex destination : g.getVertices()){
					if(arc.getFirstEndPoint().getId() == destination.getId())
						continue;
					flows.addTerm(-1,vertex2edge2var.get(destination.getId()).get(arc.getId()));
					if(aggregateCongestion){
						if(arc.getSecondEndPoint().getId() == destination.getId())
							continue;
						flows.addTerm(-1,vertex2edge2var.get(destination.getId()).get(g.getReversedArc(arc.getId()).getId()));
					}

				}
				model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
			}


			// Solve 
			System.out.println("Solving...");
			model.optimize();
			System.out.println("Solved!" + (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) + " value:"+model.get(GRB.DoubleAttr.ObjVal));


			if(model.get(GRB.IntAttr.Status) != GRB.OPTIMAL){
				return;
			}
			model.get(GRB.DoubleAttr.ObjVal);


			// stores the output of the lp program
			if(this.max<model.get(GRB.DoubleAttr.ObjVal)){
				this.max = model.get(GRB.DoubleAttr.ObjVal);
				this.demand2nameAndValue = new HashMap<Pair<Integer>,Pair<String>>();
				this.arc2idVertex2nameAndValue = new HashMap<Integer,Map<Integer,Pair<String>>>();
				for(Arc arc : g.getArcs()){
					Map<Integer,Pair<String>>  idVertex2nameAndValue = new HashMap<Integer,Pair<String>>();
					this.arc2idVertex2nameAndValue.put(arc.getId(), idVertex2nameAndValue);
					for(Vertex destination : g.getVertices()){
						if(vertex2edge2var.get(destination.getId()).get(arc.getId())!=null){
							idVertex2nameAndValue.put(destination.getId(), new Pair<String>(vertex2edge2var.get(destination.getId()).get(arc.getId()).get(GRB.StringAttr.VarName),
									vertex2edge2var.get(destination.getId()).get(arc.getId()).get(GRB.DoubleAttr.X)+""));
							Map<Integer, Map<Integer,Double>> vertex2edge2value  = this.destination2vertex2edge2value.get(destination.getId());
							if(vertex2edge2value == null){
								vertex2edge2value = new HashMap<Integer, Map<Integer,Double>>();
								destination2vertex2edge2value.put(destination.getId(), vertex2edge2value ); 
							}
							Map<Integer,Double> edge2value  = vertex2edge2value.get(arc.getFirstEndPoint().getId());
							if(edge2value == null){
								edge2value = new HashMap<Integer,Double>();
								vertex2edge2value.put(arc.getFirstEndPoint().getId(), edge2value ); 
							}
							edge2value.put(arc.getId(), vertex2edge2var.get(destination.getId()).get(arc.getId()).get(GRB.DoubleAttr.X));
						}
					}
				}

				//normalize the splitting ratios
				for(Integer destinationId: destination2vertex2edge2value.keySet()){
					for(Integer vertexId : destination2vertex2edge2value.get(destinationId).keySet()){
						double sum = 0d;
						for(Integer arcId : destination2vertex2edge2value.get(destinationId).get(vertexId).keySet()){
							sum += destination2vertex2edge2value.get(destinationId).get(vertexId).get(arcId);
						}
						for(Integer arcId : destination2vertex2edge2value.get(destinationId).get(vertexId).keySet()){
							destination2vertex2edge2value.get(destinationId).get(vertexId).put(arcId,destination2vertex2edge2value.get(destinationId).get(vertexId).get(arcId)/sum);
							//System.out.println("")
						}
					}
				}
			}


			model.dispose();
			env.dispose();
		} catch (Exception e) {
			 e.printStackTrace();
		}
	}

	public Map<Integer, Map<Integer, Pair<String>>> getArc2idVertex2nameAndValue() {
		return arc2idVertex2nameAndValue;
	}

	public Map<Integer, Map<Integer, Map<Integer, Double>>> getDestination2vertex2edge2value() {
		return destination2vertex2edge2value;
	}

}
