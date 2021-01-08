package util;

import java.util.Set;
import java.util.TreeSet;

public class Tuple<X, Y> implements Comparable<Tuple<X,Y>> {

	public final Comparable<X> x; 
	public final Y y; 
	public Tuple(Comparable<X> x, Y y) { 
		this.x = x; 
		this.y = y; 
	}
	public Comparable<X> getX() {
		return x;
	}
	public Y getY() {
		return y;
	}

	@Override
	public int compareTo(Tuple<X, Y> o) {
		return this.getX().compareTo((X)o.getX());
	}

} 
