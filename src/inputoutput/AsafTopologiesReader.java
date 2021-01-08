package inputoutput;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import model.Arc;
import model.Graph;
import model.Vertex;
import networks.random.RandomGraph;

public class AsafTopologiesReader {

	public Graph readGraph(String path,int type, boolean withDistances){
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

		RandomGraph g = new RandomGraph();
		int arcIdCounter=0;
		int counter = 0;
		for(String line: rows){

			String[] lineSplit = line.split(",");
			Integer idVertex1 = counter++;
			for(int j=0 ; j<lineSplit.length ; j++){
				if(lineSplit[j].equals("1")){
					Integer idVertex2 = j;
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

					if(v1.getId()>v2.getId())
						continue;

					Arc arc = new Arc();
					arc.setFirstEndPoint(v1);
					arc.setSecondEndPoint(v2);
					arc.setDistance(1d);
					arc.setCapacity(1d/arc.getDistance());
					arc.setId(arcIdCounter++);
					g.addDirectedArc(arc); 
					Arc arc2 = new Arc();
					arc2.setFirstEndPoint(v2);
					arc2.setSecondEndPoint(v1);
					arc2.setDistance(1d);
					arc2.setCapacity(1d/arc.getDistance());
					arc2.setId(arcIdCounter++);
					g.addDirectedArc(arc2); 

				}
			}
		}
		return g;
	}

	public Graph readDirectedGraphWithDistances(String path) {
		return this.readGraph(path, Graph.DIRECTED,true);
	}


}
