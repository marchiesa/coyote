package experiments.expanders;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lpsolver.LPSolverOptimalObliviousClosNetworkComparison;
import networks.random.RandomGraph;
import util.NDimensionalMap;
import util.Pair;
import experiments.MainOptimalOblivious;

public class MainRandomGraphsOptimalOblivious {

	public static void main(String[] args){

		int degree =4; 
		int layers = 3;
		int iterations =1;
		int numOfNodes = (int)(Math.pow(degree/2,layers-1))*(2*layers-1);
		int numOfServersClosNetwork = (int)(Math.pow(degree/2,layers-1))*degree;

		System.out.println(" degree=" + degree);
		System.out.println(" numOfNodes=" + numOfNodes);
		System.out.println(" numOfServersClosNetwork=" + numOfServersClosNetwork);

		RandomGraph randomGraph = new RandomGraph();
		Map<Integer,Integer> servers = null;
		/*ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(
					new FileInputStream("randomGraph.txt"));
			randomGraph= (RandomGraph) objectInputStream.readObject();

			objectInputStream = new ObjectInputStream(
					new FileInputStream("destination2ht.txt"));
			servers =(Map<Integer,Integer>) objectInputStream.readObject();  
		} catch (IOException e) {e.printStackTrace();}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}*/
		randomGraph.buildRandomNetwork(degree, numOfNodes);
		servers = randomGraph.selectServersInARandomNetwork(degree, numOfNodes, numOfServersClosNetwork);
		System.out.println(" servers=" + servers);
		/*ObjectOutputStream objectOutputStream=null;
		try {
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("randomGraph.txt"));
			objectOutputStream.writeObject(randomGraph);

			objectOutputStream=null;
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("servers.txt"));
			objectOutputStream.writeObject(servers);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}*/
		//NDimensionalMap<Integer> position2id = new NDimensionalMap<Integer>();
		//position2id.initialize(layers, degree/2);
		//mapServersFromClosToRandom(servers,position2id);

		//System.out.println(position2id);
		double max = Double.MIN_VALUE;
		for(int i=0; i<iterations;i++){
			//List<Pair<Integer>> setOfDemands = createPermutationTrafficMatrix(randomGraph,new HashMap<Integer,Integer>(servers));
			List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
			for(Integer server : servers.keySet()){
				for(Integer server2 : servers.keySet()){
					if(server>=server2)
						continue;
					setOfDemands.add(new Pair<Integer>(server,server2));
				}
			}

			LPSolverOptimalObliviousClosNetworkComparison lps = new LPSolverOptimalObliviousClosNetworkComparison();

			lps.computeOptimalPerformance(randomGraph, setOfDemands,servers);

			//System.out.println("size:" + lps.getArc2demand2nameAndValue().size());
			System.out.println("Optimal solution: " + MainOptimalOblivious.print(lps.getArc2demand2nameAndValue(),randomGraph));
			System.out.println("Oblivious performance: " + lps.getOpt());
			
			if(lps.getOpt()>max){
				max = lps.getOpt();
			}
			//System.out.println("Most congested arc: " + randomGraph.getArcById(lps.getMostCongestedIdArc()));
			System.out.println("max = " + max);
		}
		
		/*List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
		for(Integer server : servers.keySet()){
			for(Integer server2 : servers.keySet()){
				if(server>=server2)
					continue;
				setOfDemands.add(new Pair<Integer>(server,server2));
			}
		}

		LPSolverOptimalObliviousClosNetworkComparison lps = new LPSolverOptimalObliviousClosNetworkComparison();

		lps.computeOptimalPerformance(randomGraph, setOfDemands,servers);

		//System.out.println("Optimal solution: " + MainOptimalOblivious.print(lps.getArc2demand2nameAndValue(),randomGraph));
		System.out.println("Oblivious performance: " + lps.getOpt());
		
		
		//System.out.println("Most congested arc: " + randomGraph.getArcById(lps.getMostCongestedIdArc()));
		System.out.println("performance = " + lps.getOpt());*/

	}

	private static List<Pair<Integer>> createPermutationTrafficMatrix(
			RandomGraph randomGraph, Map<Integer, Integer> servers) {
		List<Pair<Integer>> demands = new LinkedList<Pair<Integer>>();
		List<Integer> vertices = new LinkedList<Integer>(servers.keySet());
		while(servers.size()>0){
			Integer firstVertex = vertices.get((int)(Math.random()*vertices.size()));
			if(servers.get(firstVertex)==1){
				servers.remove(firstVertex);
				vertices.remove(firstVertex);
			}else{
				servers.put(firstVertex,servers.get(firstVertex)-1);
			}
			Integer secondVertex = vertices.get((int)(Math.random()*vertices.size()));
			if(servers.get(secondVertex)==1){
				servers.remove(secondVertex);
				vertices.remove(secondVertex);
			}else{
				servers.put(secondVertex,servers.get(secondVertex)-1);
			}
			demands.add(new Pair<Integer>(firstVertex,secondVertex));
			//demands.add(new Pair<Integer>(secondVertex,firstVertex));
		}
		return demands;
	}

	private static void mapServersFromClosToRandom(
			Map<Integer, Integer> servers, NDimensionalMap<Integer> position2id) {
		int height = position2id.getHeight();
		mapServersFromClosToRandomRic(height,position2id.getDegree(),servers,position2id);
	}

	private static void mapServersFromClosToRandomRic(int height, int degree,Map<Integer, Integer> servers, NDimensionalMap<Integer> position2id) {
		if(height==0){
			Integer randomIdVertex = new LinkedList<Integer>(servers.keySet()).get((int)(Math.random()*servers.size()));
			position2id.setValue(randomIdVertex);
			servers.put(randomIdVertex, servers.get(randomIdVertex)-1);
			if(servers.get(randomIdVertex)==0)
				servers.remove(randomIdVertex);
		}
		else{
			int numberOfIndices = degree;
			if(position2id.isTop())
				numberOfIndices *= 2;
			for(int i=0;i<numberOfIndices;i++){
				mapServersFromClosToRandomRic(height-1, degree, servers, position2id.get(i));
			}
		}
	}
}
