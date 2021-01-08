package lpsolver;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class MCFSolverDAG {

	private double max = Double.MIN_VALUE;


	private Integer mostCongestedIdArc = null; 
	private Map<Pair<Integer>,Pair<String>> demand2nameAndValue;
	Map<Integer,Map<Integer,Pair<String>>> arc2idVertex2nameAndValue;
	private Map<Integer,Map<Integer,GRBVar>> vertex2edge2var;
	private Map<Integer,Double> arc2rLoad;
	private GRBVar alpha;
	Map<Integer,Map<Integer,Map<Integer,Double>>> destination2vertex2edge2value;

	public double getMax() {
		return max;
	}

	public void computeOptimalCongestion(Graph g, Map<Pair<Integer>,Double> demand2estimate,Map<Integer,Graph> destination2ht){
		this.computeOptimalCongestion(g, demand2estimate, destination2ht,false);

	}

	public void computeOptimalCongestion(Graph g, Map<Pair<Integer>,Double> demand2estimate,Map<Integer,Graph> destination2ht,boolean aggregateCongestion){

		this.arc2rLoad= new HashMap<Integer,Double>();
		destination2vertex2edge2value = new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();

		Set<Integer> destinationVertices = new TreeSet<Integer>();
		for(Pair<Integer> demand : demand2estimate.keySet())
			destinationVertices.add(demand.getSecond());
		
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
			boolean print = false;
			//print = true;

			this.vertex2edge2var = new HashMap<Integer,Map<Integer,GRBVar>>();
			for(Vertex destination: g.getVertices()){
				Map<Integer,GRBVar> edge2var = new HashMap<Integer,GRBVar>();
				vertex2edge2var.put(destination.getId(), edge2var);
				for(Arc arc : g.getArcs()){
					if(arc.getFirstEndPoint().getId()==destination.getId())
						continue;
					if(destination2ht.get(destination.getId()).getArcById(arc.getId())==null)
						continue;
					edge2var.put(arc.getId(), model.addVar(0,GRB.INFINITY, 0, GRB.CONTINUOUS,"g_"+destination.getId()+"_"+arc.getId()));
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
					if(print)System.out.print(" subject flow_constraint_"+destination.getId()+"_"+vertex.getId()+":");
					boolean atLeastOne = false;
					for(Arc outgoingArc : vertex.getArcs()){
						if(destination2ht.get(destination.getId()).getArcById(outgoingArc.getId())!=null){
							flows.addTerm(1, edge2var.get(outgoingArc.getId()));
							if(print)System.out.print(" + "+edge2var.get(outgoingArc.getId()).get(GRB.StringAttr.VarName));
							atLeastOne = true;
						}
						Arc incomingArc = g.getReversedArc(outgoingArc.getId());
						if(destination2ht.get(destination.getId()).getArcById(incomingArc.getId())!=null)
							if(incomingArc.getFirstEndPoint().getId() != destination.getId()){
								flows.addTerm(-1, edge2var.get(incomingArc.getId()));
								if(print)System.out.print(" - "+edge2var.get(incomingArc.getId()).get(GRB.StringAttr.VarName));
								atLeastOne = true;
							}
					}
					if(atLeastOne){
						model.addConstr(flows, GRB.GREATER_EQUAL, demand2estimate.get(new Pair<Integer>(vertex.getId(),destination.getId())),null);
						if(print)System.out.print(" >= " + demand2estimate.get(new Pair<Integer>(vertex.getId(),destination.getId())));
					}
					if(print)System.out.println();
				}
			}

			//add flow constraints per vertex
			for(Arc arc: g.getArcs()){
				if(aggregateCongestion && (arc.getId() % 2 == 1))
					continue;

				if(print)System.out.print("\t ("+arc.getFirstEndPoint().getId()+","+arc.getSecondEndPoint().getId()+") [");

				GRBLinExpr flows = new GRBLinExpr();
				flows.addTerm(arc.getCapacity(),this.alpha);
				if(print)System.out.print(arc.getCapacity()+ "*alpha ");
			
				boolean atLeastOne = false;
				for(Integer destinationId: destinationVertices){
					Vertex destination = g.getVertexById(destinationId);
					if(destination2ht.get(destinationId).getArcById(arc.getId())!=null){
						atLeastOne = true;
						flows.addTerm(-1,vertex2edge2var.get(destination.getId()).get(arc.getId()));
						if(print)System.out.print("- "+vertex2edge2var.get(destination.getId()).get(arc.getId()).get(GRB.StringAttr.VarName)+
								"  ");
					}
					if(aggregateCongestion && destination2ht.get(destinationId).getArcById(g.getReversedArc(arc.getId()).getId())!=null){
						atLeastOne = true;
						flows.addTerm(-1,vertex2edge2var.get(destination.getId()).get(g.getReversedArc(arc.getId()).getId()));
						if(print)System.out.print("- "+vertex2edge2var.get(destination.getId()).get(g.getReversedArc(arc.getId()).getId()).get(GRB.StringAttr.VarName)+
								"  ");
					}
				}
				if(atLeastOne){
					if(print)System.out.println(" >= 0");
					model.addConstr(flows, GRB.GREATER_EQUAL, 0,null);
				}else{
					if(print)System.out.println(" nothing" );
				}
			}

			/*		//add capacity constraints per edge
			//System.out.println("edge constraints:[");
			for(Arc arc2 : g.getArcs()){
				if(aggregateCongestion && (arc2.getId() % 2 == 1))
					continue;	
				//if(arc2.getFirstEndPoint().getId()>arc2.getSecondEndPoint().getId())
				//	continue;

				GRBLinExpr flows = new GRBLinExpr();
				boolean atLeastOne = false;
				for(Integer destinationId: destinationVertices){
//					Vertex destination = g.getVertexById(destinationId);
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

			 */
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
							if(sum==0)
								destination2vertex2edge2value.get(destinationId).get(vertexId).put(arcId,1d);
							else
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
