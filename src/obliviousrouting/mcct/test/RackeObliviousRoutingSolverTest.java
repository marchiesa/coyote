package obliviousrouting.mcct.test;

import model.Arc;
import model.Graph;
import model.Vertex;
import obliviousrouting.RackeObliviousRoutingSolver;
import obliviousrouting.mcct.MCCTDerandomizedWeightedSolver;

import org.junit.Test;

public class RackeObliviousRoutingSolverTest {

	/*@Test
	public void testSolver(){
		Graph g = new Graph();

		// vertices
		Vertex v1 = new Vertex();
		v1.setId(0);
		g.addVertex(v1);
		Vertex v2 = new Vertex();
		v2.setId(1);
		g.addVertex(v2);

		//edges
		Arc arc = new Arc();
		arc.setCapacity(5);
		arc.setDistance(2);
		arc.setFirstEndPoint(v1);
		arc.setSecondEndPoint(v2);
		arc.setId(0);

		g.addArc(arc);

		System.out.println(g);

		MCCTDerandomizedWeightedSolver mcct = new MCCTDerandomizedWeightedSolver();
		mcct.addDemand(v1, v2, 4.5);
		mcct.setGraph(g);
		Tree t = mcct.getBestTree();

		assertTrue(t.getRoot().getVertices().size()==1);

		assertTrue(t.getRoot().getVertices().size()==2);
		assertTrue(t.getRoot().getChildren().get(0).getVertices().size()==2);
		assertTrue(t.getRoot().getChildren().get(0).getChildren().get(0).getVertices().size()==1);
		assertTrue(t.getRoot().getChildren().get(0).getChildren().get(1).getVertices().size()==1);
	} 

	@Test
	public void testSolver2(){
		Graph g = new Graph();

		// vertices
		Vertex v0 = new Vertex();
		v0.setId(0);
		g.addVertex(v0);
		Vertex v1 = new Vertex();
		v1.setId(1);
		g.addVertex(v1);
		Vertex v2 = new Vertex();
		v2.setId(2);
		g.addVertex(v2);
		Vertex v3 = new Vertex();
		v3.setId(3);
		g.addVertex(v3);
		Vertex v4 = new Vertex();
		v4.setId(4);
		g.addVertex(v4);
		Vertex v5 = new Vertex();
		v5.setId(5);
		g.addVertex(v5);
		Vertex v6 = new Vertex();
		v6.setId(6);
		g.addVertex(v6);
		Vertex v7 = new Vertex();
		v7.setId(7);
		g.addVertex(v7);
		

		//edges
		Arc arc1 = new Arc();
		arc1.setCapacity(5);
		arc1.setDistance(2);
		arc1.setFirstEndPoint(v1);
		arc1.setSecondEndPoint(v2);
		arc1.setId(0);
		g.addArc(arc1);
		Arc arc2 = new Arc();
		arc2.setCapacity(5);
		arc2.setDistance(2);
		arc2.setFirstEndPoint(v2);
		arc2.setSecondEndPoint(v3);
		arc2.setId(1);
		g.addArc(arc2);
		Arc arc3 = new Arc();
		arc3.setCapacity(5);
		arc3.setDistance(2);
		arc3.setFirstEndPoint(v3);
		arc3.setSecondEndPoint(v4);
		arc3.setId(2);
		g.addArc(arc3);
		Arc arc4 = new Arc();
		arc4.setCapacity(5);
		arc4.setDistance(2);
		arc4.setFirstEndPoint(v4);
		arc4.setSecondEndPoint(v5);
		arc4.setId(3);
		g.addArc(arc4);
		Arc arc5 = new Arc();
		arc5.setCapacity(5);
		arc5.setDistance(2);
		arc5.setFirstEndPoint(v5);
		arc5.setSecondEndPoint(v6);
		arc5.setId(4);
		g.addArc(arc5);
		Arc arc6 = new Arc();
		arc6.setCapacity(5);
		arc6.setDistance(2);
		arc6.setFirstEndPoint(v6);
		arc6.setSecondEndPoint(v7);
		arc6.setId(5);
		g.addArc(arc6);
		Arc arc7 = new Arc();
		arc7.setCapacity(5);
		arc7.setDistance(2);
		arc7.setFirstEndPoint(v0);
		arc7.setSecondEndPoint(v1);
		arc7.setId(6);
		g.addArc(arc7);

		System.out.println(g);

		MCCTDerandomizedWeightedSolver mcct = new MCCTDerandomizedWeightedSolver();
		mcct.addDemand(v1, v2, 4.5);
		mcct.setGraph(g);
		Tree t = mcct.getBestTree();

		assertTrue(t.getRoot().getVertices().size()==1);
	} */



