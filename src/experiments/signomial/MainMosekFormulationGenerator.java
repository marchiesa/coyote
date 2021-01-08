package experiments.signomial;

import java.util.LinkedList;
import java.util.List;

import lpsolver.signomial.MosekFormulationGenerator;
import model.Graph;
import model.mock.Destination2HTMock;
import model.mock.GraphMock;
import util.Pair;

public class MainMosekFormulationGenerator {

	public static void main(String[] args) {
		Graph g = new GraphMock();
		Destination2HTMock d2ht = new Destination2HTMock();
		
		MosekFormulationGenerator mfg = new MosekFormulationGenerator();
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		setOfDemands.add(new Pair<Integer>(g.getVertexById(0).getId(),g.getVertexById(1).getId()));
		setOfDemands.add(new Pair<Integer>(g.getVertexById(1).getId(),g.getVertexById(0).getId()));
		setOfDemands.add(new Pair<Integer>(g.getVertexById(0).getId(),g.getVertexById(2).getId()));
		setOfDemands.add(new Pair<Integer>(g.getVertexById(2).getId(),g.getVertexById(0).getId()));
		setOfDemands.add(new Pair<Integer>(g.getVertexById(1).getId(),g.getVertexById(2).getId()));
		setOfDemands.add(new Pair<Integer>(g.getVertexById(2).getId(),g.getVertexById(1).getId()));
		mfg.computeOptimalPerformance(g, setOfDemands, d2ht.getDestination2ht());
	}
}
