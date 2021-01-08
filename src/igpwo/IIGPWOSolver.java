package igpwo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import model.Graph;
import util.Pair;

public interface IIGPWOSolver {
	
	public void computeBestECMPWeights(Graph g, List<Pair<Integer>> setOfDemands,  Map<Pair<Integer>,Double> demand2estimate) throws IOException;

}
