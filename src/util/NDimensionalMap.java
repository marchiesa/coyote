package util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NDimensionalMap<V> {

	private Map<Integer,NDimensionalMap<V>> childrenMap;
	private NDimensionalMap<V> father;
	private V value;
	private int height;
	private int degree;
	private boolean isTop=false;
	private int index ;

	public NDimensionalMap(){
		this.childrenMap = new HashMap<Integer,NDimensionalMap<V>>();
		this.isTop = true;
	}
	
	public NDimensionalMap(boolean isTop){
		this();
		this.isTop = isTop;
	}

	public void initialize(int height, int degree){
		initializeRic(height,degree,null);
		this.index=0;
		this.height = height;
		this.degree = degree;
	}

	private void initializeRic(int height, int degree, NDimensionalMap<V> father) {
		this.setFather(father);
		if(height==0){
			NDimensionalMap<V> value = new NDimensionalMap<V>(false);
			value.setHeight(height);
			value.setValue(null);
		}
		else{
			int numberOfIndices = degree;
			if(isTop)
				numberOfIndices *= 2;
			for(int i=0;i<numberOfIndices;i++){
				NDimensionalMap<V> child = new NDimensionalMap<V>(false);
				child.initialize(height-1, degree);
				child.setIndex(i);
				this.childrenMap.put(i, child);
			}
		}
	}
	
	public List<V> getValues() {
		List<V> values = new LinkedList<V>();
		this.getValuesRic(this.height,this.degree, this, values);
		return values;
	}
	
	private void getValuesRic(int height, int degree, NDimensionalMap<V> position2id, List<V> values) {
		if(height==0){
			values.add(position2id.getValue());
		}
		else{
			int numberOfIndices = degree;
			if(position2id.isTop())
				numberOfIndices *= 2;
			for(int i=0;i<numberOfIndices;i++){
				getValuesRic(height-1, degree, position2id.get(i), values);
			}
		}
	}

	

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getDegree() {
		return degree;
	}

	public void setDegree(int degree) {
		this.degree = degree;
	}

	public boolean isTop() {
		return isTop;
	}

	public void setTop(boolean isTop) {
		this.isTop = isTop;
	}

	public NDimensionalMap<V> getFather() {
		return father;
	}

	public void setFather(NDimensionalMap<V> father) {
		this.father = father;
	}

	public Map<Integer, NDimensionalMap<V>> getChildrenMap() {
		return childrenMap;
	}

	public void setChildrenMap(Map<Integer, NDimensionalMap<V>> childrenMap) {
		this.childrenMap = childrenMap;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public NDimensionalMap<V> get(Integer key){
		return this.childrenMap.get(key);
	}

	public String toString(){
		String result = "";
		if(height>0){
			int numberOfIndices = degree;
			if(isTop)
				numberOfIndices *= 2;
			for(int j=0;j<numberOfIndices;j++){
				for(int i=6;i>height;i--){
					result+="\t";
				}
				result+=j+":";
				if(height!=1)
					result+="\n";
				result+=this.childrenMap.get(j).toString();
			}
		}else{
			result+=this.value+"\n";
		}
		return result;
	}
}
