package lpsolver.signomial;
/* Copyright 2014, Gurobi Optimization, Inc. */

/* Solve the classic diet model, showing how to add constraints
   to an existing model. */


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
import util.Triplet;

public class MosekFormulationGenerator {

	private double min = Double.MIN_VALUE;
	private Integer mostCongestedIdArc = null; 
	Map<Integer,Map<Pair<Integer>,Pair<String>>> arc2demand2nameAndValue;
	Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction;

	private Map<Integer,Map<Integer,Map<Integer,Integer>>> pi_edge2vertex2vertex2index;
	private Map<Integer,Map<Integer,Integer>>  w_edge2edge2index;
	private Map<Pair<Integer>,Map<Integer,Integer>> f_demand2vertex2index;
	private Map<Pair<Integer>,Map<Integer,Integer>> y_demand2vertex2index;
	private Map<Pair<Integer>,Map<Integer,Integer>> g_demand2edge2index;
	private Map<Integer,Map<Integer,Map<Integer,Integer>>> phi_destination2vertex2edge2index;
	private SignomialSolution currentSolution;
	private Integer alphaIndex;

	public MosekFormulationGenerator(){
		this.currentSolution = new SignomialSolution();
	}

	public void computeOptimalPerformance(Graph g,
			List<Pair<Integer>> setOfDemands,
			Map<Integer,Graph> destination2ht ){

		try {

			Set<Integer> destinationVertices = new TreeSet<Integer>();
			Set<Integer> sourceVertices = new TreeSet<Integer>();
			for(Pair<Integer> demand : setOfDemands){
				destinationVertices.add(demand.getSecond());
				sourceVertices.add(demand.getFirst());
			}

			currentSolution.initialize(g, setOfDemands,destinationVertices,destination2ht);
			
			int indexCounter=1;
			List<Integer> blx = new LinkedList<Integer>();
			System.out.println("blx = [];");
			
			// CREATE PI VARIABLES pi_{v,t}(e)
			this.pi_edge2vertex2vertex2index = new HashMap<Integer,Map<Integer,Map<Integer,Integer>>>();
			
			for(Arc arc:g.getArcs()){
				Map<Integer,Map<Integer,Integer>> vertex2vertex2index = new HashMap<Integer,Map<Integer,Integer>>();
				pi_edge2vertex2vertex2index.put(arc.getId(), vertex2vertex2index);

				for(Integer destId: destinationVertices){
					Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
					vertex2vertex2index.put(destId, vertex2index);
					for(Vertex j: destination2ht.get(destId).getVertices()){
						if(destId==j.getId())
							continue;
						vertex2index.put(j.getId(),indexCounter++);
						blx.add(Integer.MIN_VALUE);
						System.out.println("blx = [-inf];");
						//System.out.print("pi_"+arc.toStringShort()+"^("+i+","+j+")"+" ");

					}
				}
				//System.out.println();
			}


			// CREATE W VARIABLES w_e(a)
			this.w_edge2edge2index = new HashMap<Integer,Map<Integer,Integer>>();

			for(Arc arc:g.getArcs()){
				Map<Integer,Integer> edge2index = new HashMap<Integer,Integer>();
				this.w_edge2edge2index.put(arc.getId(), edge2index); 
				for(Arc arc2:getAllArcs(destination2ht)){
					edge2index.put(arc2.getId(),indexCounter++);
					blx.add(Integer.MIN_VALUE);
					System.out.println("blx = [-inf];");
					//System.out.print("w_"+arc.toStringShort()+"^("+arc2.toStringShort()+" ");
				}
				//System.out.println();
			}

			// CREATE f, y, AND g VARIABLES f_(s,t)(v)
			this.f_demand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();
			this.y_demand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();
			this.g_demand2edge2index = new HashMap<Pair<Integer>,Map<Integer,Integer>>();

			for(Pair<Integer> demand:setOfDemands){
				Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				Map<Integer,Integer> vertex2indexB = new HashMap<Integer,Integer>();
				Map<Integer,Integer> edge2indexC = new HashMap<Integer,Integer>();
				this.f_demand2vertex2index.put(demand, vertex2index);
				this.y_demand2vertex2index.put(demand, vertex2indexB);
				this.g_demand2edge2index.put(demand, edge2indexC);
				Pair<Set<Integer>> hts = this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond()));
				for(Integer idVertex : hts.getFirst()){
					vertex2index.put(idVertex, indexCounter++);
					blx.add(Integer.MIN_VALUE);
					System.out.println("blx = [-inf];");
					vertex2indexB.put(idVertex, indexCounter++);
					blx.add(Integer.MIN_VALUE);
					System.out.println("blx = [-inf];");
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
				}
				for(Integer idArc : hts.getSecond()){
					edge2indexC.put(idArc, indexCounter++);
					blx.add(Integer.MIN_VALUE);
					System.out.println("blx = [-inf];");
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
				}
			}

