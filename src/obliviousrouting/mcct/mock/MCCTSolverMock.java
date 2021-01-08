package obliviousrouting.mcct.mock;

import java.util.Map;
import java.util.Set;

import model.Arc;
import model.Graph;
import model.Vertex;
import obliviousrouting.mcct.MCCTSolver;
import obliviousrouting.mcct.Tree;

public class MCCTSolverMock implements MCCTSolver{

	private Tree tree;
	
	public void setTree(Tree t){
		this.tree = t;
	}
	
	@Override
	public void setGraph(Graph g) {
	}

	public Tree getTree() {
		return this.tree;
	}

	@Override
	public Tree getBestTree() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addDemand(Vertex v1, Vertex v2, Double demand) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<Integer, Map<Integer, Set<Arc>>> getTrees() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
