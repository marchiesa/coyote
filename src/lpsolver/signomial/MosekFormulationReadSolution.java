package lpsolver.signomial;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

public class MosekFormulationReadSolution {

	public void readSolution(String graphNameFile) {
		String path =graphNameFile+".sol";
		BufferedReader br = null;
		String pathFormulation =graphNameFile+".col";
		BufferedReader brF = null;

		try {
			PrintWriter writerSplittingRatio = new PrintWriter(graphNameFile+"-splitting-ratio-formulation-data.txt", "UTF-8");
			PrintWriter writerAlpha = new PrintWriter(graphNameFile+"-alpha.txt", "UTF-8");

			String sCurrentLine;
			String sCurrentLineF;

			br = new BufferedReader(new FileReader(path));
			brF = new BufferedReader(new FileReader(pathFormulation));

			int toBeSkipped=0;
			int toBeRead=0;
			int counter =0;
			boolean sawOptions = false;
			boolean reading= false;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split(" ");

				if(!sawOptions){
					if(sCurrentLine.contains("Options"))
						sawOptions = true;
				}else{
					counter++;
					if(counter == 5)
						toBeSkipped=Integer.parseInt(sCurrentLine);			
					else if(counter == 7)
						toBeRead = Integer.parseInt(sCurrentLine);
					else if(counter >= 9+ toBeSkipped){
						if(counter >= 9 + toBeSkipped + toBeRead )
							break;
						//read the variables
						if((sCurrentLineF = brF.readLine() )!= null){
							//System.out.println(sCurrentLineF + " --- " +sCurrentLine );
							if(sCurrentLineF.contains("phi")){
								sCurrentLineF= sCurrentLineF.replace("phi_", "");
								sCurrentLineF= sCurrentLineF.replace("_", " ");
								writerSplittingRatio.write(sCurrentLineF+ " " + sCurrentLine+"\n");
							}
							if(sCurrentLineF.contains("alpha")){
								writerAlpha.write(sCurrentLine+"\n");
							}
						}
					}
				}


			}
			writerSplittingRatio.flush();writerSplittingRatio.close();
			writerAlpha.flush();writerAlpha.close();
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}

	}

}
