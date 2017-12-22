package org.armanious.network.analysis;

import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.armanious.Tuple;

public class DistanceMatrix<T extends Comparable<T>> {

	private final TreeMap<T, TreeMap<T, Double>> matrix = new TreeMap<>();

	public DistanceMatrix(){}
	
	private DistanceMatrix(TreeMap<T, TreeMap<T, Double>> matrix){
		this.matrix.putAll(matrix);
	}

	public void setDistance(T a, T b, double d){
		if(!Double.isFinite(d))
			throw new IllegalArgumentException("Only finite distances allowed");
		
		final T lower;
		final T upper;
		if(a.compareTo(b) < 0){
			lower = a;
			upper = b;
		}else{
			lower = b;
			upper = a;
		}

		TreeMap<T, Double> r = matrix.get(lower);
		if(r == null){
			r = new TreeMap<>();
			matrix.put(lower, r);
		}
		r.put(upper, d);
	}

	public boolean isEmpty(){
		return matrix.isEmpty();
	}

	public double getDistance(T a, T b){
		final T lower;
		final T upper;
		if(a.compareTo(b) < 0){
			lower = a;
			upper = b;
		}else{
			lower = b;
			upper = a;
		}
		return matrix.get(lower).get(upper);
	}
	
	public Tuple<T, T> getMinimumDistanceEntry() {
		T minA = null;
		T minB = null;
		double minD = Double.MAX_VALUE;
		for(T a : matrix.keySet()){
			final TreeMap<T, Double> r = matrix.get(a);
			for(T b : r.keySet()){
				final double d = r.get(b);
				if(d < minD){
					minA = a;
					minB = b;
					minD = d;
				}
			}
		}
		if(!(minA != null && minB != null))
			System.err.println("DEBUG");
		return new Tuple<>(minA, minB);
	}

	public void removeAllAssociated(T t){
		matrix.remove(t);
		for(T a : matrix.keySet()){
			matrix.get(a).remove(t);
		}
	}

	public Set<T> getAllEntities(){
		if(matrix.size() > 0){
			final TreeSet<T> t = new TreeSet<>(matrix.keySet());
			t.add(matrix.firstEntry().getValue().lastKey());
			return t;
		}else{
			return Collections.emptySet();
		}
	}

	public String toString(){
		final StringBuilder sb = new StringBuilder("DistanceMatrix\n");
		for(T r : matrix.keySet()){
			final TreeMap<T, Double> row = matrix.get(r);
			for(T c : row.keySet()){
				sb.append('\t').append(r).append('-').append(c).append(": ").append(row.get(c)).append('\n');
			}
		}
		return sb.substring(0, sb.length() - 1);
	}
	
	public DistanceMatrix<T> clone(){
		return new DistanceMatrix<>(matrix);
	}

}
