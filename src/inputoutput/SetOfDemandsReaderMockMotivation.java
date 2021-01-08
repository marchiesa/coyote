package inputoutput;

import java.util.LinkedList;
import java.util.List;

import model.Graph;
import util.Pair;

public class SetOfDemandsReaderMockMotivation implements ISetOfDemandsReader{
	
	public List<Pair<Integer>> getSetOfDemands(Graph g, String file){
		
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		
		setOfDemands.add(new Pair<Integer>(0,3));
		setOfDemands.add(new Pair<Integer>(1,3));
		
		return setOfDemands;
		
	}

}
