package lpsolver.signomial;
/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */


import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class MosekFormulationGeneratorAmplCompact implements IMosekFormulationGeneratorAmpl {

	private double min = Double.MIN_VALUE;
	private Integer mostCongestedIdArc = null; 
	Map<Integer,Map<Pair<Integer>,Pair<String>>> arc2demand2nameAndValue;
	Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction;

	private Map<Integer,Map<Integer,Integer>> pi_vertex2vertex2index;
	private Map<Integer,Integer>  w_edge2index;
	private Map<Pair<Integer>,Map<Integer,Integer>> f_demand2vertex2index;
	private Map<Pair<Integer>,Map<Integer,Integer>> y_demand2vertex2index;
	private Map<Pair<Integer>,Map<Integer,Integer>> g_demand2edge2index;
	private Map<Integer,Map<Integer,Map<Integer,Integer>>> phi_destination2vertex2edge2index;
	private SignomialSolution currentSolution;
	private Integer alphaIndex;

	public MosekFormulationGeneratorAmplCompact(){
		this.currentSolution = new SignomialSolution();
	}

	public void computeOptimalPerformance(String graphName, Graph g,
			List<Pair<Integer>> setOfDemands,
			Map<Integer,Graph> destination2ht ){

		try {
			//System.out.println(g);
			PrintWriter writer = new PrintWriter("formulation.mod", "UTF-8");


			Set<Integer> destinationVertices = new TreeSet<Integer>();
			Set<Integer> sourceVertices = new TreeSet<Integer>();
			for(Pair<Integer> demand : setOfDemands){
				destinationVertices.add(demand.getSecond());
				sourceVertices.add(demand.getFirst());
			}

			Map<Integer,Set<Integer>> destination2sources = new HashMap<Integer,Set<Integer>>();
			g.createDistanceMatrixAll();
			for(Pair<Integer> demand : setOfDemands){
				if(destination2sources.get(demand.getSecond())==null)
					destination2sources.put(demand.getSecond(), new TreeSet<Integer>());
				destination2sources.get(demand.getSecond()).add(demand.getFirst());
			}


			int indexCounter=1;
			List<Integer> blx = new LinkedList<Integer>();

			// CREATE PI VARIABLES pi_{v,t}
			this.pi_vertex2vertex2index = new HashMap<Integer,Map<Integer,Integer>>();


			for(Integer destId: destinationVertices){
				Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				this.pi_vertex2vertex2index.put(destId, vertex2index);
				for(Vertex j: destination2ht.get(destId).getVertices()){
					if(destId==j.getId())
						continue;
					vertex2index.put(j.getId(),indexCounter++);
					blx.add(Integer.MIN_VALUE);
					//System.out.println("var pi_"+arc.getId()+"_"+destId+"_"+j.getId()+";");
					writer.println("var pi_"+destId+"_"+j.getId()+";");
					//System.out.print("pi_"+arc.toStringShort()+"^("+i+","+j+")"+" ");

				}
			}
			//System.out.println();


			// CREATE W VARIABLES w_e(a)
			this.w_edge2index = new HashMap<Integer,Integer>();

			for(Arc arc2:getAllArcs(destination2ht)){
				this.w_edge2index.put(arc2.getId(),indexCounter++);
				blx.add(Integer.MIN_VALUE);
				//		System.out.println("blx = [-inf];");
				//System.out.println("var w_"+arc.getId()+"_"+arc2.getId()+";");
				writer.println("var w_"+arc2.getId()+";");
				//System.out.print("w_"+arc.toStringShort()+"^("+arc2.toStringShort()+" ");
			}
			//System.out.println();

			PrintWriter writerAmpl = new PrintWriter("signomial.run", "UTF-8");
			writerAmpl.println("model formulation2.mod");
			writerAmpl.println("option solver mosek;");
			writerAmpl.println("option mosek_options 'MSK_DPAR_INTPNT_NL_TOL_MU_RED = 0.1  MSK_DPAR_INTPNT_NL_TOL_PFEAS = 0.1 MSK_DPAR_INTPNT_NL_TOL_REL_GAP = 0.1';");
			writerAmpl.println("solve;");
			writerAmpl.print("display alpha;");
			writerAmpl.print("display ");

			// CREATE f, y, AND g VARIABLES f_(s,t)(v)
			this.f_demand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();
			this.y_demand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();

			boolean first = true;
			for(Pair<Integer> demand:setOfDemands){
				Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				this.f_demand2vertex2index.put(demand, vertex2index);
				Pair<Set<Integer>> hts = this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond()));
				for(Integer idVertex : hts.getFirst()){
					vertex2index.put(idVertex, indexCounter++);
					blx.add(Integer.MIN_VALUE);
					//System.out.println("blx = [-inf];");
					blx.add(Integer.MIN_VALUE);
					//System.out.println("blx = [-inf];");
					//System.out.println("var f_"+demand.getFirst()+"_"+demand.getSecond()+"_"+idVertex+";");
					writer.println("var f_"+demand.getFirst()+"_"+demand.getSecond()+"_"+idVertex+";");
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
					if(!first){
						writerAmpl.print(",");
					}else
						first=false;
					writerAmpl.print(" f_"+demand.getFirst()+"_"+demand.getSecond()+"_"+idVertex);
				}
			}
			writerAmpl.println(";");

			writerAmpl.print("display ");
			// CREATE phi
			first=true;
			this.phi_destination2vertex2edge2index = new HashMap<Integer,Map<Integer,Map<Integer,Integer>>>();
			for(Integer destId:destinationVertices){
				Map<Integer,Map<Integer,Integer>> vertex2edge2index = new HashMap<Integer,Map<Integer,Integer>>();
				this.phi_destination2vertex2edge2index.put(destId, vertex2edge2index);
				for(Vertex v : destination2ht.get(destId).getVertices()){
					Map<Integer,Integer> edge2index = new HashMap<Integer,Integer>();
					vertex2edge2index.put(v.getId(), edge2index);
					for(Arc arc: v.getArcs()){
						edge2index.put(arc.getId(), indexCounter++);
						blx.add(Integer.MIN_VALUE);
						//System.out.println("blx = [-inf];");
						//System.out.println("var phi_"+destId+"_"+v.getId()+"_"+arc.getId()+";");
						writer.println("var phi_"+destId+"_"+v.getId()+"_"+arc.getId()+";");
						if(!first){
							writerAmpl.print(",");
						}else
							first=false;
						writerAmpl.print(" phi_"+destId+"_"+v.getId()+"_"+arc.getId());
					}
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
				}
			}
			writerAmpl.println(";");
			writerAmpl.close();

			//System.out.println();

			// CREATE alpha VARIABLE
			this.alphaIndex = indexCounter++;
			//System.out.println("var alpha;");
			writer.println("var alpha;");
			blx.add(0);
			//System.out.println("blx = [0];");

			/*

			// count number of constraints
			int numberOfConstraints = 0;
			int numberOfNonZeroVariables=0;
			Integer temp=0;
			for(Pair<Integer> demand : setOfDemands){
				temp+=this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond())).getFirst().size()-1;
			}
			numberOfNonZeroVariables+=2*temp;
			numberOfConstraints+=2*temp;
			System.out.println("% number of constraints: " + numberOfConstraints);
			System.out.println("% number of non-zero variables: " + numberOfNonZeroVariables);
			numberOfNonZeroVariables+=3*temp*g.getMaxDegree();
			numberOfConstraints+=temp*g.getMaxDegree();

			System.out.println("% number of constraints: " + numberOfConstraints);
			System.out.println("% number of non-zero variables: " + numberOfNonZeroVariables);
			numberOfConstraints+=temp;
			numberOfNonZeroVariables+=temp*g.getMaxDegree();
			System.out.println("% number of constraints: " + numberOfConstraints);
			System.out.println("% number of non-zero variables: " + numberOfNonZeroVariables);
			numberOfConstraints+=g.getArcs().size();
			numberOfNonZeroVariables+=g.getArcs().size()*(g.getArcs().size()+1);
			System.out.println("% number of constraints: " + numberOfConstraints);
			System.out.println("% number of non-zero variables: " + numberOfNonZeroVariables);
			temp=0;
			for(Integer destination : destination2ht.keySet()){
				temp+=destination2ht.get(destination).getArcs().size();
			}
			numberOfConstraints+=g.getArcs().size()*temp;
			numberOfNonZeroVariables+=temp*3;
			System.out.println("% number of constraints: " + numberOfConstraints);
			System.out.println("% number of non-zero variables: " + numberOfNonZeroVariables);
			for(Arc arc: g.getArcs()){
				for(Integer destination : destinationVertices){
					Set<Integer> arcsSource = this.buildSE(arc.getId(), destination2ht.get(destination), sourceVertices);
					if(arcsSource!=null){
						numberOfConstraints+=arcsSource.size();
						numberOfNonZeroVariables+=temp*3;
					}
				}
			}
			System.out.println("% number of constraints: " + numberOfConstraints);
			System.out.println("% number of non-zero variables: " + numberOfNonZeroVariables);



			System.out.println("% Specify the linear part of the problem."); 
			System.out.println(); 
			//define che objective function
			System.out.println("c = [];");
			for(int i=1;i<=indexCounter;i++){
				if(i!=alphaIndex)
					System.out.println("c(end+1)=0;");
				else
					System.out.println("c(end+1)=1;");

			}

			//define the linear constraint matrix
			List<String> opr = new LinkedList<String>();

			List<Integer> opri = new LinkedList<Integer>();
			System.out.println("opri = [];");
			List<Integer> oprj = new LinkedList<Integer>();
			System.out.println("oprj = [];");
			List<Integer> oprf = new LinkedList<Integer>();
			System.out.println("oprf = [];");
			List<Integer> oprg = new LinkedList<Integer>();
			System.out.println("opg = [];");
			List<Double> blc = new LinkedList<Double>();
			System.out.println("blc = [];");
			System.out.println("buc = [];");

			System.out.println("a = spalloc("+numberOfConstraints+","+indexCounter+","+numberOfNonZeroVariables+");");
			List<Triplet<Integer,Double>> entriesInMatrixA = new LinkedList<Triplet<Integer,Double>>();
			//set of constraints to indices in the matrix
			Map<Pair<Integer>,Map<Integer,Integer>> firstConstraintDemand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();
			Map<Pair<Integer>,Map<Integer,Integer>> secondConstraintDemand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();
			Map<Pair<Integer>,Map<Integer,Integer>> thirdConstraintDemand2edge2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();
			Map<Integer,Map<Integer,Integer>> fourthConstraintDestination2vertex2index = new HashMap<Integer,Map<Integer,Integer>>();
			Map<Integer,Integer> fifthConstraintArc2index = new HashMap<Integer,Integer>();
			Map<Integer,Map<Integer,Map<Integer,Integer>>> sixthConstraintEdge2destination2edge2index = new HashMap<Integer,Map<Integer,Map<Integer,Integer>>>();
			Map<Integer,Map<Integer,Integer>> seventhConstraintEdge2edge2index = new HashMap<Integer,Map<Integer,Integer>>();
			Map<Integer,Map<Integer,Map<Integer,Integer>>> eighthConstraintEdge2destination2vertex2index = new HashMap<Integer,Map<Integer,Map<Integer,Integer>>>();
			 */

			//System.out.println("minimize congestion: alpha;");
			writer.println("minimize congestion: alpha;");

			int indexConstraint = 1;
			//first type of constraint
			for(Pair<Integer> demand : setOfDemands){
				//Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				//firstConstraintDemand2vertex2index.put(demand, vertex2index);
				Pair<Set<Integer>> hts = this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond()));
				for(Integer vertexId: this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond())).getFirst()){
					if(vertexId!=demand.getSecond()){
						//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.f_demand2vertex2index.get(demand).get(vertexId),1d));
						//vertex2index.put(vertexId, indexConstraint);
						//System.out.print("subject to flow_constraint_"+demand.getFirst()+"_"+demand.getSecond()+"_"+vertexId+": f_"+demand.getFirst()+"_"+demand.getSecond()+"_"+vertexId );
						writer.print("subject to flow_constraint_"+demand.getFirst()+"_"+demand.getSecond()+"_"+vertexId+": f_"+demand.getFirst()+"_"+demand.getSecond()+"_"+vertexId );

						//System.out.println("% "+indexConstraint+",f_{"+demand.getFirst()+","+demand.getSecond()+"}("+vertexId+")");
						//System.out.println("a("+indexConstraint+","+this.f_demand2vertex2index.get(demand).get(vertexId)+")=1");
						if(vertexId!=demand.getFirst()){
							//System.out.print(" - log ( ");
							writer.print(" - log ( ");

							for(Arc arc: g.getVertexById(vertexId).getArcs()){
								Arc incomingArcAtV = g.getReversedArc(arc.getId());
								if(hts.getSecond().contains(incomingArcAtV.getId())){
									//System.out.print(" + exp ( ");
									writer.print(" + exp ( ");
									//System.out.print("phi_"+demand.getSecond()+"_"+incomingArcAtV.getFirstEndPoint().getId()+"_"+incomingArcAtV.getId()+""
									//		+ "+ f_"+demand.getFirst()+"_"+demand.getSecond()+"_"+incomingArcAtV.getFirstEndPoint().getId()+ ")");
									writer.print("phi_"+demand.getSecond()+"_"+incomingArcAtV.getFirstEndPoint().getId()+"_"+incomingArcAtV.getId()+""
											+ "+ f_"+demand.getFirst()+"_"+demand.getSecond()+"_"+incomingArcAtV.getFirstEndPoint().getId()+ ")");
								}
							}
							//System.out.print(" )");
							writer.print(" )");
						}
						//System.out.println("  >= 0; ");
						writer.println("  >= 0; ");
						indexConstraint++;
					}
				}
			}




			PrintWriter writerSplittingRatio = new PrintWriter("splitting-ratio-formulation-data.txt", "UTF-8");
			//fourth type of constraint
			for(Integer destId : destinationVertices){
				//Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				//fourthConstraintDestination2vertex2index.put(destId, vertex2index);
				for(Vertex v: destination2ht.get(destId).getVertices()){
					if(v.getId()!=destId){
						//System.out.print("subject to splitting_ratio_constraint_"+destId+"_"+v.getId()+": ");
						//Map<Integer,Double> edge2value = currentSolution.getA(destId, v.getId());
						for(Arc arc: v.getArcs()){
							//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(destId).get(v.getId()).get(arc.getId()),edge2value.get(arc.getId())));
							//System.out.print(" + a_"+destId+"_"+arc.getId() +" * phi_"+destId+"_"+arc.getId()+" ");
							//System.out.println(destId+" "+v.getId() + " "+ arc.getId()+ " " + 1d/v.getArcs().size());
							writerSplittingRatio.println(destId+" "+v.getId() + " "+ arc.getId() + " " + Math.log(1d/v.getArcs().size()));
							//vertex2index.put(v.getId(), indexConstraint);
						}
						//blc.add(-Math.log(currentSolution.getK(destId,v.getId())));
						//System.out.println(" >= -"+Math.log(currentSolution.getK(destId,v.getId()))+";");
						//System.out.println(" >= - k_"+destId+"_"+v.getId()+" ;");
						indexConstraint++;
					}
				}
			}
			writerSplittingRatio.close();



			//sixth type of constraint
			/*for(Arc arc: g.getArcs()){
				Map<Integer,Map<Integer,Integer>> destination2edge2index = new HashMap<Integer,Map<Integer,Integer>>();
				//sixthConstraintEdge2destination2edge2index.put(arc.getId(), destination2edge2index);
				for(Integer destId : destinationVertices){
					Map<Integer,Integer> edge2index = new HashMap<Integer,Integer>();
					destination2edge2index.put(destId,edge2index);
					for(Arc arc2 : destination2ht.get(destId).getArcs()){
						if(arc2.getSecondEndPoint().getId()!=destId){
							System.out.print("subject to dualpotential_"+arc.getId()+"_"+destId+"_"+arc2.getId()+":  ");
							System.out.print(" w_"+arc.getId()+"_"+arc2.getId() + " ");
							System.out.print(" + pi_"+arc.getId()+"_"+destId+"_"+arc2.getSecondEndPoint().getId() + " ");
							System.out.print(" - pi_"+arc.getId()+"_"+destId+"_"+arc2.getFirstEndPoint().getId() + " ");
							System.out.println(" >= 0;");

							//edge2index.put(arc2.getId(), indexConstraint);
							//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.w_edge2edge2index.get(arc.getId()).get(arc2.getId()),1d));
							//System.out.println("a("+indexConstraint+","+this.w_edge2edge2index.get(arc.getId()).get(arc2.getId())+")=1");
							//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getSecondEndPoint().getId()),1d));
							//System.out.println("a("+indexConstraint+","+this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getSecondEndPoint().getId())+")=1");
							//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getFirstEndPoint().getId()),-1d));
							//System.out.println("a("+indexConstraint+","+this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getFirstEndPoint().getId())+")=-1");

							//blc.add(0d);
							//System.out.println("blc(end+1)=0;");
							//System.out.println("buc(end+1)=inf;");
							indexConstraint++;
						}
					}
				}
			}*/


			//seventh type of constraint
			Set<Integer> analyzedArcs = new HashSet<Integer>();
			for(Integer destId : destinationVertices){

				//get the selected source vertices
				//for each arc in hs(t) - do I have it?
				for(Arc arc2 : destination2ht.get(destId).getArcs()){
					Integer arcId2 = arc2.getId();
					if(analyzedArcs.contains(arcId2))
						continue;
					analyzedArcs.add(arcId2);
					//System.out.print("subject to dualpotential_"+arc.getId()+"_"+destId+"_"+arcId2+":  ");
					writer.print("subject to dualpotential_"+destId+"_"+arcId2+":  ");
					//System.out.print(" w_"+arc.getId()+"_"+arcId2 + " ");
					writer.print(" w_"+arcId2 + " ");
					if(arc2.getSecondEndPoint().getId()!=destId){
						//System.out.print(" + pi_"+arc.getId()+"_"+destId+"_"+arc2.getSecondEndPoint().getId() + " ");
						writer.print(" + pi_"+destId+"_"+arc2.getSecondEndPoint().getId() + " ");
					}
					//System.out.print(" - pi_"+arc.getId()+"_"+destId+"_"+arc2.getFirstEndPoint().getId() + " ");
					writer.print(" - pi_"+destId+"_"+arc2.getFirstEndPoint().getId() + " ");
					//System.out.println(" >= 0;");
					writer.println(" >= 0;");
				}
			}					

			//fifth type of constraint
			//fifthConstraintArc2index.put(arc.getId(), indexConstraint);
			//System.out.print("subject to dualobjective_"+arc.getId()+": alpha ");
			writer.print("subject to dualobjective: alpha ");
			//				entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,alphaIndex,1d));
			for(Integer arcId2:analyzedArcs){
				Arc arc2 = g.getArcById(arcId2);
				//System.out.print( " - "+ arc2.getCapacity() + " * w_"+arc.getId()+"_"+arc2.getId() + " ");
				writer.print( " - "+ arc2.getCapacity() + " * w_"+arc2.getId() + " ");

				indexConstraint++;
			}
			//System.out.println(" >= 0;");
			writer.println(" >= 0;");


			//eighth type of constraint
			//eighthConstraintEdge2destination2vertex2index.put(arc.getId(), destination2source2index);
			for(Integer destId : destinationVertices){
				Map<Integer,Integer> source2index = new HashMap<Integer,Integer>();
				for(Integer sourceId:destination2sources.get(destId)){

					writer.print("subject to dualdemandpotential_"+destId+"_"+sourceId+":  ");
					writer.print(  " pi_"+destId + "_"+sourceId+"  ");
					for(Integer arcId : this.buildHTS(sourceId, destination2ht.get(destId)).getSecond()){
						Arc arc = g.getArcById(arcId);
						//System.out.print("subject to dualdemandpotential_"+arc.getId()+"_"+destId+"_"+sourceId+":  ");
						//System.out.print( arc.getCapacity() + " * pi_"+arc.getId()+"_"+destId + "_"+sourceId+"  ");
						//System.out.print(" - exp( f_"+sourceId+"_"+destId + "_" + arc.getFirstEndPoint().getId()+" ");
						writer.print(" - (1/"+arc.getCapacity()+") *exp( f_"+sourceId+"_"+destId + "_" + arc.getFirstEndPoint().getId()+" ");
						//System.out.print(" + phi_"+destId+"_"+arc.getFirstEndPoint().getId()+"_"+arc.getId() + " ) ");
						writer.print(" + phi_"+destId+"_"+arc.getFirstEndPoint().getId()+"_"+arc.getId() + " ) ");
						//System.out.println(" >= 0;");
					}
					writer.println(" >= 0;");

					//source2index.put(sourceId,indexConstraint);
					//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(sourceId),arc.getCapacity()));
					//System.out.println("a("+indexConstraint+","+this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(sourceId)+")="+((int)arc.getCapacity())+"");
					//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(destId).get(arc.getFirstEndPoint().getId()).get(arc.getId()),-1d));
					//System.out.println("a("+indexConstraint+","+this.phi_destination2vertex2edge2index.get(destId).get(arc.getFirstEndPoint().getId()).get(arc.getId())+")=-1");
					//entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.f_demand2vertex2index.get(new Pair<Integer>(sourceId,destId)).get(arc.getFirstEndPoint().getId()),-1d));
					//System.out.println("a("+indexConstraint+","+this.f_demand2vertex2index.get(new Pair<Integer>(sourceId,destId)).get(arc.getFirstEndPoint().getId())+")=-1");
					//blc.add(0d);	
					//System.out.println("blc(end+1)=0;");
					//System.out.println("buc(end+1)=inf;");
					indexConstraint++;
				}
			}

				for(Integer destId :  this.pi_vertex2vertex2index.keySet()){
					for(Integer vertexId : this.pi_vertex2vertex2index.get(destId).keySet()){
						//System.out.println("subject to variablebound_pi_"+arcId+"_"+destId+"_"+vertexId+": pi_"+arcId+"_"+destId+"_"+vertexId+" >=0;");
						writer.println("subject to variablebound_pi_"+destId+"_"+vertexId+": pi_"+destId+"_"+vertexId+" >=0;");
					}
				}
			

				for(Integer arcId2 :  this.w_edge2index.keySet()){
					//System.out.println("subject to variablebound_w_"+arcId+"_"+arcId2+": w_"+arcId+"_"+arcId2+" >=0;");
					writer.println("subject to variablebound_w_"+arcId2+": w_"+arcId2+" >=0;");
				}

			/*
			 * System.out.print("opr = [");
			for(int i=0;i<opri.size();i++){
				System.out.print("'"+opr.get(i)+"'");
				if(i<opri.size()-1)
					System.out.print(",");
			}
			System.out.println("];");

			System.out.println("[res] = mskscopt(opr,opri,oprj,oprf,oprg,c,a,blc,buc,blx);");  
			System.out.println();

			System.out.println("% Print the solution. ");
			System.out.println("res.sol.itr.xx ");*/
			writer.close();
		} catch (Exception e) {
			System.out.println("Error code: " + e.getMessage());
		}
	}





	private Set<Integer> getSourcesToDestination(
			List<Pair<Integer>> setOfDemands, Integer destId) {
		Set<Integer> sources = new TreeSet<Integer>();
		for (Pair<Integer> demand: setOfDemands){
			if(demand.getSecond()==destId)
				sources.add(demand.getFirst());
		}
		return sources;
	}

	private Set<Arc> getAllArcs(Map<Integer, Graph> destination2ht) {
		Set<Arc> arcs = new TreeSet<Arc>();
		for(Integer destination : destination2ht.keySet()){
			for(Arc arc : destination2ht.get(destination).getArcs())
				arcs.add(arc);
		}
		return arcs;
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

	// return D_t(e)
	public Set<Integer> buildSE(Integer arcId, Graph ht, Set<Integer> sources, Graph g){
		Set<Integer> selectedSources = new TreeSet<Integer>();
		Arc arc = ht.getArcById(arcId);
		if(arc==null)
			return selectedSources;
		Set<Integer> analyzed = new TreeSet<Integer>();
		List<Arc> queue = new LinkedList<Arc>();
		Set<Integer> queued = new TreeSet<Integer>();
		queue.add(arc);
		queued.add(arc.getId());
		while(!queue.isEmpty()){
			Arc next = queue.remove(0);
			if(analyzed.contains(next.getId()))
				continue;
			analyzed.add(next.getId());
			if(sources.contains(next.getFirstEndPoint().getId()))
				selectedSources.add(next.getFirstEndPoint().getId());
			Vertex from = next.getFirstEndPoint();
			Vertex fromTemp = g.getVertexById(from.getId());
			for(Arc fromArc : fromTemp.getArcs()){
				Arc incomingFromArc = g.getReversedArc(fromArc.getId());
				if(ht.getArcs().contains(incomingFromArc)){
					if(!queued.contains(incomingFromArc.getId())){
						queue.add(incomingFromArc);
						queued.add(incomingFromArc.getId());
					}
				}
			}
		}
		return selectedSources;
	}

	// return H_t(s)
	public Pair<Set<Integer>> buildHTS(Integer sourceId, Graph ht){
		Vertex v = ht.getVertexById(sourceId);
		Set<Integer> analyzed = new TreeSet<Integer>();
		Set<Integer> arcs = new TreeSet<Integer>();
		List<Vertex> queue = new LinkedList<Vertex>();
		Set<Integer> queued = new TreeSet<Integer>();
		queue.add(v);
		queued.add(v.getId());
		while(!queue.isEmpty()){
			Vertex next = queue.remove(0);
			if(analyzed.contains(next.getId()))
				continue;
			analyzed.add(next.getId());
			for(Arc toArc : next.getArcs()){
				arcs.add(toArc.getId());
				if(!queued.contains(toArc.getSecondEndPoint().getId())){
					queue.add(toArc.getSecondEndPoint());
					queued.add(toArc.getSecondEndPoint().getId());
				}
			}
		}
		return new Pair<Set<Integer>>(analyzed,arcs);
	}

	@Override
	public void computeOptimalPerformance(String graphName, Graph g,
			List<Pair<Integer>> setOfDemands,
			Map<Integer, Graph> destination2ht,
			Map<Pair<Integer>, Double> demand2estimate, double w) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void computeOptimalPerformance(String graphName, Graph g,
			List<Pair<Integer>> setOfDemands,
			Map<Integer, Graph> destination2ht, boolean aggregateCongestion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void computeOptimalPerformance(String graphName, Graph g,
			List<Pair<Integer>> setOfDemands,
			Map<Integer, Graph> destination2ht,
			Map<Pair<Integer>, Double> demand2estimate, double w,
			boolean aggregateCongestion) {
		// TODO Auto-generated method stub
		
	}
}