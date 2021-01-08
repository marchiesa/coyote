package inputoutput;

import java.util.List;

import model.Graph;
import util.Pair;

public interface ISetOfDemandsReader {
	
	public List<Pair<Integer>> getSetOfDemands(Graph g, String file);

}