			// CREATE phi
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
						System.out.println("blx = [-inf];");
					}
					//System.out.print("x_"+arc.toStringShort()+"^("+demand.getFirst()+","+demand.getSecond()+")"+" ");
				}
			}

			//System.out.println();

			// CREATE alpha VARIABLE
			this.alphaIndex = indexCounter++;
			blx.add(0);
			System.out.println("blx = [0];");

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

			int indexConstraint = 1;
			//first type of constraint
			for(Pair<Integer> demand : setOfDemands){
				Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				firstConstraintDemand2vertex2index.put(demand, vertex2index);
				for(Integer vertexId: this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond())).getFirst()){
					if(vertexId!=demand.getSecond()){
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.f_demand2vertex2index.get(demand).get(vertexId),1d));
						vertex2index.put(vertexId, indexConstraint);
						System.out.println("% "+indexConstraint+",f_{"+demand.getFirst()+","+demand.getSecond()+"}("+vertexId+")");
						System.out.println("a("+indexConstraint+","+this.f_demand2vertex2index.get(demand).get(vertexId)+")=1");
						if(vertexId!=demand.getFirst()){
							opr.add("log");
							//System.out.println("opr(end+1)='log';");
							opri.add(indexConstraint);
							System.out.println("opri(end+1)="+indexConstraint+";");
							oprj.add(this.y_demand2vertex2index.get(demand).get(vertexId));
							System.out.println("oprj(end+1)="+this.y_demand2vertex2index.get(demand).get(vertexId)+";");
							oprf.add(-1);
							System.out.println("oprf(end+1)=-1;");
							oprg.add(0);
							System.out.println("oprg(end+1)=0;");
							System.out.println("% "+indexConstraint+",-log y_{"+demand.getFirst()+","+demand.getSecond()+"}("+vertexId+")");
						}
						blc.add(0d);
						System.out.println("blc(end+1)=0;");
						System.out.println("buc(end+1)=inf;");
						indexConstraint++;
					}
				}
			}
			
			//second type of constraint
			for(Pair<Integer> demand : setOfDemands){
				Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				secondConstraintDemand2vertex2index.put(demand, vertex2index);
				Pair<Set<Integer>> hts = this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond()));
				for(Integer vertexId: hts.getFirst()){
					if(vertexId!=demand.getSecond() && vertexId!=demand.getFirst()){
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.y_demand2vertex2index.get(demand).get(vertexId),1d));
						System.out.println("a("+indexConstraint+","+this.y_demand2vertex2index.get(demand).get(vertexId)+")=1");
						System.out.println("% "+indexConstraint+",y_{"+demand.getFirst()+","+demand.getSecond()+"}("+vertexId+")");
						vertex2index.put(vertexId, indexConstraint);
						for(Arc arc: g.getVertexById(vertexId).getArcs()){
							Arc incomingArcAtV = g.getReversedArc(arc.getId());
							if(hts.getSecond().contains(incomingArcAtV.getId())){
								opr.add("exp");
								//System.out.println("opr(end+1)='exp';");
								opri.add(indexConstraint);
								System.out.println("opri(end+1)="+indexConstraint+";");
								oprj.add(this.g_demand2edge2index.get(demand).get(incomingArcAtV.getId()));
								System.out.println("oprj(end+1)="+this.g_demand2edge2index.get(demand).get(incomingArcAtV.getId())+";");
								oprf.add(-1);
								System.out.println("oprf(end+1)=-1;");
								oprg.add(1);
								System.out.println("oprg(end+1)=1;");
								System.out.println("% "+indexConstraint+",-e^g_{"+demand.getFirst()+","+demand.getSecond()+"}("+incomingArcAtV.getFirstEndPoint()+","+incomingArcAtV.getSecondEndPoint()+")");
							}
						}
						blc.add(0d);
						System.out.println("blc(end+1)=0;");
						System.out.println("buc(end+1)=inf;");
						indexConstraint++;
					}
				}

			}
			
			
			//third type of constraint
			for(Pair<Integer> demand : setOfDemands){
				Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				thirdConstraintDemand2edge2index.put(demand, vertex2index);
				Pair<Set<Integer>> hts = this.buildHTS(demand.getFirst(), destination2ht.get(demand.getSecond()));
				for(Integer edgeId: hts.getSecond()){
					if(g.getArcById(edgeId).getSecondEndPoint().getId()!=demand.getSecond()){
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.g_demand2edge2index.get(demand).get(edgeId),1d));
						System.out.println("a("+indexConstraint+","+this.g_demand2edge2index.get(demand).get(edgeId)+")=1");
						System.out.println("% "+indexConstraint+",g_{"+demand.getFirst()+","+demand.getSecond()+"}("+g.getArcById(edgeId).getFirstEndPoint().getId()+","+g.getArcById(edgeId).getSecondEndPoint().getId()+")");
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(demand.getSecond()).get(g.getArcById(edgeId).getFirstEndPoint().getId()).get(edgeId),-1d));
						System.out.println("a("+indexConstraint+","+this.phi_destination2vertex2edge2index.get(demand.getSecond()).get(g.getArcById(edgeId).getFirstEndPoint().getId()).get(edgeId)+")=-1");
						System.out.println("% "+indexConstraint+",-phi_{"+demand.getSecond()+"}("+g.getArcById(edgeId).getFirstEndPoint().getId()+","+g.getArcById(edgeId).getSecondEndPoint().getId()+")");
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.f_demand2vertex2index.get(demand).get(g.getArcById(edgeId).getFirstEndPoint().getId()),-1d));
						System.out.println("a("+indexConstraint+","+this.f_demand2vertex2index.get(demand).get(g.getArcById(edgeId).getFirstEndPoint().getId())+")=-1");
						System.out.println("% "+indexConstraint+",-f_{"+demand.getFirst()+","+demand.getSecond()+"}("+g.getArcById(edgeId).getSecondEndPoint().getId()+")");
						vertex2index.put(edgeId, indexConstraint);
						blc.add(0d);
						System.out.println("blc(end+1)=0;");
						System.out.println("buc(end+1)=inf;");
						indexConstraint++;
					}
				}
			}

			//fourth type of constraint
			for(Integer destId : destinationVertices){
				Map<Integer,Integer> vertex2index = new HashMap<Integer,Integer>();
				fourthConstraintDestination2vertex2index.put(destId, vertex2index);
				for(Vertex v: destination2ht.get(destId).getVertices()){
					if(v.getId()!=destId){
						Map<Integer,Double> edge2value = currentSolution.getA(destId, v.getId());
						for(Arc arc: v.getArcs()){
							entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(destId).get(v.getId()).get(arc.getId()),edge2value.get(arc.getId())));
							System.out.println("a("+indexConstraint+","+this.phi_destination2vertex2edge2index.get(destId).get(v.getId()).get(arc.getId())+")="+edge2value.get(arc.getId())+"");	
							vertex2index.put(v.getId(), indexConstraint);
						}
						blc.add(-Math.log(currentSolution.getK(destId,v.getId())));
						System.out.println("blc(end+1)=-"+Math.log(currentSolution.getK(destId,v.getId()))+";");
						System.out.println("buc(end+1)=inf;");
						indexConstraint++;
					}
				}
			}

			
			//fifth type of constraint
			for(Arc arc: g.getArcs()){
				fifthConstraintArc2index.put(arc.getId(), indexConstraint);
				entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,alphaIndex,1d));
				for(Arc arc2:getAllArcs(destination2ht)){
					entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.w_edge2edge2index.get(arc.getId()).get(arc2.getId()),-arc2.getCapacity()));
					System.out.println("a("+indexConstraint+","+this.w_edge2edge2index.get(arc.getId()).get(arc2.getId())+")=-"+((int)arc2.getCapacity())+"");
					blc.add(0d);
					System.out.println("blc(end+1)=0;");
					System.out.println("buc(end+1)=inf;");
					indexConstraint++;
				}
			}

			//sixth type of constraint
			for(Arc arc: g.getArcs()){
				Map<Integer,Map<Integer,Integer>> destination2edge2index = new HashMap<Integer,Map<Integer,Integer>>();
				sixthConstraintEdge2destination2edge2index.put(arc.getId(), destination2edge2index);
				for(Integer destId : destinationVertices){
					Map<Integer,Integer> edge2index = new HashMap<Integer,Integer>();
					destination2edge2index.put(destId,edge2index);
					for(Arc arc2 : destination2ht.get(destId).getArcs()){
						if(arc2.getSecondEndPoint().getId()!=destId){
							edge2index.put(arc2.getId(), indexConstraint);
							entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.w_edge2edge2index.get(arc.getId()).get(arc2.getId()),1d));
							System.out.println("a("+indexConstraint+","+this.w_edge2edge2index.get(arc.getId()).get(arc2.getId())+")=1");
							entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getSecondEndPoint().getId()),1d));
							System.out.println("a("+indexConstraint+","+this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getSecondEndPoint().getId())+")=1");
							entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getFirstEndPoint().getId()),-1d));
							System.out.println("a("+indexConstraint+","+this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getFirstEndPoint().getId())+")=-1");
							
							blc.add(0d);
							System.out.println("blc(end+1)=0;");
							System.out.println("buc(end+1)=inf;");
							indexConstraint++;
						}
					}
				}
			}

			//seventh type of constraint
			for(Arc arc: g.getArcs()){
				Map<Integer,Integer> edge2index = new HashMap<Integer,Integer>();
				seventhConstraintEdge2edge2index.put(arc.getId(), edge2index);
				for(Integer destId : destinationVertices){
					for(Arc arc2 : destination2ht.get(destId).getArcs()){
						edge2index.put(arc2.getId(),indexConstraint);
						if(arc2.getSecondEndPoint().getId()==destId){
							edge2index.put(arc2.getId(), indexConstraint);
							entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.w_edge2edge2index.get(arc.getId()).get(arc2.getId()),1d));
							System.out.println("a("+indexConstraint+","+this.w_edge2edge2index.get(arc.getId()).get(arc2.getId())+")=1");
							entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getFirstEndPoint().getId()),-1d));
							System.out.println("a("+indexConstraint+","+this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(arc2.getFirstEndPoint().getId())+")=-1");
							blc.add(0d);	
							System.out.println("blc(end+1)=0;");
							System.out.println("buc(end+1)=inf;");
							indexConstraint++;
						}
					}
				}
			}

			//eighth type of constraint
			for(Arc arc: g.getArcs()){
				Map<Integer,Map<Integer,Integer>> destination2source2index = new HashMap<Integer,Map<Integer,Integer>>();
				eighthConstraintEdge2destination2vertex2index.put(arc.getId(), destination2source2index);
				for(Integer destId : destinationVertices){
					Map<Integer,Integer> source2index = new HashMap<Integer,Integer>();
					destination2source2index.put(destId,source2index);
					Set<Integer> sourcesInSE = this.buildSE(arc.getId(), destination2ht.get(destId), sourceVertices);
					if(sourcesInSE==null)
						continue;
					for(Integer sourceId:sourcesInSE){
						source2index.put(sourceId,indexConstraint);
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(sourceId),arc.getCapacity()));
						System.out.println("a("+indexConstraint+","+this.pi_edge2vertex2vertex2index.get(arc.getId()).get(destId).get(sourceId)+")="+((int)arc.getCapacity())+"");
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.phi_destination2vertex2edge2index.get(destId).get(arc.getFirstEndPoint().getId()).get(arc.getId()),-1d));
						System.out.println("a("+indexConstraint+","+this.phi_destination2vertex2edge2index.get(destId).get(arc.getFirstEndPoint().getId()).get(arc.getId())+")=-1");
						entriesInMatrixA.add(new Triplet<Integer,Double>(indexConstraint,this.f_demand2vertex2index.get(new Pair<Integer>(sourceId,destId)).get(arc.getFirstEndPoint().getId()),-1d));
						System.out.println("a("+indexConstraint+","+this.f_demand2vertex2index.get(new Pair<Integer>(sourceId,destId)).get(arc.getFirstEndPoint().getId())+")=-1");
						blc.add(0d);	
						System.out.println("blc(end+1)=0;");
						System.out.println("buc(end+1)=inf;");
						indexConstraint++;
					}
				}
			}
			System.out.print("opr = [");
			for(int i=0;i<opri.size();i++){
				System.out.print("'"+opr.get(i)+"'");
				if(i<opri.size()-1)
					System.out.print(",");
			}
			System.out.println("];");

			System.out.println("[res] = mskscopt(opr,opri,oprj,oprf,oprg,c,a,blc,buc,blx);");  
			System.out.println();

			System.out.println("% Print the solution. ");
			System.out.println("res.sol.itr.xx ");

		} catch (Exception e) {
			System.out.println("Error code: " + e.getMessage());
		}
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
	public Set<Integer> buildSE(Integer arcId, Graph ht, Set<Integer> sources){
		Set<Integer> selectedSources = new TreeSet<Integer>();
		Arc arc = ht.getArcById(arcId);
		if(arc==null)
			return null;
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
			for(Arc fromArc : from.getArcs()){
				if(!queued.contains(fromArc.getId())){
					queue.add(fromArc);
					queued.add(fromArc.getId());
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
}