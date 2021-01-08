package experiments.expanders;

import inputoutput.AsafTopologiesReader;

import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lpsolver.LPSolverOptimalObliviousClosNetworkComparison;
import model.Vertex;
import networks.random.RandomGraph;
import util.NDimensionalMap;
import util.Pair;
import experiments.MainOptimalOblivious;

public class MainXpanderComparison {

	public static void main(String[] args){

		RandomGraph randomGraph = null;
		Map<Integer,Integer> servers = null;

		double min = Double.MAX_VALUE;
		for(int j=0;j<1000;j++){
			ObjectInputStream objectInputStream = null;
/*			try {
				objectInputStream = new ObjectInputStream(
						new FileInputStream("randomGraphAsaf.txt"));
				randomGraph= (RandomGraph) objectInputStream.readObject();

//				objectInputStream = new ObjectInputStream(
//						new FileInputStream("serversAsaf.txt"));
//				servers =(Map<Integer,Integer>) objectInputStream.readObject();  
			} catch (IOException e) {e.printStackTrace();}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}*/
			//if(randomGraph==null){
				randomGraph = (RandomGraph)new AsafTopologiesReader().readDirectedGraphWithDistances(args[0]);
				
		//	}
				//for(Arc arc : randomGraph.getArcs())
				//	System.out.println(arc.getFirstEndPoint().getId()+ " -> " + arc.getSecondEndPoint().getId()+";");
				//if(1==1)return;
			int degree = randomGraph.getVertexById(0).getArcs().size();
			int numOfNodes = randomGraph.getVertices().size();
			int layers = 3;
			int numOfServersClosNetwork = (int)(Math.pow(degree/2,layers-1))*degree;
			//servers = randomGraph.selectServersInARandomNetwork(degree, numOfNodes, numOfServersClosNetwork);
			servers = new HashMap<Integer,Integer>();
			for(Vertex v : randomGraph.getVertices())
				servers.put(v.getId(), 1);
			System.out.println(" servers=" + servers);
			/*ObjectOutputStream objectOutputStream=null;
		try {
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("randomGraphAsaf.txt"));
			objectOutputStream.writeObject(randomGraph);

			objectOutputStream=null;
			objectOutputStream = new ObjectOutputStream(
					new FileOutputStream("serversAsaf.txt"));
			objectOutputStream.writeObject(servers);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}*/
			//NDimensionalMap<Integer> position2id = new NDimensionalMap<Integer>();
			//position2id.initialize(layers, degree/2);
			//mapServersFromClosToRandom(servers,position2id);

			System.out.println(randomGraph);
			double max = Double.MIN_VALUE;

			//DEACTIVATED IN THE FOR LOOP
			for(int i=0; i<0;i++){
				//List<Pair<Integer>> setOfDemands = createPermutationTrafficMatrix(randomGraph,new HashMap<Integer,Integer>(servers));
				//List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
				//setOfDemands.add(new Pair<Integer>(0,1));
				//setOfDemands.add(new Pair<Integer>(2,3));
				List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();
				for(Integer server : servers.keySet()){
					for(Integer server2 : servers.keySet()){
						if(server>=server2)
							continue;
						setOfDemands.add(new Pair<Integer>(server,server2));
					}
				}
				System.out.println("demands:"+setOfDemands);

				LPSolverOptimalObliviousClosNetworkComparison lps = new LPSolverOptimalObliviousClosNetworkComparison();

				lps.computeOptimalPerformance(randomGraph, setOfDemands,servers);

				//System.out.println("Optimal solution: " + MainOptimalOblivious.print(lps.getArc2demand2nameAndValue(),randomGraph));
				System.out.println("Oblivious performance: " + lps.getOpt());

				if(lps.getOpt()>max){
					max = lps.getOpt();
				}
				//System.out.println("Most congested arc: " + randomGraph.getArcById(lps.getMostCongestedIdArc()));
				System.out.println("max = " + max);
			}

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

			System.out.println("Optimal solution: " + MainOptimalOblivious.print(lps.getArc2demand2nameAndValue(),randomGraph));
			System.out.println("Oblivious performance: " + lps.getOpt());
			if(min>lps.getOpt())
				min=lps.getOpt();


			//System.out.println("Most congested arc: " + randomGraph.getArcById(lps.getMostCongestedIdArc()));
			System.out.println("performance = " + lps.getOpt());
			System.out.println(j+": performance-min = " + min);
		}
		System.out.println("performance = " + min);
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
