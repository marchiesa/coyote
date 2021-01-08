package networks.random;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.Arc;
import model.Graph;
import model.Vertex;

public class RandomGraph extends Graph{

	private static final long serialVersionUID = 1L;

	public void buildRandomNetwork(int degree,int numberOfVertices){
		buildRandomNetworkRic(degree,numberOfVertices);
	}
	
	private void buildRandomNetworkRic(int degree, int numberOfVertices){
		if(numberOfVertices == 1){
			Vertex router1 = new Vertex();
			router1.setId(0);
			this.addVertex(router1);
			//Router router2 = new Router(1);
			//network.addRouter(router2);
			//add degree/2 edges
			for (int i=0; i<degree/2; i++){
				Arc port1 = new Arc();
				port1.setId(2*i);
				port1.setFirstEndPoint(router1);
				port1.setSecondEndPoint(router1);
				port1.setDistance(1);
				port1.setCapacity(1);
				Arc port2 = new Arc();
				port2.setId(2*i+1);
				port2.setFirstEndPoint(router1);
				port2.setSecondEndPoint(router1);
				port2.setDistance(1);
				port2.setCapacity(1);
				this.addDirectedArc(port1);
				this.addDirectedArc(port2);
			}
		}else{
			buildRandomNetworkRic(degree,numberOfVertices-1);
			addNewVertex(degree,numberOfVertices);
		}
	}

	private void addNewVertex(int degree,int numberOfVertices) {
		Vertex router = new Vertex();
		router.setId(numberOfVertices-1);
		this.addVertex(router);

		//pick k/2 edges at random
		List<Integer> ports = new LinkedList<Integer>();
		//Map<Integer,Integer> idRouter2numberOfPortsSelected = new HashMap<Integer,Integer>();
		while(ports.size() <degree/2){
			Arc randomPort = null;
			while(randomPort == null || ports.contains(2*(randomPort.getId()/2)) || ports.contains(2*(randomPort.getId()/2)+1) ){
				Integer id = null;
				//if(numberOfVertices == 2)
					id = (int)(Math.random()*((numberOfVertices-1)*degree));
				//else
				//	id = (int)(Math.random()*((numberOfVertices-1)*degree));
				
				randomPort = this.getArcByPosition(id);
			}
			ports.add(randomPort.getId());
		}

		//add k edges to the new router
		int i=(numberOfVertices-1)*degree*2;
		for(Integer portId: ports){
			Arc port = this.getArcById(portId);
			Arc port1 = new Arc();
			port1.setId(i++);
			port1.setFirstEndPoint(port.getFirstEndPoint());
			port1.setSecondEndPoint(router);
			port1.setCapacity(1);
			port1.setDistance(1);
			Arc port2 = new Arc();
			port2.setId(i++);
			port2.setFirstEndPoint(router);
			port2.setSecondEndPoint(port.getFirstEndPoint());
			port2.setCapacity(1);
			port2.setDistance(1);
			this.removeArcById(2*(port.getId()/2)  );
			this.removeArcById(2*(port.getId()/2) + 1 );
			this.addDirectedArc(port1);
			this.addDirectedArc(port2);
			port1 = new Arc();
			port1.setId(i++);
			port1.setFirstEndPoint(port.getSecondEndPoint());
			port1.setSecondEndPoint(router);
			port1.setCapacity(1);
			port1.setDistance(1);
			port2 = new Arc();
			port2.setId(i++);
			port2.setFirstEndPoint(router);
			port2.setSecondEndPoint(port.getSecondEndPoint());
			port2.setCapacity(1);
			port2.setDistance(1);
			this.addDirectedArc(port1);
			this.addDirectedArc(port2);
		}

	}

	public Map<Integer,Integer> selectServersInARandomNetwork(int degree,int numOfNodes, int numOfServersClosNetwork) {
		Map<Integer,Integer> servers = new HashMap<Integer,Integer>();
		int counter =0;
		while(counter < numOfServersClosNetwork){
			Arc randomArc = null;
			while(randomArc==null || (servers.get(randomArc.getFirstEndPoint().getId()) != null && 
					                  servers.get(randomArc.getFirstEndPoint().getId()) >= Math.ceil(degree/4d)) || 
					                 (servers.get(randomArc.getSecondEndPoint().getId()) != null && 
					                 servers.get(randomArc.getSecondEndPoint().getId()) >= Math.ceil(degree/4d))){
				randomArc = this.getRandomArc();
			}
			Integer firstServer = servers.get(randomArc.getFirstEndPoint().getId());
			Integer secondServer = servers.get(randomArc.getSecondEndPoint().getId());
			if(firstServer==null)
				firstServer=0;
			if(secondServer==null)
				secondServer=0;
			servers.put(randomArc.getFirstEndPoint().getId(),firstServer+1);
			servers.put(randomArc.getSecondEndPoint().getId(),secondServer+1);
			//this.removeArcById(2*(randomArc.getId()/2));
			//this.removeArcById(2*(randomArc.getId()/2)+1);
			counter+=2;
		}
		return servers;
	}



}
