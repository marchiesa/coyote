package inputoutput;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import model.Arc;
import model.Graph;
import model.Vertex;

/**
 * Read a file containing a graph topology.
 * First row must contain the keyword @arcs
 * Row format: 
 *  vertex vertex capacity
 * 
 * @author mosfet
 */
public class RocketFuelGraphReader {

	public Graph readUndirectedGraph(String path){
		return this.readGraph(path, Graph.UNDIRECTED,true);
	}

	public Graph readDirectedGraph(String path){
		return this.readGraph(path, Graph.DIRECTED,false);
	}

	public Graph readGraph(String path,int type, boolean withDistances){
		BufferedReader br = null;
		List<String> rows = new ArrayList<String>();
		boolean isCreatedNow = false;
		Graph g = null;
		ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(
					new FileInputStream("solutions/"+path.split("/")[2]+"-graph"));
			g = (Graph) objectInputStream.readObject();
		} catch (IOException e) {System.out.println("File not found. Creating a new traffic matrix.");}
		catch (ClassNotFoundException e) {
			//e.printStackTrace();
			System.out.println("File not found. Creating a new graph file.");
		}

		if(g==null){
			isCreatedNow = true;
			try {

				String sCurrentLine;

				br = new BufferedReader(new FileReader(path));

				while ((sCurrentLine = br.readLine()) != null) {
					rows.add(sCurrentLine);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}

			g = new Graph();
			boolean startReading = false;
			int arcIdCounter=0;
			int cost =-1;
			int capacity=-1;
			boolean firstLine = true;
			for(String line: rows){
				if(firstLine){
					firstLine = false;
				}
				if(line.contains("@arcs")){
					startReading = true;
					continue;
				}
				if(line.contains("label") || !startReading){
					if(startReading){
						String[] splitCost = line.split("\t");
						for(int i=0;i<splitCost.length;i++){
							if(splitCost[i].equals("cost")){
								cost = i;
								//break;
							}
							if(splitCost[i].equals("capacity")){
								capacity = i;
								//break;
							}
						}
					}
					continue;	
				}


				String[] lineSplit = line.split("\t");
				Integer idVertex1 = Integer.parseInt(lineSplit[0]);
				Integer idVertex2 = Integer.parseInt(lineSplit[1]);
				//Double capacity = 1d*Integer.parseInt(lineSplit[4]);

				if(idVertex1>idVertex2)
					continue;

				Vertex v1 = g.getVertexById(idVertex1);
				Vertex v2 = g.getVertexById(idVertex2);
				if(v1==null){
					v1 = new Vertex();
					v1.setId(idVertex1);
					g.addVertex(v1);
				}
				if(v2==null){
					v2 = new Vertex();
					v2.setId(idVertex2);
					g.addVertex(v2);
				}

				Arc arc = new Arc();
				arc.setFirstEndPoint(v1);
				arc.setSecondEndPoint(v2);
				if(withDistances)
					arc.setDistance(((int)Double.parseDouble(lineSplit[cost])));
				if(capacity == -1)
					arc.setCapacity(1/arc.getDistance());
				else
					arc.setCapacity(Double.parseDouble(lineSplit[capacity]));
				arc.setId(arcIdCounter++);
				g.addDirectedArc(arc); 

				arc = new Arc();
				arc.setFirstEndPoint(v2);
				arc.setSecondEndPoint(v1);
				if(withDistances)
					arc.setDistance(((int)Double.parseDouble(lineSplit[cost])));
				if(capacity == -1)
					arc.setCapacity(1/arc.getDistance());
				else
					arc.setCapacity(Double.parseDouble(lineSplit[capacity]));
				arc.setId(arcIdCounter++);
				g.addDirectedArc(arc); 
				//System.out.println(v1.getId()+"--"+v2.getId());
			}
			new PruneGraph().pruneDegreeOne(g);
		}
		/*System.out.print("{\"vertices\":[");
		int counter =0;
		for(Vertex v : g.getVertices()){
			System.out.print("{\"id\":"+(v.getId()+1)+",\"value\":"+v.getId()+",\"x\":"+(int)(Math.random()*400)+",\"y\":"+(int)(Math.random()*400)+"}");
			if(counter++<(g.getVertices().size()-1))
				System.out.print(",");
		}
		System.out.print("],\"edges\":[");
		counter =0;
		for(Arc arc : g.getArcs()){
			if(arc.getId() % 2 == 0){
			System.out.print("{\"id\":"+((arc.getId()/2)+1)+",\"value\":\"\",\"from\":"+(arc.getFirstEndPoint().getId()+1)+",\"to\":"+(arc.getSecondEndPoint().getId()+1)+",\"directed\":false}");
			if(counter++<(g.getArcs().size()-1)/2)
				System.out.print(",");
			}
		}
		System.out.println("],\"x\":0,\"y\":0}");
		 */
		if(isCreatedNow){
			ObjectOutputStream objectOutputStream=null;
			try {
				objectOutputStream = new ObjectOutputStream(
						new FileOutputStream("solutions/"+path.split("/")[2]+"-graph"));
				objectOutputStream.writeObject(g);
			} catch (IOException e) {e.printStackTrace();}
			finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
			PrintWriter writer;
			try {
				writer = new PrintWriter("solutions/"+path.split("/")[2]+"-graph.txt", "UTF-8");
				writer.println("@nodes");
				writer.println("label"+"\t"+"node_id");
				for(Vertex v : g.getVertices()){
					writer.println(v.getId() + "\t" + v.getId());	
				}
				
				writer.println("@arcs");
				writer.println("\t"+"\t"+"label"+"\t"+"cost"+"\t"+"capacity");
				for(Arc arc: g.getArcs()){
					writer.println(arc.getFirstEndPoint().getId()+"\t"+arc.getSecondEndPoint().getId()+"\t"+arc.getId()+"\t"+arc.getDistance()+"\t"+arc.getCapacity());
				}
				//fourth type of constraint
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		return g;
	}

	public Graph readDirectedGraphWithDistances(String path) {
		return this.readGraph(path, Graph.DIRECTED,true);
	}



}
