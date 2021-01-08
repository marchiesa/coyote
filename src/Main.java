import experiments.MainOptimalOblivious;
import experiments.racke.MainRacke;
import experiments.racke.MainRackeWithHeuristic;
import gurobi.GRBException;

import java.io.IOException;


public class Main {
	
	private final static int OPTIMAL_OBLIVIOUS=0;
	private final static int RACKE=1;
	private final static int RACKE_HEURISTIC=2;
	
	public static void main(String[] args) throws IOException, GRBException{
		int type = Integer.parseInt(args[0]);
		args[0]=args[1];
		if(type == OPTIMAL_OBLIVIOUS){
			MainOptimalOblivious.main(args);
		}else if(type == RACKE){
			MainRacke.main(args);
		}else if(type == RACKE_HEURISTIC){
			MainRackeWithHeuristic.main(args);
		} 
	}

}
