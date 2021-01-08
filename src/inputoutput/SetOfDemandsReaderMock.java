package inputoutput;

import java.util.LinkedList;
import java.util.List;

import model.Graph;
import model.Vertex;
import util.Pair;

public class SetOfDemandsReaderMock implements ISetOfDemandsReader{
	
	public List<Pair<Integer>> getSetOfDemands(Graph g, String file){
		
		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		
		for(Vertex from: g.getVertices())
			for(Vertex to : g.getVertices())
				if(from.getId()!=to.getId())
					setOfDemands.add(new Pair<Integer>(from.getId(),to.getId()));
		
		
		return setOfDemands;
		
	}

}
