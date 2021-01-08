package experiments;

import inputoutput.ISetOfDemandsReader;
import inputoutput.SetOfDemandsReaderMockMotivation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import model.Graph;
import model.mock.GraphMockMotivation;
import model.mock.GraphMockMotivationDag2;
import util.Pair;
import daggenerator.ShortestPathDagGenerator;

public class MainMockMotivaion {

	public static void main(String[] args) throws FileNotFoundException{
		
		@SuppressWarnings("unused")
		//Graph g1=  new RocketFuelGraphReader().readDirectedGraphWithDistances("topologies/Backbone/mock-dag-RL.lgf");
		
		
		Graph g= new GraphMockMotivation();
		ISetOfDemandsReader sodr = new SetOfDemandsReaderMockMotivation();
		List<Pair<Integer>> setOfDemands = sodr.getSetOfDemands(g, null);			

		ObjectOutputStream objectOutputStream=null;
		try {
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("solutions/mock.lgf-graph"));
			objectOutputStream.writeObject(g);
		} catch (IOException e) {e.printStackTrace();}
		finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
		
		Map <Integer, Graph> destination2ht = new ShortestPathDagGenerator().computeDag(g,setOfDemands);

		objectOutputStream=null;
		try {
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("solutions/mock.lgf-destination2ht-augment.txt "));
			objectOutputStream.writeObject(destination2ht);
		} catch (IOException e) {e.printStackTrace();}
		finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
/*
		g= new GraphMockMotivationDag2();
		sodr = new SetOfDemandsReaderMockMotivation();
		setOfDemands = sodr.getSetOfDemands(g, null);			

		objectOutputStream=null;
		try {
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("solutions/mock-dag-LR.lgf-graph"));
			objectOutputStream.writeObject(g);
		} catch (IOException e) {e.printStackTrace();}
		finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
		
		
		destination2ht = new ShortestPathDagGenerator().computeDag(g,setOfDemands);

		objectOutputStream=null;
		try {
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("solutions/mock-dag-LR.lgf-destination2ht-augment.txt "));
			objectOutputStream.writeObject(destination2ht);
		} catch (IOException e) {e.printStackTrace();}
		finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
*/
	}
}