	@Test
	public void testSolverComputeBestBetaAndPermutation(){
		Graph g = new Graph();

		// vertices
		Vertex v0 = new Vertex();
		v0.setId(0);
		g.addVertex(v0);
		Vertex v1 = new Vertex();
		v1.setId(1);
		g.addVertex(v1);
		Vertex v2 = new Vertex();
		v2.setId(2);
		g.addVertex(v2);
		Vertex v3 = new Vertex();
		v3.setId(3);
		g.addVertex(v3);
		Vertex v4 = new Vertex();
		v4.setId(4);
		g.addVertex(v4);
		Vertex v5 = new Vertex();
		v5.setId(5);
		g.addVertex(v5);
		Vertex v6 = new Vertex();
		v6.setId(6);
		g.addVertex(v6);
		Vertex v7 = new Vertex();
		v7.setId(7);
		g.addVertex(v7);
		Vertex v8 = new Vertex();
		v8.setId(8);
		g.addVertex(v8);
		
		//edges
		Arc arc1 = new Arc();
		arc1.setCapacity(5);
		arc1.setFirstEndPoint(v1);
		arc1.setSecondEndPoint(v2);
		arc1.setId(0);
		g.addUndirectedArc(arc1);
		Arc arc2 = new Arc();
		arc2.setCapacity(5);
		arc2.setFirstEndPoint(v2);
		arc2.setSecondEndPoint(v3);
		arc2.setId(1);
		g.addUndirectedArc(arc2);
		Arc arc3 = new Arc();
		arc3.setCapacity(5);
		arc3.setFirstEndPoint(v3);
		arc3.setSecondEndPoint(v4);
		arc3.setId(2);
		g.addUndirectedArc(arc3);
		Arc arc4 = new Arc();
		arc4.setCapacity(5);
		arc4.setFirstEndPoint(v4);
		arc4.setSecondEndPoint(v5);
		arc4.setId(3);
		g.addUndirectedArc(arc4);
		Arc arc5 = new Arc();
		arc5.setCapacity(5);
		arc5.setFirstEndPoint(v5);
		arc5.setSecondEndPoint(v6);
		arc5.setId(4);
		g.addUndirectedArc(arc5);
		Arc arc6 = new Arc();
		arc6.setCapacity(5);
		arc6.setFirstEndPoint(v6);
		arc6.setSecondEndPoint(v7);
		arc6.setId(5);
		g.addUndirectedArc(arc6);
		Arc arc7 = new Arc();
		arc7.setCapacity(5);
		arc7.setFirstEndPoint(v0);
		arc7.setSecondEndPoint(v1);
		arc7.setId(6);
		g.addUndirectedArc(arc7);
		Arc arc8 = new Arc();
		arc8.setCapacity(5);
		arc8.setFirstEndPoint(v7);
		arc8.setSecondEndPoint(v8);
		arc8.setId(7);
		g.addUndirectedArc(arc8);
		Arc arc9 = new Arc();
		arc9.setCapacity(5);
		arc9.setFirstEndPoint(v8);
		arc9.setSecondEndPoint(v0);
		arc9.setId(8);
		g.addUndirectedArc(arc9);

		System.out.println(g);

		MCCTDerandomizedWeightedSolver mcct = new MCCTDerandomizedWeightedSolver();
		mcct.setGraph(g);
		/*mcct.addDemand(v1, v2, 4.5);
		mcct.setBestB(1.75d);
		List<Vertex> permutation = new LinkedList<Vertex>();
		permutation.add(v2);
		permutation.add(v7);
		permutation.add(v6);
		permutation.add(v5);
		permutation.add(v4);
		permutation.add(v3);
		permutation.add(v1);
		permutation.add(v0);
		mcct.setVerticesPermutation(permutation);
		mcct.computeBestTree();
		
		Tree t = mcct.getTree();*/
		
		RackeObliviousRoutingSolver rors = new RackeObliviousRoutingSolver();
		//MCCTSolverMock mcctMock = new MCCTSolverMock();
		//mcctMock.setTree(t);
		rors.setMcct(mcct);
		
		rors.setGraph(g);
		//rors.iterate(0, 0);
		//rors.iterate(1, 1/3d);
		rors.createTreesAndLambda();
		//assertTrue(t.getRoot().getVertices().size()==g.getVertices().size());
	}
}
