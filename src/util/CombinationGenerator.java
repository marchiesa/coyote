package util;


public class CombinationGenerator {

	private int[] elements;
	private int n;
	private int k;
	private int pointerIndex;
	private boolean hasNext;
	private boolean first;

	public CombinationGenerator(int n, int k){
		this.n=n;
		this.k=k;
		this.pointerIndex=k-1;
		this.elements = new int[k];
		this.hasNext=true;
		this.first=true;
		for(int i=0;i<k;i++)
			this.elements[i]=i;
	}

	public int[] next(){
		if(this.first){
			this.first=false;
		}else{
			int i=0;
			// find first elements that can be increased by 1
			while(this.elements[pointerIndex-i]==n-i-1){
				i++;
			}
			pointerIndex-=i;
			this.elements[pointerIndex]++;
			for(i=pointerIndex+1;i<k;i++)
				this.elements[i]=this.elements[pointerIndex]+(i-pointerIndex);
			if(pointerIndex==0 && this.elements[k-1]==n-1)
				this.hasNext = false;
			pointerIndex=k-1;
		}
		return this.elements;
	}

	public boolean hasNext(){
		return this.hasNext;
	}

}
