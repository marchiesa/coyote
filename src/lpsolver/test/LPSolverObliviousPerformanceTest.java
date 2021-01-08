package lpsolver.test;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import lpsolver.CompactLPSolverObliviousPerformance;
import model.Arc;
import model.Graph;
import model.Vertex;

import org.junit.Test;

import util.Pair;

public class LPSolverObliviousPerformanceTest {

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
		g.addUndirectedArc(arc1);
		Arc arc2 = new Arc();
		arc2.setCapacity(5);
		arc2.setFirstEndPoint(v2);
		arc2.setSecondEndPoint(v3);
		g.addUndirectedArc(arc2);
		Arc arc3 = new Arc();
		arc3.setCapacity(5);
		arc3.setFirstEndPoint(v3);
		arc3.setSecondEndPoint(v4);
		g.addUndirectedArc(arc3);
		Arc arc4 = new Arc();
		arc4.setCapacity(5);
		arc4.setFirstEndPoint(v4);
		arc4.setSecondEndPoint(v5);
		g.addUndirectedArc(arc4);
		Arc arc5 = new Arc();
		arc5.setCapacity(5);
		arc5.setFirstEndPoint(v5);
		arc5.setSecondEndPoint(v6);
		g.addUndirectedArc(arc5);
		Arc arc6 = new Arc();
		arc6.setCapacity(5);
		arc6.setFirstEndPoint(v6);
		arc6.setSecondEndPoint(v7);
		g.addUndirectedArc(arc6);
		Arc arc7 = new Arc();
		arc7.setCapacity(5);
		arc7.setFirstEndPoint(v0);
		arc7.setSecondEndPoint(v1);
		g.addUndirectedArc(arc7);
		Arc arc8 = new Arc();
		arc8.setCapacity(5);
		arc8.setFirstEndPoint(v7);
		arc8.setSecondEndPoint(v8);
		g.addUndirectedArc(arc8);
		Arc arc9 = new Arc();
		arc9.setCapacity(5);
		arc9.setFirstEndPoint(v8);
		arc9.setSecondEndPoint(v0);
		g.addUndirectedArc(arc9);

		System.out.println(g);

		Map<Integer,Map<Pair<Integer>,Double>> arc2demand2fraction = new HashMap<Integer,Map<Pair<Integer>,Double>>();
		Map<Pair<Integer>,Double> demand2fraction = new HashMap<Pair<Integer>,Double>();
		arc2demand2fraction.put(arc2.getId(), demand2fraction);
		demand2fraction.put(new Pair<Integer>(2,3), 1d);
		
		CompactLPSolverObliviousPerformance lpSolver = new CompactLPSolverObliviousPerformance();
		
		lpSolver.computeObliviousPerformance(g, arc2demand2fraction);
		assertTrue(lpSolver.getOpt()==2);

	}
}
