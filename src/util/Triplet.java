package util;

/**
 * 
 * @author mosfet
 *
 * @param <K>
 */
public class Triplet<K,J> implements Comparable<Triplet<K,J>>{
	
	private K first;
	private K second;
	private J third;
	
	public Triplet(K first, K second, J third){
		this.first=first;
		this.second = second;
		this.third= third;
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
	
	public J getThird() {
		return third;
	}

	public void setThird(J third) {
		this.third = third;
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		Triplet<K,J> p=(Triplet<K,J>)o;
		return this.first.equals(p.getFirst()) && this.second.equals(p.getSecond()) && this.third.equals(p.getThird());
	}
	
	public int hashCode(){
		return this.first.hashCode() + this.second.hashCode() + this.third.hashCode();
	}
	
	public String toString(){
		return "("+this.getFirst() + "," + this.getSecond()+ ")";
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Triplet<K,J> o) {
		Comparable<K> thisFirst = (Comparable<K>)this.getFirst();
		Comparable<K> thisSecond= (Comparable<K>)this.getSecond();
		Comparable<J> thisThird= (Comparable<J>)this.getThird();
		
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
				if(thisThird.compareTo(o.getThird())<0)
					return -1;
				else if(thisThird.compareTo(o.getThird())>0)
					return 1;
				else
					return 0;
		}
	}

}
