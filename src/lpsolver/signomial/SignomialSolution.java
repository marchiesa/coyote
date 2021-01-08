package lpsolver.signomial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class SignomialSolution {

	private Map<Integer,Map<Integer,Map<Integer,Double>>> pi_edge2vertex2vertex2index;
	private Map<Integer,Map<Integer,Double>>  w_edge2edge2index;
	private Map<Pair<Integer>,Map<Integer,Double>> f_demand2vertex2index;
	private Map<Pair<Integer>,Map<Integer,Double>> y_demand2vertex2index;
	private Map<Pair<Integer>,Map<Integer,Double>> g_demand2edge2index;
	private Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value;

	public SignomialSolution(){
		this.pi_edge2vertex2vertex2index = new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();
		this.w_edge2edge2index = new HashMap<Integer,Map<Integer,Double>>();
		this.f_demand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Double>>();
		this.y_demand2vertex2index = new HashMap<Pair<Integer>,Map<Integer,Double>>();
		this.g_demand2edge2index = new HashMap<Pair<Integer>,Map<Integer,Double>>();
		this.phi_destination2vertex2edge2value = new HashMap<Integer,Map<Integer,Map<Integer,Double>>>();
	}

	public Map<Integer, Map<Integer, Map<Integer, Double>>> getPi_edge2vertex2vertex2index() {
		return pi_edge2vertex2vertex2index;
	}
	public void setPi_edge2vertex2vertex2index(
			Map<Integer, Map<Integer, Map<Integer, Double>>> pi_edge2vertex2vertex2index) {
		this.pi_edge2vertex2vertex2index = pi_edge2vertex2vertex2index;
	}
	public Map<Integer, Map<Integer, Double>> getW_edge2edge2index() {
		return w_edge2edge2index;
	}
	public void setW_edge2edge2index(
			Map<Integer, Map<Integer, Double>> w_edge2edge2index) {
		this.w_edge2edge2index = w_edge2edge2index;
	}
	public Map<Pair<Integer>, Map<Integer, Double>> getF_demand2vertex2index() {
		return f_demand2vertex2index;
	}
	public void setF_demand2vertex2index(
			Map<Pair<Integer>, Map<Integer, Double>> f_demand2vertex2index) {
		this.f_demand2vertex2index = f_demand2vertex2index;
	}
	public Map<Pair<Integer>, Map<Integer, Double>> getY_demand2vertex2index() {
		return y_demand2vertex2index;
	}
	public void setY_demand2vertex2index(
			Map<Pair<Integer>, Map<Integer, Double>> y_demand2vertex2index) {
		this.y_demand2vertex2index = y_demand2vertex2index;
	}
	public Map<Pair<Integer>, Map<Integer, Double>> getG_demand2edge2index() {
		return g_demand2edge2index;
	}
	public void setG_demand2edge2index(
			Map<Pair<Integer>, Map<Integer, Double>> g_demand2edge2index) {
		this.g_demand2edge2index = g_demand2edge2index;
	}
	public Map<Integer, Map<Integer, Map<Integer, Double>>> getPhi_destination2edge2value() {
		return phi_destination2vertex2edge2value;
	}
	public void setPhi_destination2edge2value(
			Map<Integer, Map<Integer, Map<Integer, Double>>> phi_destination2edge2value) {
		this.phi_destination2vertex2edge2value = phi_destination2edge2value;
	}
	public double getK(Integer destId,int vertexId) {
		double sum = 0;
		for(Double value : this.phi_destination2vertex2edge2value.get(destId).get(vertexId).values()){
			sum += value;
		}
		Map<Integer,Double> edge2value = getA(destId,vertexId);
		double prod=1;
		for(Integer arcId: this.phi_destination2vertex2edge2value.get(destId).get(vertexId).keySet()){
			prod *= Math.pow(this.phi_destination2vertex2edge2value.get(destId).get(vertexId).get(arcId), edge2value.get(arcId));
		}
		return sum/prod;

	}
	public Map<Integer,Double> getA(Integer destId, int vertexId) {
		double sum = 0;
		Map<Integer,Double> edge2value = new HashMap<Integer,Double>();
		for(Double value : this.phi_destination2vertex2edge2value.get(destId).get(vertexId).values()){
			sum += value;
		}
		for(Integer arcId : this.phi_destination2vertex2edge2value.get(destId).get(vertexId).keySet()){
			edge2value.put(arcId, this.phi_destination2vertex2edge2value.get(destId).get(vertexId).get(arcId)/sum);
		}
		return edge2value;
	}
	public void initialize(Graph g, List<Pair<Integer>> setOfDemands, Set<Integer> destinationVertices, Map<Integer,Graph> destination2ht) {
		for(Integer destId : destinationVertices){
			Map<Integer, Map<Integer,Double>> vertex2edge2value = new HashMap<Integer, Map<Integer,Double>>();
			this.phi_destination2vertex2edge2value.put(destId, vertex2edge2value);
			for(Vertex v: destination2ht.get(destId).getVertices()){
				Map<Integer,Double> edge2value = new HashMap<Integer,Double>();
				vertex2edge2value.put(v.getId(), edge2value);
				int numberOfOutgoingArcs = v.getArcs().size();
				for(Arc arc : v.getArcs()){
					edge2value.put(arc.getId(), 1d/numberOfOutgoingArcs);
				}
			}
		}
	}



}
