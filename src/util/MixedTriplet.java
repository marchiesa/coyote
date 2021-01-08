package util;

/**
 * A comparable triplet. 
 * @author mosfet
 *
 * @param <K>
 * @param <J>
 * @param <L>
 */
public class MixedTriplet<K,J,L> implements Comparable<MixedTriplet<K,J,L>>{

	private Comparable<K> first;
	private Comparable<J> second;
	private Comparable<L> third;

	public MixedTriplet(Comparable<K> first, Comparable<J> second, Comparable<L> third){
		this.first=first;
		this.second = second;
		this.third= third;
	}

	public Comparable<K> getFirst() {
		return first;
	}
	public void setFirst(Comparable<K> first) {
		this.first = first;
	}
	public Comparable<J> getSecond() {
		return second;
	}
	public void setSecond(Comparable<J> second) {
		this.second = second;
	}
	public Comparable<L> getThird() {
		return third;
	}
	public void setThird(Comparable<L> third) {
		this.third= third;
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		MixedTriplet<K,J,L> p=(MixedTriplet<K,J,L>)o;
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
	public int compareTo(MixedTriplet<K, J,L> o) {

		if(this.getFirst().compareTo((K) o.getFirst())<0)
			return -1;
		else if(this.getFirst().compareTo((K) o.getFirst())>0)
			return 1;
		else{
			if(this.getSecond().compareTo((J) o.getSecond())<0)
				return -1;
			else if(this.getSecond().compareTo((J) o.getSecond())>0)
				return 1;
			else{
				if(this.getThird().compareTo((L) o.getThird())<0)
					return -1;
				else if(this.getThird().compareTo((L) o.getThird())>0)
					return 1;
				else
					return 0;
			}
		}
	}

}
