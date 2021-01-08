package inputoutput;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import model.Arc;
import model.Graph;
import model.Vertex;

/**
 * Simple graph format consists of one row per edge.
 * vertex vertex capacity
 * @author mosfet
 *
 */
public class SimpleToyGraphReader {

	public Graph readUndirectedGraph(String path){
		return this.readGraph(path, Graph.UNDIRECTED);
	}

	public Graph readGraph(String path,int type){
		BufferedReader br = null;
		List<String> rows = new ArrayList<String>();
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

		Graph g = new Graph();
		int i=0;
		for(String line: rows){
			String[] lineSplit = line.split(" ");
			Integer idVertex1 = Integer.parseInt(lineSplit[0]);
			Integer idVertex2 = Integer.parseInt(lineSplit[1]);
			Integer capacity = Integer.parseInt(lineSplit[2]);

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
			arc.setCapacity(capacity);
			arc.setId(2*i);
			g.addDirectedArc(arc); 
			if(type==Graph.UNDIRECTED){
				Arc reversedArc = new Arc();
				reversedArc.setFirstEndPoint(v2);
				reversedArc.setSecondEndPoint(v1);
				reversedArc.setCapacity(capacity);
				reversedArc.setId(2*i+1);
				g.addDirectedArc(reversedArc);
			}
			i++;
		}
		return g;
	}



}
