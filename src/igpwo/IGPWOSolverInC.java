package igpwo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import model.Arc;
import model.Graph;
import model.Vertex;
import util.Pair;

public class IGPWOSolverInC implements IIGPWOSolver{

	public void computeBestECMPWeights(Graph g, List<Pair<Integer>> setOfDemands,  Map<Pair<Integer>,Double> demand2estimate) throws IOException{

		PrintWriter writer = new PrintWriter("input-topology-igpwo.dat", "UTF-8");
		writer.write(g.getVertices().size()+ " " + g.getArcs().size()+ " " + demand2estimate.keySet().size()+"\n");
		for(Arc arc : g.getArcs())
			writer.write(arc.getId()+ " " + arc.getFirstEndPoint().getId()+" "+arc.getSecondEndPoint().getId()+ " "+arc.getCapacity()+"\n");
		for(Pair<Integer> demand : demand2estimate.keySet())
			writer.write(demand.getSecond() + " " + demand.getFirst() + " " + demand2estimate.get(demand)+"\n");
		writer.flush();
		writer.close();

		Process p = Runtime.getRuntime().exec("/home/mosfet/Desktop/signomial/IGPWO/IGPWO-src/igpwo-v0-gen");

		String s;
		BufferedReader stdInput = new BufferedReader(new
				InputStreamReader(p.getInputStream()));

		//System.out.println(g);
		// read the output from the command
		int i=0;
		while ((s = stdInput.readLine()) != null) {
			//System.out.println(s);
			g.getArcById(i).setDistance(Double.parseDouble(s.split("=")[1]));
			i++;
		}



/*

		writer = new PrintWriter("topology-totem.xml", "UTF-8");

		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
		writer.write("         <domain ASID=\"11537\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
		writer.write("                 xsi:noNamespaceSchemaLocation=\"http://totem.run.montefiore.ulg.ac.be/Schema/Domain-v1_3.xsd\">\n");
		writer.write("             <info>\n");
		writer.write("                 <title>Abilene Topology</title>\n");
		writer.write("                 <date>2005-01-01</date>\n");
		writer.write("                 <author>\n");
		writer.write("                     Olivier Delcourt (delcourt@run.montefiore.ulg.ac.be) - RUN - Univers\n");
		writer.write("         ity of Liege\n");
		writer.write("                     Jean Lepropre (lepropre@run.montefiore.ulg.ac.be) - RUN - University\n");
		writer.write("          of Liege\n");
		writer.write("                 </author>\n");
		writer.write("                 <description>Topology of Abilene network - 21 feb 2005</description>\n");
		writer.write("                 <units>\n");
		writer.write("                     <unit type=\"bandwidth\" value=\"mbps\"/>\n");
		writer.write("                     <unit type=\"delay\" value=\"ms\"/>\n");
		writer.write("                 </units>\n");
		writer.write("                 <diff-serv>\n");
		writer.write("                     <priority ct=\"0\" id=\"0\" preemption=\"0\"/>\n");
		writer.write("                 </diff-serv>\n");
		writer.write("             </info>\n");
		writer.write("<topology>\n");
		writer.write("<nodes>\n");
		for(Vertex v : g.getVertices()){
			writer.write(" <node id=\""+v.getId()+"\">\n");
			writer.write("<interfaces>\n");
			writer.write("<interface id="lo">\n");
			writer.write("<ip mask="198.32.12.153/32">198.32.12.153</ip>\n");
			writer.write("</interface>\n");
			writer.write("<interface id="so-0/2/0"/>\n");
			writer.write("<interface id="so-3/1/0"/>\n");
			writer.write("</interfaces>\n");
			</node>

		}

		writer.flush();
		writer.close();*/
	}
}
