package experiments.signomial;

import igpwo.IGPWOSolverInC;
import igpwo.IIGPWOSolver;
import inputoutput.DAGSolutionFromFileReader;
import inputoutput.ISetOfDemandsReader;
import inputoutput.RocketFuelGraphReader;
import inputoutput.SetOfDemandsReaderMock;
import inputoutput.SetOfDemandsReaderMockForSlides;
import inputoutput.SetOfDemandsReaderMockMotivation;
import inputoutput.SplittingRatioWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lpsolver.CompactLPSolverObliviousPerformanceGivenDemand;
import lpsolver.CompactLPSolverObliviousPerformanceGivenDemandDAG;
import lpsolver.MCFSolver;
import lpsolver.MCFSolverDAG;
import lpsolver.signomial.IMosekFormulationGeneratorAmpl;
import lpsolver.signomial.MosekFormulationGeneratorAmpl;
import lpsolver.signomial.MosekFormulationGeneratorAmplCheckSolution;
import lpsolver.signomial.MosekFormulationGeneratorAmplCompact;
import lpsolver.signomial.MosekFormulationGeneratorAmplGivenDemand;
import lpsolver.signomial.MosekFormulationGeneratorAmplHose;
import lpsolver.signomial.MosekFormulationGeneratorAmplUpdate;
import lpsolver.signomial.MosekFormulationReadSolution;
import lpsolver.signomial.SolutionNormalizer;
import model.Arc;
import model.Graph;
import model.Vertex;
import model.mock.Destination2HTMock;
import model.mock.GraphMock;
import networks.random.RandomGraph;
import performance.calculator.APerformanceTrafficMatrix;
import performance.calculator.PerformanceTrafficMatrixFortzThorupSquared;
import performance.calculator.PerformanceTrafficMatrixMostCongestedEdge;
import performance.trafficmatrix.BimodalModelCalculator;
import performance.trafficmatrix.ConstantModelCalculator;
import performance.trafficmatrix.GaussianModelCalculator;
import performance.trafficmatrix.GravityModelCalculator;
import performance.trafficmatrix.IModelCalculator;
import performance.trafficmatrix.UniformModelCalculator;
import util.Pair;
import daggenerator.AugmentedShortestPathDagGenerator;
import daggenerator.IDagGenerator;
import daggenerator.ShortestPathDagGenerator;
import experiments.racke.MainRacke;

public class MainMosekFormulationGeneratorAmpl {

	private static boolean AUGMENT_PATHS=false ;
	private static boolean WEIGHT_1=false ;
	private static boolean ECMP_SOLUTION=false;
	private static boolean RACKE_SOLUTION=false;
	private static boolean OPTIMAL_SOLUTION=false;
	private static boolean OPTIMAL_SOLUTION_UNCONSTRAINED=false;
	private static boolean MARGIN=false;
	private static boolean AGGREGATE_CONGESTION=false;
	private static boolean SPECIFIC_DEMAND=false;

