package inputoutput;

import java.util.LinkedList;
import java.util.List;

import model.Graph;
import util.Pair;

public class SetOfDemandsReaderMockForSlides implements ISetOfDemandsReader{
	
	public List<Pair<Integer>> getSetOfDemands(Graph g, String file){
		
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		
		setOfDemands.add(new Pair<Integer>(1,4));
		setOfDemands.add(new Pair<Integer>(2,4));
		setOfDemands.add(new Pair<Integer>(3,4));
		
		return setOfDemands;
		
	}

}
