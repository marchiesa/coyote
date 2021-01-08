package util;

import java.io.Serializable;

/**
 * 
 * @author mosfet
 *
 * @param <K>
 */
public class Pair<K> implements Comparable<Pair<K>>, Serializable{
	
	private K first;
	private K second;
	
	public Pair(K first, K second){
		this.first=first;
		this.second = second;
	}
	
	public K getFirst() {
		return first;
	}
	public void setFirst(K first) {
		this.first = first;
	}
	public K getSecond() {
		return second;
	}
	public void setSecond(K second) {
		this.second = second;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		Pair<K> p=(Pair<K>)o;
		return this.first.equals(p.getFirst()) && this.second.equals(p.getSecond());
	}
	
	public int hashCode(){
		return this.first.hashCode() + this.second.hashCode();
	}
	
	public String toString(){
		return "("+this.getFirst() + "," + this.getSecond()+ ")";
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Pair<K> o) {
		Comparable<K> thisFirst = (Comparable<K>)this.getFirst();
		Comparable<K> thisSecond= (Comparable<K>)this.getSecond();
		
		if(thisFirst.compareTo(o.getFirst())<0)
			return -1;
		else if(thisFirst.compareTo(o.getFirst())>0)
			return 1;
		else{
			if(thisSecond.compareTo(o.getSecond())<0)
				return -1;
			else if(thisSecond.compareTo(o.getSecond())>0)
				return 1;
			else
				return 0;
		}
	}

}