	public static void main(String[] args) throws IOException {

		Graph g = new GraphMock();
		String graphNameFile=null;
		if(args[1].contains(".lgf"))
			graphNameFile = args[1].split("/")[2];
		else
			graphNameFile = args[0].split("/")[2];
		Destination2HTMock d2ht = new Destination2HTMock();
		Map<Integer,Graph> destination2ht = null;
		String destinationFile = graphNameFile+"-destination2ht.txt";
		String destinationTrafficFile = "gravity";
		IModelCalculator trafficDemandCalculator=new GravityModelCalculator();
		APerformanceTrafficMatrix perf = new PerformanceTrafficMatrixMostCongestedEdge();
		String perfString= "maxcong";
		IDagGenerator dagGenerator = new ShortestPathDagGenerator();
		String algorithm = "signomial";
		Long seed = null;


		for(String arg: args){
			if(arg.equals("--augment")){
				AUGMENT_PATHS=true;
				destinationFile = graphNameFile+"-destination2ht-augment.txt";
				dagGenerator = new AugmentedShortestPathDagGenerator();
			}
			else if(arg.equals("--weight-1")){
				WEIGHT_1=true;
			}
			else if(arg.contains("--seed")){
				seed = Long.parseLong(arg.replaceAll("--seed", ""));
			}
			else if(arg.equals("--bimodal")){
				trafficDemandCalculator = new BimodalModelCalculator();
				destinationTrafficFile = "bimodal";
			}
			else if(arg.equals("--gaussian")){
				trafficDemandCalculator = new GaussianModelCalculator();
				destinationTrafficFile = "gaussian";
			}
			else if(arg.equals("--uniform")){
				trafficDemandCalculator = new UniformModelCalculator();
				destinationTrafficFile = "uniform";
			}
			else if(arg.equals("--constant")){
				trafficDemandCalculator = new ConstantModelCalculator();
				destinationTrafficFile = "constant";
			}
			else if(arg.equals("--fortz")){
				perf = new PerformanceTrafficMatrixFortzThorupSquared();
				perfString= "fortz";
			}
			else if(arg.equals("--solutionecmp")){
				ECMP_SOLUTION=true;
				algorithm = "ecmp";
			}
			else if(arg.equals("--solutionracke")){
				RACKE_SOLUTION=true;
				algorithm = "racke";
			}
			else if(arg.equals("--margin")){
				MARGIN=true;
				algorithm = "margin";
			}else if(arg.equals("--aggregate")){
				AGGREGATE_CONGESTION=true;
			}else if(arg.equals("--optimal-dag")){
				OPTIMAL_SOLUTION=true;
				algorithm = "optimal-dag";
			}
			else if(arg.equals("--optimal-dag-unconstrained")){
				OPTIMAL_SOLUTION_UNCONSTRAINED=true;
				algorithm = "optimal-dag-unconstrained";
			}


		}

		String graphPath = args[0];

		List<Pair<Integer>> setOfDemands = new LinkedList<Pair<Integer>>();

		if(graphPath.equals("-update")){ // signomial approximation update step
			MosekFormulationGeneratorAmplUpdate main = new MosekFormulationGeneratorAmplUpdate();
			main.updateFormulation(graphNameFile);
		}
		else if(graphPath.equals("-readsolution")){ // signomial approximation update step
			MosekFormulationReadSolution main = new MosekFormulationReadSolution();
			main.readSolution(graphNameFile);
		}
		else if(graphPath.equals("-testmargin")){ // signomial approximation update step
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			g= rfgr.readDirectedGraphWithDistances(args[1]);
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);	


			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;
			DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
			if(ECMP_SOLUTION){
				Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
				arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);
			}else if(OPTIMAL_SOLUTION){
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG",setOfDemands);
			}else if(OPTIMAL_SOLUTION_UNCONSTRAINED){
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG-unconstrained",setOfDemands);
			}else if(!MARGIN)
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands);
			File fileAvg = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-avg.txt");
			PrintWriter writerAvg =new PrintWriter(fileAvg);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
			File fileMax = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-max.txt");
			PrintWriter writerMax =new PrintWriter(fileMax);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
			DecimalFormat df = new DecimalFormat("#.00"); 
			for(double w=10;w<=50;w+=5){
				double sum = 0;
				double max = Double.MIN_VALUE;
				double wReal = w/10;
				if(!ECMP_SOLUTION && MARGIN){
					arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands,wReal);
				}
				for(int i =1; i<=Integer.parseInt(args[2]); i++){
					if(i % 10 == 0)System.out.println("margin: + " + wReal + " iteration: " + i);
					Map<Pair<Integer>,Double> demand2flow = null;
					ObjectInputStream objectInputStream = null;
					try {
						objectInputStream = new ObjectInputStream(
								new FileInputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+i+"-"+df.format(wReal)));
						demand2flow = (Map<Pair<Integer>,Double>) objectInputStream.readObject();
					} catch (IOException e) {if(i % 10 == 0)System.out.println("margin: + " + wReal + " iteration: " + i);System.out.println("File not found. Creating a new traffic matrix.");}
					catch (ClassNotFoundException e) {
						//e.printStackTrace();
						if(i % 10 == 0)System.out.println("File not found. Creating a new traffic matrix.");
					}
					if(demand2flow==null){
						demand2flow = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands,wReal);
						ObjectOutputStream objectOutputStream=null;
						try {
							/*File fileTM = new File("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+i+"-"+df.format(wReal));
							PrintWriter writerTM =new PrintWriter(fileTM);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
							for(Pair<Integer> demand : demand2flow.keySet())
								writerTM.println(demand.getFirst()+" "+demand.getSecond()+ " "+demand2flow.get(demand));
							writerTM.flush();writerTM.close();*/
							objectOutputStream = new ObjectOutputStream(
									new FileOutputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+i+"-"+df.format(wReal)));
							objectOutputStream.writeObject(demand2flow);
						} catch (IOException e) {e.printStackTrace();}
						finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
					}
					perf.computePerformanceSpecificDemand(g, setOfDemands, arc2demand2fraction, demand2flow, AGGREGATE_CONGESTION);
					double performance = perf.getOpt();
					sum+=performance;
					if(max < performance)
						max = performance;
				}
				writerAvg.println(wReal+" " +sum/Integer.parseInt(args[2]));
				writerMax.println(wReal+" "  + max);
			}
			writerAvg.flush();
			writerAvg.close();
			writerMax.flush();
			writerMax.close();
		}
		else if(graphPath.equals("-randomsolution")){
			MosekFormulationGeneratorAmplRandomSolution main = new MosekFormulationGeneratorAmplRandomSolution();
			destination2ht = readDestination2HtFromFile(destinationFile);
			if(destination2ht==null){
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
			}
			main.writeRandomSolution(destination2ht);
		}
		else if(graphPath.equals("-checkdagsize")){
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			g= rfgr.readDirectedGraphWithDistances(args[1]);
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);			

			if(WEIGHT_1)
				for(Arc arc:g.getArcs())
					arc.setDistance(1d);

			dagGenerator = new ShortestPathDagGenerator();
			destinationFile = graphNameFile+"-destination2ht.txt";
			destination2ht = readDestination2HtFromFile(destinationFile);
			if(destination2ht==null){
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
			}

			double capacitiesShortestPaths = computeSumOfArc(destination2ht);

			dagGenerator = new AugmentedShortestPathDagGenerator();
			destinationFile = graphNameFile+"-destination2ht-augment.txt";
			if(destination2ht==null){
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
			}
			destination2ht = readDestination2HtFromFile(destinationFile);

			double capacitiesAugmentedShortestPaths = computeSumOfArc(destination2ht);


			File file2 = new File("results/paths-in-dags-"+graphNameFile.split("\\.")[0]+".txt");
			PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
			writer.println(capacitiesShortestPaths + " " + capacitiesAugmentedShortestPaths);
			System.out.println(capacitiesShortestPaths + " " + capacitiesAugmentedShortestPaths);
			writer.flush();
			writer.close();
		}
		else if(graphPath.equals("-check")){
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			g= rfgr.readDirectedGraphWithDistances(args[1]);
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);
			//sodr = new SetOfDemandsReaderMockMotivation();
			//setOfDemands = sodr.getSetOfDemands(g, null);	

			if(WEIGHT_1)
				for(Arc arc:g.getArcs())
					arc.setDistance(1d);

			destination2ht = readDestination2HtFromFile(destinationFile);
			if(destination2ht==null){
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
			}

			MosekFormulationGeneratorAmplCheckSolution main = new MosekFormulationGeneratorAmplCheckSolution();
			DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;
			if(ECMP_SOLUTION){
				Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
				arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);
			}
			else if(OPTIMAL_SOLUTION)
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g,"solutions/"+graphNameFile+"-optimalDAG",setOfDemands);
			else if(OPTIMAL_SOLUTION_UNCONSTRAINED)
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG-unconstrained",setOfDemands);
			else
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands);

			main.checkDAGSolution(args[1],g,destination2ht,arc2demand2fraction, algorithm,AGGREGATE_CONGESTION);
			//System.out.println(destination2ht);
		}
		else if(graphPath.equals("-checkdemand")){
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			g= rfgr.readDirectedGraphWithDistances(args[1]);
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);	

			if(WEIGHT_1)
				for(Arc arc:g.getArcs())
					arc.setDistance(1d);

			destination2ht = readDestination2HtFromFile(destinationFile);
			if(destination2ht==null){
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
			}
			DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;
			if(ECMP_SOLUTION){
				Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
				arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);				
			}
			else
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands);
			Map<Pair<Integer>,Double> trafficMatrix =  trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
			System.out.println("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+".txt");
			File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+".txt");
			PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
			perf.computePerformanceSpecificDemand(g, setOfDemands, arc2demand2fraction, trafficMatrix, AGGREGATE_CONGESTION);
			//lpSolver.computeObliviousPerformance(g, solution);
			writer.println(perf.getOpt());
			writer.flush();
			writer.close();

			//System.out.print("Oblivious Solution: " + MainRacke.print(solution,g,lpSolver.getDemand2nameAndValue()));
			System.out.println("performance-gravity: " + perf.getOpt());
			if(perf.getMostCongestedIdArc()!=null)System.out.println("Most congested arc: " + g.getArcById(perf.getMostCongestedIdArc()));
			//System.out.println("Demands: " + MainRacke.printDemands(lpSolver.getDemand2nameAndValue()));
			//System.out.println("Lambda: " +lpSolver.getLambda());
			//System.out.println("Optimal solution for these demands: " +MainRacke.printFlow(lpSolver.getArc2idVertex2nameAndValue()));

		}
		else if(graphPath.equals("-checkdemandmargin")){
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			g= rfgr.readDirectedGraphWithDistances(args[1]);
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);	

			if(WEIGHT_1)
				for(Arc arc:g.getArcs())
					arc.setDistance(1d);

			destination2ht = readDestination2HtFromFile(destinationFile);
			if(destination2ht==null){
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
			}
			DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
			Map<Pair<Integer>,Double> trafficMatrix =null;
			if(seed==null)
				trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
			else{

				ObjectInputStream objectInputStream = null;
				try {
					objectInputStream = new ObjectInputStream(
							new FileInputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed));
					trafficMatrix = (Map<Pair<Integer>,Double>) objectInputStream.readObject();
				} catch (IOException e){System.out.println("File not found. Creating a new traffic matrix.");}
				catch (ClassNotFoundException e) {
					//e.printStackTrace();
					System.out.println("File not found. Creating a new traffic matrix.");
				}
				if(trafficMatrix==null)
					trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands,seed);
			}
			//System.out.println(trafficMatrix);
			System.out.println("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-margin.txt");
			File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-margin.txt");
			PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);

			CompactLPSolverObliviousPerformanceGivenDemandDAG lpperf = new CompactLPSolverObliviousPerformanceGivenDemandDAG();
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;
			if(ECMP_SOLUTION){
				Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
				arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);
			}else if(OPTIMAL_SOLUTION){
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG",setOfDemands);
			}else if(OPTIMAL_SOLUTION_UNCONSTRAINED){
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG-unconstrained",setOfDemands);
			}else if(!MARGIN)
				arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands);
			for(double w=10;w<=50;w+=5){
				double wReal = w/10;
				if(!ECMP_SOLUTION && MARGIN){
					arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands,wReal);
				}
				//lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal ,AGGREGATE_CONGESTION);
				lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal , destination2ht,AGGREGATE_CONGESTION);
				//lpSolver.computeObliviousPerformance(g, solution);
				writer.println(wReal + " " +lpperf.getOpt());
				System.out.println(wReal + " " +lpperf.getOpt());
			}
			writer.flush();
			writer.close();

			//System.out.print("Oblivious Solution: " + MainRacke.print(solution,g,lpSolver.getDemand2nameAndValue()));
			//System.out.println("performance-gravity: " + lpperf.getOpt());
			//if(lpperf.getMostCongestedIdArc()!=null)System.out.println("Most congested arc: " + g.getArcById(lpperf.getMostCongestedIdArc()));
			//System.out.println("lambda: " + lpperf.getLambdaVal());
			//System.out.println("Demands: " + MainRacke.printDemands(lpSolver.getDemand2nameAndValue()));
			//System.out.println("Lambda: " +lpSolver.getLambda());
			//System.out.println("Optimal solution for these demands: " +MainRacke.printFlow(lpSolver.getArc2idVertex2nameAndValue()));

		}
		else if(graphPath.equals("-computebestobliviousecmp")){
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			g= rfgr.readDirectedGraphWithDistances(args[1].replaceAll("\\.lgf", "-1.00\\.lgf"));
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);

			//System.out.println("graph: " + g);
			if(WEIGHT_1)
				for(Arc arc:g.getArcs())
					arc.setDistance(1d);

			Map<Pair<Integer>,Double> trafficMatrix =null;
			if(seed==null)
				trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
			else{

				ObjectInputStream objectInputStream = null;
				try {
					objectInputStream = new ObjectInputStream(
							new FileInputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed));
					trafficMatrix = (Map<Pair<Integer>,Double>) objectInputStream.readObject();
				} catch (IOException e){System.out.println("File not found solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed+". Creating a new traffic matrix.");}
				catch (ClassNotFoundException e) {
					//e.printStackTrace();
					System.out.println("File not found solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed+". Creating a new traffic matrix.");
				}
				if(trafficMatrix==null)
					trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands,seed);
			}
			//System.out.println(trafficMatrix);
			System.out.println("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-margin.txt");
			File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-margin.txt");
			PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);

			DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
			//CompactLPSolverObliviousPerformanceGivenDemand lpperf = new CompactLPSolverObliviousPerformanceGivenDemand();
			CompactLPSolverObliviousPerformanceGivenDemand lpperf = new CompactLPSolverObliviousPerformanceGivenDemand();
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;

			DecimalFormat df = new DecimalFormat("#.00");
			Map<Double,Double> margin2bestMargin = new HashMap<Double,Double>();
			String marginFileName = "solutions/"+graphNameFile + "-best-margin";
			for(double w=10;w<=50;w+=5){
				double wReal = w/10;
				double min=Double.MAX_VALUE;
				double bestMargin = -1;
				for(double ww=10;ww<=50;ww+=5){
					double wwReal = ww/10;

					double wGraph = wwReal;
					g= rfgr.readDirectedGraphWithDistances(args[1].replaceAll("\\.lgf", "-"+df.format(wGraph)+"\\.lgf"));
					String newDestinationFile = destinationFile.replaceAll("\\.lgf", "-"+df.format(wGraph)+"\\.lgf");

					destination2ht = readDestination2HtFromFile(newDestinationFile);
					if(destination2ht==null){
						destination2ht = dagGenerator.computeDag(g,setOfDemands);
					}

					ObjectOutputStream objectOutputStream=null;
					try {
						objectOutputStream = new ObjectOutputStream(
								new FileOutputStream("solutions/"+newDestinationFile));
						objectOutputStream.writeObject(destination2ht);
					} catch (IOException e) {e.printStackTrace();}
					finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}


					Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
					arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);


					lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal ,AGGREGATE_CONGESTION);
					//lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal , destination2ht,AGGREGATE_CONGESTION);
					//lpSolver.computeObliviousPerformance(g, solution);
					//writer.println(wReal + " " + wwReal + " " +lpperf.getOpt());
					System.out.println(wReal + " " + wwReal + " " +lpperf.getOpt());
					if(lpperf.getOpt()<min){
						min = lpperf.getOpt();
						bestMargin = wwReal;
					}
				}
				margin2bestMargin.put(wReal, bestMargin);
			}
			System.out.println(margin2bestMargin);
			ObjectOutputStream objectOutputStream=null;
			try {
				objectOutputStream = new ObjectOutputStream(
						new FileOutputStream(marginFileName));
				objectOutputStream.writeObject(margin2bestMargin);
			} catch (IOException e) {e.printStackTrace();}
			finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}
			}

			writer.flush();
			writer.close();

		}
		else if(graphPath.equals("-checkdemandmarginobliviousecmp")){
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			g= rfgr.readDirectedGraphWithDistances(args[1].replaceAll("\\.lgf", "-1.00\\.lgf"));
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);

			if(WEIGHT_1)
				for(Arc arc:g.getArcs())
					arc.setDistance(1d);

			Map<Pair<Integer>,Double> trafficMatrix =null;
			if(seed==null)
				trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
			else{

				ObjectInputStream objectInputStream = null;
				try {
					objectInputStream = new ObjectInputStream(
							new FileInputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed));
					trafficMatrix = (Map<Pair<Integer>,Double>) objectInputStream.readObject();
				} catch (IOException e){System.out.println("File not found. Creating a new traffic matrix.");}
				catch (ClassNotFoundException e) {
					//e.printStackTrace();
					System.out.println("File not found. Creating a new traffic matrix.");
				}
				if(trafficMatrix==null)
					trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands,seed);
			}
			//System.out.println(trafficMatrix);
			System.out.println("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-margin.txt");
			File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-margin.txt");
			PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);

			DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
			//CompactLPSolverObliviousPerformanceGivenDemand lpperf = new CompactLPSolverObliviousPerformanceGivenDemand();
			CompactLPSolverObliviousPerformanceGivenDemandDAG lpperf = new CompactLPSolverObliviousPerformanceGivenDemandDAG();
			Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;

			DecimalFormat df = new DecimalFormat("#.00");
			Map<Double,Double> margin2bestMargin = new HashMap<Double,Double>();
			String marginFileName = "solutions/"+graphNameFile + "-best-margin";

			ObjectInputStream objectInputStream = null;
			try {
				objectInputStream = new ObjectInputStream(
						new FileInputStream("solutions/"+graphNameFile + "-best-margin"));
				margin2bestMargin = (Map<Double,Double>) objectInputStream.readObject();
			} catch (IOException e){System.out.println("Best margin file not found.");}
			catch (ClassNotFoundException e) {System.out.println("Best margin file not found.");}

			for(double w=10;w<=50;w+=5){
				double wReal = w/10;
				//for(double ww=10;ww<=50;ww+=5){
				//	double wwReal = ww/10;

				double wGraph = margin2bestMargin.get(wReal);
				g= rfgr.readDirectedGraphWithDistances(args[1].replaceAll("\\.lgf", "-"+df.format(wGraph)+"\\.lgf"));
				String newGraphNameFile = graphNameFile.replaceAll("\\.lgf", "-"+df.format(wGraph)+"\\.lgf");
				String newDestinationFile = destinationFile.replaceAll("\\.lgf", "-"+df.format(wGraph)+"\\.lgf");

				destination2ht = readDestination2HtFromFile(newDestinationFile);
				if(destination2ht==null){
					destination2ht = dagGenerator.computeDag(g,setOfDemands);
				}

				ObjectOutputStream objectOutputStream=null;
				try {
					objectOutputStream = new ObjectOutputStream(
							new FileOutputStream(newDestinationFile));
					objectOutputStream.writeObject(destination2ht);
				} catch (IOException e) {e.printStackTrace();}
				finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}

				if(ECMP_SOLUTION){
					Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
					arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);
				}else if(MARGIN){
					//arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile.replace(".lgf", ""),setOfDemands,wGraph);
					arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands,wGraph);
				}

				//lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal ,AGGREGATE_CONGESTION);
				lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal , destination2ht,AGGREGATE_CONGESTION);
				//lpSolver.computeObliviousPerformance(g, solution);
				writer.println(wReal + " " +lpperf.getOpt());
				System.out.println(wReal + " " +lpperf.getOpt());
			}

			writer.flush();
			writer.close();

		}
		else if(graphPath.equals("-adjustsolution")){
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();
			//g= rfgr.readDirectedGraph(args[1]);
			g= rfgr.readDirectedGraphWithDistances(args[1]);

			if(WEIGHT_1)
				for(Arc arc:g.getArcs())
					arc.setDistance(1d);

			//ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
			setOfDemands = sodr.getSetOfDemands(g, null);
			if(SPECIFIC_DEMAND){
				sodr = new SetOfDemandsReaderMockMotivation();
				setOfDemands = sodr.getSetOfDemands(g, null);
			}

			destination2ht = readDestination2HtFromFile(destinationFile);
			if(destination2ht==null){
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
			}

			DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
			//Map<Integer,Map<Pair<Integer>,Double>> solution = reader.getSolutionForSignomialApproach(g, "results/"+args[1].split("/")[2]);
			Map<Integer,Map<Integer,Map<Integer,Double>>> phi_destination2vertex2edge2value = reader.getPhiForSignomialApproach(graphNameFile);
			new SolutionNormalizer().normalizeSolution(destination2ht, phi_destination2vertex2edge2value);

			new SplittingRatioWriter().writeToFile(graphNameFile,destination2ht, setOfDemands, phi_destination2vertex2edge2value);
			//lpSolver.computeObliviousPerformance(g, solution);
		}
		else{
			IMosekFormulationGeneratorAmpl mfg = null; 
			RocketFuelGraphReader rfgr = new RocketFuelGraphReader();

			if(graphPath.equals("-mock")){
				g = new GraphMock();
				destination2ht = d2ht.getDestination2ht();
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);
				destination2ht = dagGenerator.computeDag(g,setOfDemands);
				mfg = new MosekFormulationGeneratorAmpl();
				mfg.computeOptimalPerformance(graphNameFile,g, setOfDemands, destination2ht);
			}
			else if(graphPath.equals("-compact")){ // does not work because the dual is not convex
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMockForSlides();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				destination2ht = dagGenerator.computeDag(g,setOfDemands);
				mfg = new MosekFormulationGeneratorAmplCompact();
				mfg.computeOptimalPerformance(graphNameFile,g, setOfDemands, destination2ht);
			}
			else if(graphPath.equals("-checksinglefailure")){
				//construct all the graphs with a failed link
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				destination2ht = readDestination2HtFromFile(destinationFile);
				if(destination2ht==null){
					destination2ht = dagGenerator.computeDag(g,setOfDemands);
				}

				DecimalFormat df = new DecimalFormat("#.00");
				double scalingFactor = Double.parseDouble(args[2]);

				Map<Pair<Integer> , Double> trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
				File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-"+destinationTrafficFile+"-"+perfString+"-margin-failure-"+args[2]+".txt");
				PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
				DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();
				Map<Double,Double> margin2value = new HashMap<Double,Double>();
				double sum = 0d;
				int counter =0;
				for(Arc arc: g.getArcs()){
					if(arc.getId() % 2 == 1)
						continue;
					Arc reversedArc = g.getReversedArc(arc.getId());
					arc.setCapacity(arc.getCapacity()*scalingFactor);
					reversedArc.setCapacity(reversedArc.getCapacity()*scalingFactor);
					//System.out.println("ARC: " + arc);

					Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;
					if(ECMP_SOLUTION){
						Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
						arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands,arc,scalingFactor);
					}else if(OPTIMAL_SOLUTION){
						arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG",setOfDemands,arc,scalingFactor);
					}else if(OPTIMAL_SOLUTION_UNCONSTRAINED){
						arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG-unconstrained",setOfDemands,arc,scalingFactor);
					}else if(!MARGIN)
						arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands,arc,scalingFactor);
					for(double w=10;w<=50;w+=5){
						double wReal = w/10;
						if(!ECMP_SOLUTION && MARGIN){
							arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands,wReal,arc,scalingFactor);
						}

						CompactLPSolverObliviousPerformanceGivenDemandDAG lpperf = new CompactLPSolverObliviousPerformanceGivenDemandDAG();
						//lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal ,AGGREGATE_CONGESTION);
						//lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal , destination2ht,AGGREGATE_CONGESTION,arc,scalingFactor);
						lpperf.computeObliviousPerformance(g, setOfDemands, arc2demand2fraction, trafficMatrix, wReal , destination2ht,AGGREGATE_CONGESTION);
						//lpSolver.computeObliviousPerformance(g, solution);
						//writer.println(wReal + " " +lpperf.getOpt());
						System.out.println(wReal + " " +lpperf.getOpt());



						Double value = margin2value.get(wReal);
						if(value == null)
							value =0d;
						value += lpperf.getOpt();
						margin2value.put(wReal, value);

					}
					arc.setCapacity(arc.getCapacity()/scalingFactor);
					reversedArc.setCapacity(reversedArc.getCapacity()/scalingFactor);
					counter++;
					if(counter % 10 == 0)
						System.out.println(counter + " arcs out of " + g.getArcs().size()/2);
					System.out.println("------------");
				}

				System.out.println("---summary---");
				for(double w=10;w<=50;w+=5){
					double wReal = w/10;
					writer.println(wReal + " " + 2*margin2value.get(wReal)/g.getArcs().size());
					System.out.println(wReal + " " + 2*margin2value.get(wReal)/g.getArcs().size());
				}
				System.out.println("------------");

				writer.flush();
				writer.close();

			}
			else if(graphPath.equals("-checkconnectivity")){
				//construct all the graphs with a failed link
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				destination2ht  = new AugmentedShortestPathDagGenerator().computeDag(g,setOfDemands);

				Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);



				File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-connectivity.txt");
				PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
				int counter =0;
				for(Arc arc: g.getArcs()){
					if(arc.getId() % 2 == 1)
						continue;

					Map<Integer,Graph> destinationid2htReversed = computeReversedDestination2Ht(destination2ht);
					for(Integer destId : destinationid2htReversed.keySet()){
						Graph htReversed = destinationid2htReversed.get(destId);
						List<Integer> queue = new LinkedList<Integer>();
						Set<Integer> analyzed = new TreeSet<Integer>();
						queue.add(destId);
						
						while(!queue.isEmpty()){
							Integer nextId = queue.remove(0);
							if(analyzed.contains(nextId))
								continue;
							analyzed.add(nextId);
							Vertex next = htReversed.getVertexById(nextId);
							for(Arc arcNext: next.getArcs()){
								if(2*(arcNext.getId()/2) == 2*(arc.getId()/2))
									continue;
								queue.add(arcNext.getSecondEndPoint().getId());
							}
						}						
						counter+=analyzed.size()-1;
					}
				}
				double ratio = ((double)counter)/(setOfDemands.size()*(g.getArcs().size()/2));
				
				counter =0;
				for(Arc arc: g.getArcs()){
					if(arc.getId() % 2 == 1)
						continue;

					Map<Integer,Graph> destinationid2htReversed = computeReversedDestination2Ht(shortestPathDestination2ht);
					for(Integer destId : destinationid2htReversed.keySet()){
						Graph htReversed = destinationid2htReversed.get(destId);
						List<Integer> queue = new LinkedList<Integer>();
						Set<Integer> analyzed = new TreeSet<Integer>();
						queue.add(destId);
						
						while(!queue.isEmpty()){
							Integer nextId = queue.remove(0);
							if(analyzed.contains(nextId))
								continue;
							analyzed.add(nextId);
							Vertex next = htReversed.getVertexById(nextId);
							for(Arc arcNext: next.getArcs()){
								if(2*(arcNext.getId()/2) == 2*(arc.getId()/2))
									continue;
								queue.add(arcNext.getSecondEndPoint().getId());
							}
						}						
						counter+=analyzed.size()-1;
					}
				}
				double ratioShortestPath = ((double)counter)/(setOfDemands.size()*(g.getArcs().size()/2));
				writer.println(ratio + " " + ratioShortestPath);
				System.out.println(ratio + " " + ratioShortestPath);
				writer.flush();
				writer.close();

			}
			else if(graphPath.equals("-checkstretch")){
				//construct all the graphs with a failed link
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				destination2ht = readDestination2HtFromFile(destinationFile);
				if(destination2ht==null){
					destination2ht = dagGenerator.computeDag(g,setOfDemands);
				}


				File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-"+algorithm+"-stretch.txt");
				PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
				DAGSolutionFromFileReader reader = new DAGSolutionFromFileReader();


				Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fractionECMP =null;
				Map<Integer,Graph> shortestPathDestination2ht = new ShortestPathDagGenerator().computeDag(g, setOfDemands);
				arc2demand2fractionECMP = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);
				Map<Pair<Integer>,Map<Integer,Double>> demand2vertex2expectedStretchECMP = computeStretch(g, destination2ht, setOfDemands, arc2demand2fractionECMP);

				Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fractionOblivious =null;
				arc2demand2fractionOblivious = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands);
				Map<Pair<Integer>,Map<Integer,Double>> demand2vertex2expectedStretchOblivious= computeStretch(g, destination2ht, setOfDemands, arc2demand2fractionOblivious);


				Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction =null;
				if(ECMP_SOLUTION){
					arc2demand2fraction = computeECMPArc2Demand2Fraction(g,shortestPathDestination2ht ,setOfDemands);
				}else if(OPTIMAL_SOLUTION){
					arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG",setOfDemands);
				}else if(OPTIMAL_SOLUTION_UNCONSTRAINED){
					arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile+"-optimalDAG-unconstrained",setOfDemands);
				}else if(!MARGIN)
					arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands);
				for(double w=10;w<=50;w+=5){
					double wReal = w/10;
					if(!ECMP_SOLUTION && MARGIN){
						arc2demand2fraction = reader.getSolutionForSignomialApproach(g, "solutions/"+graphNameFile,setOfDemands,wReal);
					}

					Map<Pair<Integer>,Map<Integer,Double>> demand2vertex2expectedStretch = computeStretch(g, destination2ht, setOfDemands, arc2demand2fraction);
					double sum = 0d;
					double sumOblivious = 0d;
					for(Pair<Integer> demand : setOfDemands){
						sum+=  demand2vertex2expectedStretch.get(demand).get(demand.getSecond())/demand2vertex2expectedStretchECMP.get(demand).get(demand.getSecond());
						sumOblivious+=  demand2vertex2expectedStretchOblivious.get(demand).get(demand.getSecond())/demand2vertex2expectedStretchECMP.get(demand).get(demand.getSecond());
					}
					double average = sum / setOfDemands.size();
					double averageOblivious = sumOblivious / setOfDemands.size();
					System.out.println(wReal + " " + average + " " + averageOblivious);
					writer.println(wReal + " " + average+ " " + averageOblivious);
				}


				writer.flush();
				writer.close();

			}
			else if(graphPath.equals("-ecmp")){ // solves the oblivious performance problem with shortest paths DAGs with Gurobi
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				destination2ht = readDestination2HtFromFile(destinationFile);
				if(destination2ht==null){
					destination2ht = dagGenerator.computeDag(g,setOfDemands);
					ObjectOutputStream objectOutputStream=null;
					try {
						objectOutputStream = new ObjectOutputStream(
								new FileOutputStream("solutions/"+graphNameFile+"-"+destinationFile));
						objectOutputStream.writeObject(destination2ht);
					} catch (IOException e) {e.printStackTrace();}
					finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
				}
				Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction = computeECMPArc2Demand2Fraction(g,destination2ht,setOfDemands);
				Map<Pair<Integer>,Double> trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
				System.out.println("results-ecmp/"+graphNameFile.split("\\.")[0]+"-ecmp-"+destinationTrafficFile+"-"+perfString+".txt");
				File file2 = new File("results-ecmp/"+graphNameFile.split("\\.")[0]+"-ecmp-"+destinationTrafficFile+"-"+perfString+".txt");
				PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
				//lpSolver.computeObliviousPerformance(g, solution);
				perf.computePerformanceSpecificDemand(g, setOfDemands, arc2demand2fraction, trafficMatrix);
				writer.println(perf.getOpt());
				System.out.println("performance-gravity:"+perf.getOpt());
				if(perf.getMostCongestedIdArc()!=null)System.out.println("Most congested arc: " + g.getArcById(perf.getMostCongestedIdArc()));
				writer.flush();
				writer.close();

				//System.out.print("Oblivious Solution: " + MainRacke.print(arc2demand2fraction,g,lpSolver.getDemand2nameAndValue()));
				//System.out.println("Optimal solution for these demands: " +MainRacke.printFlow(lpSolver.getArc2idVertex2nameAndValue()));

				//MosekFormulationGeneratorAmplCheckSolution main = new MosekFormulationGeneratorAmplCheckSolution();
				//main.setFile("ecmp");
				//main.checkDAGSolution(args[1],g,destination2ht,arc2demand2fraction);

			}
			else if(graphPath.equals("-optimal")){ // solves the oblivious performance problem with shortest paths DAGs with Gurobi
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);
				System.out.println(g);

				Map<Pair<Integer>,Double> trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
				System.out.println("results-optimal/"+graphNameFile.split("\\.")[0]+"-optimalDAG-unconstrained-"+destinationTrafficFile+"-"+perfString+".txt");
				File file2 = new File("results-optimal/"+graphNameFile.split("\\.")[0]+"-optimalDAG-unconstrained-"+destinationTrafficFile+"-"+perfString+".txt");
				PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
				//lpSolver.computeObliviousPerformance(g, solution);
				MCFSolver mcf = new MCFSolver();
				if(AGGREGATE_CONGESTION)
					mcf.computeOptimalCongestion(g, trafficMatrix,true);
				else
					mcf.computeOptimalCongestion(g, trafficMatrix);
				writer.println(mcf.getMax());
				System.out.println("performance-gravity:"+mcf.getMax());
				writer.flush();
				writer.close();

				//System.out.println("Optimal solution for these demands: " + MainRacke.printFlow(mcf.getArc2idVertex2nameAndValue()));
				new SplittingRatioWriter().writeToFile(graphNameFile+"-optimalDAG-unconstrained",g, setOfDemands, mcf.getDestination2vertex2edge2value());			}
			else if(graphPath.equals("-optimaldag")){ // solves the oblivious performance problem with shortest paths DAGs with Gurobi
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);
				System.out.println(g);

				destination2ht = readDestination2HtFromFile(destinationFile);
				if(destination2ht==null){
					destination2ht = dagGenerator.computeDag(g,setOfDemands);
				}

				Map<Pair<Integer>,Double> trafficMatrix = null;
				if(seed==null)
					trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
				else{

					ObjectInputStream objectInputStream = null;
					try {
						objectInputStream = new ObjectInputStream(
								new FileInputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed));
						trafficMatrix = (Map<Pair<Integer>,Double>) objectInputStream.readObject();
					} catch (IOException e){System.out.println("File not found. Creating a new traffic matrix.");}
					catch (ClassNotFoundException e) {
						//e.printStackTrace();
						System.out.println("File not found. Creating a new traffic matrix.");
					}
					if(trafficMatrix==null)
						trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands,seed);
				}

				System.out.println("results-optimal/"+graphNameFile.split("\\.")[0]+"-optimalDAG-"+destinationTrafficFile+"-"+perfString+".txt");
				File file2 = new File("results-optimal/"+graphNameFile.split("\\.")[0]+"-optimalDAG-"+destinationTrafficFile+"-"+perfString+".txt");
				PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
				//lpSolver.computeObliviousPerformance(g, solution);
				MCFSolverDAG mcf = new MCFSolverDAG();
				if(AGGREGATE_CONGESTION)
					mcf.computeOptimalCongestion(g, trafficMatrix,destination2ht,true);
				else
					mcf.computeOptimalCongestion(g, trafficMatrix,destination2ht);
				writer.println(mcf.getMax());
				System.out.println("performance-gravity:"+mcf.getMax());
				writer.flush();
				writer.close();

				System.out.println("Optimal solution for these demands: " + MainRacke.printFlow(mcf.getArc2idVertex2nameAndValue()));
				new SplittingRatioWriter().writeToFile(graphNameFile+"-optimalDAG",destination2ht, setOfDemands, mcf.getDestination2vertex2edge2value());
			}
			else if(graphPath.equals("-ecmp-igpwo")){ // solves the oblivious performance problem with shortest paths DAGs with Gurobi
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				Map<Pair<Integer>,Double> trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
				destination2ht = readDestination2HtFromFile(destinationFile);
				if(destination2ht==null){
					IIGPWOSolver igpwo = new IGPWOSolverInC();
					igpwo.computeBestECMPWeights(g, setOfDemands, trafficMatrix);
					destination2ht = computeD2HTShortestPath(g,setOfDemands);
					if(AUGMENT_PATHS)
						destination2ht = computeD2HTAddArcsCorrect(g,setOfDemands,destination2ht);
					ObjectOutputStream objectOutputStream=null;
					try {
						objectOutputStream = new ObjectOutputStream(
								new FileOutputStream("destination2ht-IGPWO.txt"));
						objectOutputStream.writeObject(destination2ht);
					} catch (IOException e) {e.printStackTrace();}

				}
				Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction = computeECMPArc2Demand2Fraction(g,destination2ht,setOfDemands);
				System.out.println("results/"+graphNameFile.split("\\.")[0]+"-ecmp-igpwo-"+destinationTrafficFile+"-"+perfString+".txt");
				File file2 = new File("results/"+graphNameFile.split("\\.")[0]+"-ecmp-igpwo-"+destinationTrafficFile+"-"+perfString+".txt");
				PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
				//lpSolver.computeObliviousPerformance(g, solution);
				perf.computePerformanceSpecificDemand(g, setOfDemands, arc2demand2fraction, trafficMatrix);
				writer.println(perf.getOpt());
				System.out.println("performance-gravity:"+perf.getOpt());
				writer.flush();
				writer.close();

				//System.out.print("Oblivious Solution: " + MainRacke.print(arc2demand2fraction,g,lpSolver.getDemand2nameAndValue()));
				//System.out.println("Optimal solution for these demands: " +MainRacke.printFlow(lpSolver.getArc2idVertex2nameAndValue()));

				//MosekFormulationGeneratorAmplCheckSolution main = new MosekFormulationGeneratorAmplCheckSolution();
				//main.setFile("ecmp-igpwo");
				//main.checkDAGSolution(args[1],g,destination2ht,arc2demand2fraction);

			}
			else if(graphPath.equals("-racke")){ // solves the oblivious performance problem with shortest paths DAGs with Gurobi
				String file = args[1];

				g= rfgr.readDirectedGraphWithDistances(file);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				Integer numberOfTrees = readNumberOfTreesFromFile(file);
				if(numberOfTrees == null){
					MainRacke racke = new MainRacke();
					racke.computeRacke(file,WEIGHT_1);
					numberOfTrees = racke.getNumberOfTrees();
				}


				for(int i=1; i<=numberOfTrees;i++){
					Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction = readRackeSolution(file,i);
					Map<Pair<Integer>,Double> trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
					System.out.println("results-racke/"+graphNameFile.split("\\.")[0]+"-racke-"+i+"-"+destinationTrafficFile+"-"+perfString+".txt");
					File file2 = new File("results-ra"
							+ "cke/"+graphNameFile.split("\\.")[0]+"-racke-"+i+"-"+destinationTrafficFile+"-"+perfString+".txt");
					PrintWriter writer =new PrintWriter(file2);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
					//lpSolver.computeObliviousPerformance(g, solution);
					perf.computePerformanceSpecificDemand(g, setOfDemands, arc2demand2fraction, trafficMatrix,AGGREGATE_CONGESTION);
					writer.println(perf.getOpt());
					System.out.println("performance-gravity:"+perf.getOpt());
					if(perf.getMostCongestedIdArc()!=null)System.out.println("Most congested arc: " + g.getArcById(perf.getMostCongestedIdArc()));
					writer.flush();
					writer.close();
				}

			}
			else if(graphPath.equals("-whatisthebestdag")){
				DecimalFormat df = new DecimalFormat("#.00");
				Map<Double,Double> margin2bestMargin = new HashMap<Double,Double>();
				String marginFileName = "solutions/"+graphNameFile + "-best-margin";

				ObjectInputStream objectInputStream = null;
				try {
					objectInputStream = new ObjectInputStream(
							new FileInputStream("solutions/"+graphNameFile + "-best-margin"));
					margin2bestMargin = (Map<Double,Double>) objectInputStream.readObject();
				} catch (IOException e){System.out.println("Best margin file not found.");}
				catch (ClassNotFoundException e) {System.out.println("Best margin file not found.");}

				double wGraph = margin2bestMargin.get(Double.parseDouble(args[2]));
				System.out.println(args[1].split("/")[2].replaceAll("\\.lgf", "-"+df.format(wGraph)+"\\.lgf"));
			}
			else if(graphPath.equals("-demand")){ // signomial approximation formulation using demand estimate
				g= rfgr.readDirectedGraphWithDistances(args[1]);
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);


				destination2ht = dagGenerator.computeDag(g,setOfDemands);
				ObjectOutputStream objectOutputStream=null;
				try {
					objectOutputStream = new ObjectOutputStream(
							new FileOutputStream(destinationFile));
					objectOutputStream.writeObject(destination2ht);
				} catch (IOException e) {e.printStackTrace();}
				finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}

				Map<Pair<Integer>,Double> trafficMatrix =null;
				if(seed==null){
					trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands);
					File fileTM = new File("solutions/"+destinationTrafficFile+"/"+graphNameFile+".txt");
					PrintWriter writerTM =new PrintWriter(fileTM);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
					for(Pair<Integer> demand : trafficMatrix.keySet())
						writerTM.println(demand.getFirst()+" "+demand.getSecond()+ " "+trafficMatrix.get(demand));
					writerTM.flush();writerTM.close();
				}
				else
				{
					ObjectInputStream objectInputStream = null;
					try {
						objectInputStream = new ObjectInputStream(
								new FileInputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed));
						trafficMatrix = (Map<Pair<Integer>,Double>) objectInputStream.readObject();
					} catch (IOException e){System.out.println("File not found. Creating a new traffic matrix.");}
					catch (ClassNotFoundException e) {
						//e.printStackTrace();
						System.out.println("File not found. Creating a new traffic matrix.");
					}
					if(trafficMatrix==null){
						trafficMatrix = trafficDemandCalculator.computeTrafficMatrix(g, setOfDemands,seed);
						objectOutputStream=null;
						try {
							File fileTM = new File("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed+".txt");
							PrintWriter writerTM =new PrintWriter(fileTM);//new FileOutputStream(file.split("\\.")[0]+".txt",true), true);
							for(Pair<Integer> demand : trafficMatrix.keySet())
								writerTM.println(demand.getFirst()+" "+demand.getSecond()+ " "+trafficMatrix.get(demand));
							writerTM.flush();writerTM.close();
							objectOutputStream = new ObjectOutputStream(
									new FileOutputStream("solutions/"+destinationTrafficFile+"/"+graphNameFile+"-"+seed));
							objectOutputStream.writeObject(trafficMatrix);
						} catch (IOException e) {e.printStackTrace();}
						finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
					}
				}

				mfg =new MosekFormulationGeneratorAmplGivenDemand();
				mfg.computeOptimalPerformance(graphNameFile,g, setOfDemands, destination2ht,trafficMatrix,Double.parseDouble(args[2]),AGGREGATE_CONGESTION);
			}
			else if(graphPath.equals("-createrandomgraph")){ // signomial approximation formulation
				//g= rfgr.readDirectedGraph(graphPath);
				RandomGraph randomGraph = new RandomGraph();
				randomGraph.buildRandomNetwork(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
				//ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();

				ObjectOutputStream objectOutputStream=null;
				try {
					objectOutputStream = new ObjectOutputStream(
							new FileOutputStream(args[3]));
					objectOutputStream.writeObject(randomGraph);
				} catch (IOException e) {e.printStackTrace();}
				finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}
			}
			else  if(graphPath.equals("-hose")){ // signomial approximation formulation plus hose model
				//g= rfgr.readDirectedGraph(graphPath);
				g= rfgr.readDirectedGraphWithDistances(graphPath);
				//ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);

				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				destination2ht = dagGenerator.computeDag(g,setOfDemands);

				ObjectOutputStream objectOutputStream=null;
				try {
					objectOutputStream = new ObjectOutputStream(
							new FileOutputStream(destinationFile));
					objectOutputStream.writeObject(destination2ht);
				} catch (IOException e) {e.printStackTrace();}
				finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}

				mfg =new MosekFormulationGeneratorAmplHose();
				mfg.computeOptimalPerformance(graphNameFile,g, setOfDemands, destination2ht);
			}
			else{ // signomial approximation formulation
				//g= rfgr.readDirectedGraph(graphPath);
				g= rfgr.readDirectedGraphWithDistances(graphPath);
				//ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				ISetOfDemandsReader sodr = new SetOfDemandsReaderMock();
				setOfDemands = sodr.getSetOfDemands(g, null);
				if(SPECIFIC_DEMAND){
					sodr = new SetOfDemandsReaderMockMotivation();
					setOfDemands = sodr.getSetOfDemands(g, null);
				}


				if(WEIGHT_1)
					for(Arc arc:g.getArcs())
						arc.setDistance(1d);

				destination2ht = dagGenerator.computeDag(g,setOfDemands);

				ObjectOutputStream objectOutputStream=null;
				try {
					objectOutputStream = new ObjectOutputStream(
							new FileOutputStream(destinationFile));
					objectOutputStream.writeObject(destination2ht);
				} catch (IOException e) {e.printStackTrace();}
				finally{try {objectOutputStream.close();} catch (IOException e) {e.printStackTrace();}}

				mfg =new MosekFormulationGeneratorAmpl();
				mfg.computeOptimalPerformance(args[0].split("/")[2],g, setOfDemands, destination2ht,AGGREGATE_CONGESTION);
			}
		}
	}



	private static double computeSumOfArc(Map<Integer, Graph> destination2ht) {
		double sum=0d;
		for(Integer destId : destination2ht.keySet()){
			Set<Integer> analyzed = new TreeSet<Integer>();
			for(Arc arc: destination2ht.get(destId).getArcs()){
				if(analyzed.contains(2*(arc.getId()/2)))
					continue;
				sum += arc.getCapacity();
				analyzed.add(2*(arc.getId()/2));
			}
		}
		return sum;
	}


	private static Integer readNumberOfTreesFromFile(String file) {
		Integer numberOfTrees = null;
		ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(
					new FileInputStream("solutions-racke/"+file.split("\\.")[0].split("/")[2]+"-racke-numberOfTrees.txt"));
			numberOfTrees = (Integer) objectInputStream.readObject();
		} catch (IOException e) {System.out.println("solutions-racke/"+file.split("\\.")[0].split("/")[2]+"-racke-numberOfTrees.txt not found. Generating destination2ht.");}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return numberOfTrees;
	}


	private static Map<Integer, Map<Pair<Integer>, Double>> readRackeSolution(
			String file, int i) {
		Map<Integer, Map<Pair<Integer>, Double>> solution = null;
		ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(
					new FileInputStream("solutions-racke/"+file.split("\\.")[0].split("/")[2]+"-solution"+i));
			solution = (Map<Integer, Map<Pair<Integer>, Double>>) objectInputStream.readObject();
		} catch (IOException e) {System.out.println("File "+"solutions-racke/"+file.split("\\.")[0].split("/")[2]+"-solution"+i+" not found. Generating destination2ht.");}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return solution;
	}


	public static Map<Integer, Graph> readDestination2HtFromFile(String destinationFile) {
		Map<Integer,Graph> destination2ht= null;
		ObjectInputStream objectInputStream = null;
		try {
			objectInputStream = new ObjectInputStream(
					new FileInputStream("solutions/"+destinationFile));
			destination2ht = (Map<Integer,Graph>) objectInputStream.readObject();
		} catch (IOException e) {System.out.println("File "+"solutions/"+destinationFile+" not found. Generating destination2ht.");}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return destination2ht;
	}

	public static Map<Integer, Map<Pair<Integer>, Double>> computeECMPArc2Demand2Fraction(
			Graph g, Map<Integer, Graph> destination2ht, List<Pair<Integer>> setOfDemands) {
		return computeECMPArc2Demand2Fraction(g, destination2ht, setOfDemands,null,0);
	}

	public static Map<Integer, Map<Pair<Integer>, Double>> computeECMPArc2Demand2Fraction(
			Graph g, Map<Integer, Graph> destination2ht, List<Pair<Integer>> setOfDemands, Arc arcWReducedCapacity, double reducingFactor) {

		Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
		Map<Integer, Graph> destination2htReversed = computeReversedDestination2Ht(destination2ht);

		for(Pair<Integer> demand : setOfDemands){
			//System.out.println("demand: "+demand);
			Map<Integer,Double> vertex2ingoingFlow = new HashMap<Integer,Double>();
			Integer sourceId = demand.getFirst();
			Integer destId = demand.getSecond();
			Graph ht = destination2ht.get(destId);
			Vertex source = ht.getVertexById(sourceId);
			vertex2ingoingFlow.put(sourceId, 1d);
			Graph htReversed = destination2htReversed.get(destId);


			// start topological search from the source vertex
			Set<Vertex> queue = new TreeSet<Vertex>();
			Set<Vertex> analyzed = new TreeSet<Vertex>();
			queue.add(source);
			for(Vertex vertex : htReversed.getVertices())
				if(vertex.getId() != sourceId && htReversed.getVertexById(vertex.getId()).getArcs().size()==0){
					queue.add(destination2ht.get(destId).getVertexById(vertex.getId()));
					vertex2ingoingFlow.put(vertex.getId(), 0d);
				}

			while(!queue.isEmpty()){
				Vertex next = getNextVertex(queue,analyzed,g,destination2ht.get(destId),htReversed);
				if(analyzed.contains(next))
					continue;
				queue.remove(next);
				analyzed.add(next);
				int size = next.getArcs().size();
				boolean resplit = false;
				double sumCapacities = 0d;
				for(Arc arc : next.getArcs()){
					if(arcWReducedCapacity!= null && 2*(arc.getId()/2) == 2*(arcWReducedCapacity.getId()/2)){  //FIXME: fix this if you want undirected
						resplit = true;
						sumCapacities+=arc.getCapacity()*reducingFactor;
					}
					else
						sumCapacities+=arc.getCapacity();
				}

				for(Arc arc:next.getArcs()){
					Map<Pair<Integer>,Double> demand2fraction = arc2demand2fraction.get(arc.getId());
					if(demand2fraction==null){
						demand2fraction = new HashMap<Pair<Integer>,Double>();
						arc2demand2fraction.put(arc.getId(), demand2fraction);
					}
					double amount = 0d;
					if(resplit)
						if(2*(arc.getId()/2) == 2*(arcWReducedCapacity.getId()/2)) //FIXME: fix this if you want undirected
							amount = vertex2ingoingFlow.get(next.getId())/size*reducingFactor/(1-(1-reducingFactor)/size);
						else
							amount = vertex2ingoingFlow.get(next.getId())/size/(1-(1-reducingFactor)/size);
					else
						amount = vertex2ingoingFlow.get(next.getId())/size;
					demand2fraction.put(demand, amount);
					Double ingoingFlow = vertex2ingoingFlow.get(arc.getSecondEndPoint().getId());
					if(ingoingFlow==null)
						ingoingFlow=0d;
					vertex2ingoingFlow.put(arc.getSecondEndPoint().getId(), ingoingFlow + amount);
					queue.add(arc.getSecondEndPoint());
				}

			}

		}


		return arc2demand2fraction;
	}

	public static Map<Pair<Integer>, Map<Integer,Double>> computeStretch(
			Graph g, Map<Integer, Graph> destination2ht, List<Pair<Integer>> setOfDemands, Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction) {

		Map<Pair<Integer>, Map<Integer,Double>> demand2vertex2expectedStretch = new HashMap<Pair<Integer>, Map<Integer,Double>>();

		Map<Integer, Graph> destination2htReversed = computeReversedDestination2Ht(destination2ht);

		for(Pair<Integer> demand : setOfDemands){
			//System.out.println("Demand: "+demand);
			Map<Integer,Double> vertex2expectedStretch = new HashMap<Integer,Double>();
			demand2vertex2expectedStretch.put(demand, vertex2expectedStretch);
			vertex2expectedStretch.put(demand.getFirst(), 0d);
			//System.out.println("demand: "+demand);
			Map<Integer,Double> vertex2ingoingFlow = new HashMap<Integer,Double>();
			Integer sourceId = demand.getFirst();
			Integer destId = demand.getSecond();
			Graph ht = destination2ht.get(destId);
			Vertex source = ht.getVertexById(sourceId);
			vertex2ingoingFlow.put(sourceId, 1d);
			Graph htReversed = destination2htReversed.get(destId);


			// start topological search from the source vertex
			Set<Vertex> queue = new TreeSet<Vertex>();
			Set<Vertex> analyzed = new TreeSet<Vertex>();
			queue.add(source);
			for(Vertex vertex : htReversed.getVertices())
				if(vertex.getId() != sourceId && htReversed.getVertexById(vertex.getId()).getArcs().size()==0){
					queue.add(destination2ht.get(destId).getVertexById(vertex.getId()));
					vertex2ingoingFlow.put(vertex.getId(), 0d);
					vertex2expectedStretch.put(vertex.getId(), 0d);
				}

			while(!queue.isEmpty()){
				Vertex next = getNextVertex(queue,analyzed,g,destination2ht.get(destId),htReversed);
				//System.out.println("  vertex2expectedStretch("+next.getId()+"): "+ vertex2expectedStretch.get(next.getId()));
				if(analyzed.contains(next))
					continue;
				queue.remove(next);
				analyzed.add(next);
				Double inFlowAtNext = vertex2ingoingFlow.get(next.getId());
				if(inFlowAtNext >0 )
					vertex2expectedStretch.put(next.getId(), vertex2expectedStretch.get(next.getId())/inFlowAtNext);

				for(Arc arc:next.getArcs()){
					Double amount =0d;
					if(arc2demand2fraction.get(arc.getId()) != null)
						amount = arc2demand2fraction.get(arc.getId()).get(demand);
					//System.out.println("    arc:"+arc+ " fraction: " + amount);

					Double ingoingFlow = vertex2ingoingFlow.get(arc.getSecondEndPoint().getId());
					Double expectedStretch = vertex2expectedStretch.get(arc.getSecondEndPoint().getId());
					if(amount == null)
						amount = 0d;
					if(ingoingFlow==null)
						ingoingFlow=0d;
					if(expectedStretch ==null)
						expectedStretch =0d;
					vertex2ingoingFlow.put(arc.getSecondEndPoint().getId(), ingoingFlow + amount);
					vertex2expectedStretch.put(arc.getSecondEndPoint().getId(), expectedStretch + amount*(vertex2expectedStretch.get(next.getId())+1));
					//System.out.println("      update vertex2expectedStretch("+arc.getSecondEndPoint().getId()+"): "+ vertex2expectedStretch.get(arc.getSecondEndPoint().getId()));
					queue.add(arc.getSecondEndPoint());
				}

			}

		}


		return demand2vertex2expectedStretch;
	}

	public static Map<Integer, Graph> computeReversedDestination2Ht(
			Map<Integer, Graph> destination2ht) {
		Map<Integer, Graph> destination2htReversed = new HashMap<Integer, Graph>();

		for(Integer destId : destination2ht.keySet()){

			Graph reversed = destination2ht.get(destId).getReversedCopy();
			destination2htReversed.put(destId, reversed);
		}

		return destination2htReversed;
	}

	private static Vertex getNextVertex(Set<Vertex> queue, Set<Vertex> analyzed, Graph g, Graph ht, Graph reversedHt) {
		outerloop:
			for(Vertex vertex : queue){
				for (Arc arcRev : reversedHt.getVertexById(vertex.getId()).getArcs()){
					if(!analyzed.contains(ht.getVertexById(arcRev.getSecondEndPoint().getId()))){
						continue outerloop;
					}
				}
				return vertex;
			}
	return null;
	}

	public static Map<Integer, Graph> computeD2HTShortestPath(Graph g, List<Pair<Integer>> setOfDemands) {
		Map<Integer,Graph> destination2ht = new HashMap<Integer,Graph>();
		Map<Integer,Set<Integer>> destination2sources = computeDestination2Sources(setOfDemands);
		g.createDistanceMatrixAll();

		//compute shortest path DAG
		for(Integer destId : destination2sources.keySet()){
			Set<Integer> analyzed = new HashSet<Integer>();
			Graph ht = new Graph();
			destination2ht.put(destId, ht);
			for(Integer sourceId : destination2sources.get(destId)){
				if(analyzed.contains(sourceId))
					continue;
				List<Integer> queue = new LinkedList<Integer>();
				Set<Integer> queued = new HashSet<Integer>();
				queue.add(sourceId);
				queued.add(sourceId);
				Vertex source = new Vertex();
				source.setId(sourceId);
				ht.addVertex(source);
				while(!queue.isEmpty()){
					Vertex v = g.getVertexById(queue.remove(0));
					if(analyzed.contains(v.getId()))
						continue;
					analyzed.add(v.getId());
					for(Arc arc : g.getIdVertex2idVertex2allNextArc().get(v.getId()).get(destId)){
						Arc arcCopy = new Arc();
						arcCopy.setCapacity(arc.getCapacity());
						arcCopy.setDistance(arc.getDistance());
						arcCopy.setId(arc.getId());
						arcCopy.setFirstEndPoint(ht.getVertexById(v.getId()));
						Vertex head = ht.getVertexById(arc.getSecondEndPoint().getId());
						if(head==null){
							head = new Vertex();
							head.setId(arc.getSecondEndPoint().getId());
							ht.addVertex(head);
						}
						arcCopy.setSecondEndPoint(head);
						ht.addDirectedArc(arcCopy);
						queue.add(head.getId());
					}
				}	
			}			
		}

		return destination2ht;
	}

	public static Map<Integer, Graph> computeD2HTAddArcsCorrect(Graph g, List<Pair<Integer>> setOfDemands,
			Map<Integer,Graph> destination2ht ) {

		Map<Integer, Graph> destination2htReversed = MainMosekFormulationGeneratorAmpl.computeReversedDestination2Ht(destination2ht);
		for(Integer destId : destination2ht.keySet()){
			Graph ht = destination2ht.get(destId);
			Map<Integer,Integer> order2vertexId = computeTopologicalOrder(g,ht,destination2htReversed.get(destId),destId);
			Set<Integer> analyzed = new TreeSet<Integer>();
			for(int i=order2vertexId.size()-1;i>0;i--){
				Vertex vFromG =g.getVertexById(order2vertexId.get(i));
				Vertex vFromHt =ht.getVertexById(order2vertexId.get(i));
				if(vFromHt == null){
					vFromHt = new Vertex();
					vFromHt.setId(order2vertexId.get(i));
					ht.addVertex(vFromHt);
				}
				analyzed.add(order2vertexId.get(i));
				for(Arc arc : vFromG.getArcs()){
					if(ht.getArcById(arc.getId())!=null)
						continue;
					if(!analyzed.contains(arc.getSecondEndPoint().getId())){
						Arc arcHt = new Arc();
						arcHt.setCapacity(arc.getCapacity());
						arcHt.setDistance(arc.getDistance());
						arcHt.setFirstEndPoint(vFromHt);
						arcHt.setId(arc.getId());
						Vertex head = ht.getVertexById(arc.getSecondEndPoint().getId());
						if(head== null){
							head = new Vertex();
							head.setId(arc.getSecondEndPoint().getId());
							ht.addVertex(head);
						}
						arcHt.setSecondEndPoint(head);
						ht.addDirectedArc(arcHt);
					}
				}
			}
		}

		return destination2ht;
	}

	private static Map<Integer, Integer> computeTopologicalOrder( Graph g, Graph ht, Graph htReversed, Integer destId) {
		Map<Integer,Integer> order2vertexId = new HashMap<Integer,Integer>();

		Set<Integer> analyzed = new TreeSet<Integer>();
		List<Integer> queue = new LinkedList<Integer>();
		queue.add(destId);
		int counter =0;
		while(!queue.isEmpty()){
			Integer id = queue.remove(0);
			Vertex v = htReversed.getVertexById(id);
			if(v==null){
				v = new Vertex();
				v.setId(id);
				ht.addVertex(v);
			}
			analyzed.add(v.getId());
			order2vertexId.put(counter++,v.getId());
			outerloop:
				for(Arc arc: v.getArcs()){
					Vertex nextReversed = arc.getSecondEndPoint(); //reversed graph
					Vertex next = ht.getVertexById(nextReversed.getId());
					for(Arc arc2 : next.getArcs())
						if(!analyzed.contains(arc2.getSecondEndPoint().getId())){
							continue outerloop;
						}
					queue.add(arc.getSecondEndPoint().getId());
				}
			Vertex vFromG = g.getVertexById(v.getId());
			for(Arc arc: vFromG.getArcs()){
				if(ht.getVertexById(arc.getSecondEndPoint().getId())==null)
					queue.add(arc.getSecondEndPoint().getId());
			}

		}

		return order2vertexId;
	}

	/*private static Map<Integer, Integer> computeTopologicalOrderBasedOnWeights( Graph g, Graph ht, Graph htReversed, Integer destId) {
		Map<Integer,Integer> order2vertexId = new HashMap<Integer,Integer>();

		Queue<Tuple<Double,Integer>> distance2vertex = new PriorityQueue<Tuple<Double,Integer>>();
		Map<Integer,Double> vertexId2distance = new HashMap<Integer,Double>();
		Set<Integer> analyzed = new TreeSet<Integer>();
		List<Integer> queue = new LinkedList<Integer>();
		distance2vertex.add(new Tuple<Double,Integer>(0d,destId));
		vertexId2distance.put(destId, 0d);
		int counter =0;
		while(analyzed.size()<g.getVertices().size()){
			Tuple<Double,Integer> distance2id = distance2vertex.poll();
			Double distance = (Double)distance2id.getX();
			Integer id = distance2id.getY();

			Vertex v = htReversed.getVertexById(id);
			if(v==null){
				v = new Vertex();
				v.setId(id);
				ht.addVertex(v);
			}
			analyzed.add(v.getId());
			order2vertexId.put(counter++,v.getId());
			for(Arc arc: v.getArcs()){
				Vertex nextReversed = arc.getSecondEndPoint(); //reversed graph
				if(vertexId2distance.get(nextReversed.getId()) < distance+arc.getDistance()) {
					distance2vertex.add(new Tuple<Double,Integer>(distance+arc.getDistance(),nextReversed.getId()));
					vertexId2distance.put(nextReversed.getId(), distance + arc.getDistance()); 
				}
			}
			Vertex vFromG = g.getVertexById(v.getId());
			for(Arc arc: vFromG.getArcs()){
				if(ht.getVertexById(arc.getSecondEndPoint().getId())==null)
					queue.add(arc.getSecondEndPoint().getId());
			}

		}

		return order2vertexId;
	}
	 */


	private static Map<Integer, Graph> computeD2HTAddArcsWrong(Graph g, List<Pair<Integer>> setOfDemands,
			Map<Integer,Graph> destination2ht ) {

		Map<Integer,Set<Integer>> destination2sources = computeDestination2Sources(setOfDemands);

		//add unassigned arcs
		for(Integer destId : destination2sources.keySet()){
			boolean pathFound = true;
			Graph ht = destination2ht.get(destId);

			while(pathFound){
				pathFound=false;
				for(Vertex vHt : destination2ht.get(destId).getVertices()){
					if(vHt.getId()==destId)
						continue;
					List<Arc> path = findPath(g, ht, g.getVertexById(vHt.getId())); 
					if(path!=null){
						pathFound=true;
						for(Arc arc : path){
							// create arc in Ht
							Arc arcHt = new Arc();
							Vertex tailHt = ht.getVertexById(arc.getFirstEndPoint().getId());
							Vertex headHt = ht.getVertexById(arc.getSecondEndPoint().getId());
							if(tailHt==null){
								tailHt = new Vertex();
								tailHt.setId(arc.getFirstEndPoint().getId());
								ht.addVertex(tailHt);
							}if(headHt==null){
								headHt = new Vertex();
								headHt.setId(arc.getSecondEndPoint().getId());
								ht.addVertex(headHt);
							}
							arcHt.setId(arc.getId());
							arcHt.setCapacity(arc.getCapacity());
							arcHt.setDistance(arc.getDistance());
							arcHt.setFirstEndPoint(tailHt);
							arcHt.setSecondEndPoint(headHt);
							ht.addDirectedArc(arcHt);
						}
					}
				}
			}

		}
		return destination2ht;
	}

	private static Map<Integer, Set<Integer>> computeDestination2Sources(
			List<Pair<Integer>> setOfDemands) {
		Map<Integer, Set<Integer>> destination2sources = new HashMap<Integer,Set<Integer>>();
		for(Pair<Integer> demand : setOfDemands){
			if(destination2sources.get(demand.getSecond())==null)
				destination2sources.put(demand.getSecond(), new TreeSet<Integer>());
			destination2sources.get(demand.getSecond()).add(demand.getFirst());
		}
		return destination2sources;
	}

	private static List<Arc> findPath(Graph g, Graph ht, Vertex from){
		List<Arc> path = null;
		Set<Integer> analyzed = new TreeSet<Integer>();
		analyzed.add(from.getId());
		for(Arc arc : from.getArcs()){
			if(ht.getArcs().contains(arc) || ht.getArcs().contains(g.getReversedArc(arc.getId())))
				continue;
			path = findPathRec(g, ht, analyzed, arc.getSecondEndPoint());
			if(path!=null){
				path.add(arc);
				break;
			}	
		}
		if(path!=null){
			if(isDescendant(ht.getVertexById(path.get(path.size()-1).getSecondEndPoint().getId()), ht.getVertexById(from.getId())))
				return reversePath(g,path);
			else
				return path;
		}
		return null;
	}

	private static List<Arc> findPathRec(Graph g, Graph ht, Set<Integer> analyzed, Vertex from){
		if(ht.getVertices().contains(from)){
			return new LinkedList<Arc>();
		}else{
			if(analyzed.contains(from.getId()))
				return null;
			for(Arc arc : from.getArcs()){
				if(ht.getArcs().contains(arc) || ht.getArcs().contains(g.getReversedArc(arc.getId())))
					continue;
				List<Arc> path = findPathRec(g, ht, analyzed, arc.getSecondEndPoint());
				if(path!=null){
					path.add(arc);
					return path;
				}	
			}
		}
		return null;
	}

	private static List<Arc> reversePath(Graph g, List<Arc> path) {
		List<Arc> reversedPath = new LinkedList<Arc>();
		for(Arc arc : path){
			reversedPath.add(g.getArcById(arc.getId()));
		}
		return reversedPath;
	}


	private static boolean isDescendant( Vertex from, Vertex to){
		return isDescendantRec( from, to, new TreeSet<Integer>());
	}

	private static boolean isDescendantRec( Vertex from, Vertex to, Set<Integer> analyzed){
		if(analyzed.contains(from.getId()))
			return false;
		else if(from.getId()==to.getId())
			return true;
		else{
			analyzed.add(from.getId());
			for(Arc arc : from.getArcs()){
				if(isDescendantRec( arc.getSecondEndPoint(), to, analyzed))
					return true;
			}
		}
		return false;
	}
}


