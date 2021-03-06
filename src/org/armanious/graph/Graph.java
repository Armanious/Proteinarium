package org.armanious.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.armanious.network.analysis.Pathfinder;

public class Graph<K extends Comparable<K>> implements Pathfinder<K> {
		
	protected final HashMap<K, HashSet<Edge<K>>> neighbors = new HashMap<>();
	protected final HashMap<K, HashMap<K, Path<K>>> cachedPaths = new HashMap<>();
	protected final double maxPathCost;
	protected final int maxPathLength;
	
	public Graph(double maxPathCost, int maxPathLength) {
		this.maxPathCost = maxPathCost;
		this.maxPathLength = maxPathLength;
	}
	
	public void addEdge(Edge<K> edge){
		if(!neighbors.containsKey(edge.getSource())) neighbors.put(edge.getSource(), new HashSet<>());
		neighbors.get(edge.getSource()).add(edge);
		
		// edges are added directionally, but we keep track of vertices in the graph by neighbors.keySet(),
		// so just have an empty set of neighbors for "sink" vertices
		if(!neighbors.containsKey(edge.getTarget())) neighbors.put(edge.getTarget(), new HashSet<>());
	}
	
	public Collection<K> getVertices(){
		return neighbors.keySet();
	}
	
	public Collection<Edge<K>> getNeighbors(K n){
		assert(neighbors.containsKey(n));
		return neighbors.get(n);
	}

	public void removeVertex(K k) {
		neighbors.remove(k);
		for(HashSet<Edge<K>> edges : neighbors.values()){
			final Iterator<Edge<K>> iter = edges.iterator();
			while(iter.hasNext()){
				if(iter.next().getTarget().equals(k)){
					iter.remove();
				}
			}
		}
		cachedPaths.remove(k);
		for(HashMap<K, Path<K>> pathsByDest : cachedPaths.values()) pathsByDest.remove(k);
	}
	
	public void clear(){
		neighbors.clear();
		cachedPaths.clear();
	}
	
	public final void addEdge(K src, K target){
		addEdge(src, target, 1);
	}
	
	public final void addEdge(K src, K target, int weight){
		addEdge(src, target, weight, true);
	}
	
	public final void addEdge(K src, K target, int weight, boolean bidirectional){
		if(src == null || target == null)
			throw new IllegalArgumentException("Both source and target vertices of an edge must be non-null");
		if(src == target || src.equals(target))
			throw new IllegalArgumentException("The source and target vertices cannot be the same");
		addEdge(new Edge<>(src, target, weight));
		if(bidirectional) addEdge(new Edge<>(target, src, weight));
	}

	public final Path<K> dijkstras(K source, K target){
		return dijkstras(source, target, e -> (double) e.getWeight());
	}

	public final Path<K> dijkstras(K source, K target, Function<Edge<K>, Double> cost){
		return dijkstras(source, target, cost, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
	
	public final Path<K> dijkstras(K source, K target, Function<Edge<K>, Double> cost, double maxPathCost, int maxPathLength){
		if(!neighbors.containsKey(source) || !neighbors.containsKey(target))
			return new Path<>();
		
		final HashMap<K, Double> distances = new HashMap<>();
		final HashMap<K, Integer> lengths = new HashMap<>();
		final HashMap<K, Edge<K>> prev = new HashMap<>();

		distances.put(source, 0d);
		lengths.put(source, 1);
		prev.put(source, null);

		final PriorityQueue<K> queue = new PriorityQueue<>(Comparator.comparing(k -> distances.get(k)));
		queue.add(source);

		while(!queue.isEmpty()){
			final K cur = queue.poll();
			final double currentCost = distances.get(cur);
			final int currentLength = lengths.get(cur);
			if(currentLength == maxPathLength) continue;
			if(getNeighbors(cur) == null){
				System.out.println("getNeighbors() for " + cur + " returns null!! :(");
			}
			for(Edge<K> edge : getNeighbors(cur)){
				final K next = edge.getTarget();
				final double edgeCost = cost.apply(edge);
				if(currentCost + edgeCost < distances.getOrDefault(next, Double.MAX_VALUE)
						&& currentCost + edgeCost <= maxPathCost){
					distances.put(next, currentCost + edgeCost);
					lengths.put(next, currentLength + 1);
					prev.put(next, edge);
					queue.remove(next);
					queue.add(next);
				}
			}
		}
		
		ArrayList<Edge<K>> path = new ArrayList<>();
		Edge<K> cur = prev.get(target);
		
		while(cur != null){
			path.add(cur);
			cur = prev.get(cur.getSource());
		}
		Collections.reverse(path);
		return new Path<>(path);
	}
	
	public Graph<K> subgraphWithEdges(Collection<Edge<K>> edges){
		return subgraphWithEdges(new Graph<>(maxPathCost, maxPathLength), edges);
	}

	public <G extends Graph<K>> G subgraphWithEdges(G g, Collection<Edge<K>> edges) {
		for(Edge<K> e : edges)
			g.addEdge(e);
		return g;
	}
	
	int getReductionMetric(K k) {
		final HashSet<Edge<K>> neighbors = this.neighbors.get(k);
		return neighbors == null ? 0 : neighbors.size();  // by the degree of the protein
		// can override in LayeredGraph so that it is the number of patients the protein is found in
	}
	
	Graph<K> emptyGraph() {
		return new Graph<>(maxPathCost, maxPathLength);
	}
	
	public Graph<K> reduceByPaths(Collection<K> endpoints, int maxVertices) {
		return reduceByPaths(endpoints, maxVertices, true);
	}
	
	public Graph<K> reduceByPaths(Collection<K> endpoints, int maxVertices, boolean bidirectional) {
		final Graph<K> g = emptyGraph();
		if(endpoints.size() < 2) return g;
		
		final ArrayList<K> validEndpoints = new ArrayList<>(endpoints.size());
		for(K k : endpoints)
			if(neighbors.containsKey(k))
				validEndpoints.add(k);
		if(validEndpoints.size() < 2) return g;
		
		validEndpoints.sort(Comparator.comparingInt(t -> -getReductionMetric(t)));
		
		HashSet<K> containedVertices = new HashSet<>();
		containedVertices.add(validEndpoints.get(0));
		
		for(int nextEndpointToAdd = 1;
				nextEndpointToAdd < validEndpoints.size() && containedVertices.size() < maxVertices;
				nextEndpointToAdd++) {
			final LinkedList<Path<K>> pathsToAdd = new LinkedList<>();
			final HashSet<K> newVertices = new HashSet<>();
			for(int prevEndpoint = 0; prevEndpoint < nextEndpointToAdd; prevEndpoint++) {
				final Path<K> pathToAdd = findPath(validEndpoints.get(prevEndpoint), validEndpoints.get(nextEndpointToAdd));
				pathsToAdd.add(pathToAdd);
				for(Edge<K> edge : pathToAdd.getEdges()) {
					newVertices.add(edge.getSource());
					newVertices.add(edge.getTarget());
				}
			}
			if(containedVertices.size() + newVertices.size() > maxVertices) break;
			for(Path<K> pathToAdd : pathsToAdd) {
				for(Edge<K> edge : pathToAdd.getEdges()) {
					g.addEdge(edge);
					if(bidirectional) g.addEdge(edge.getTarget(), edge.getSource(), edge.getWeight());
				}
			}
			containedVertices.addAll(newVertices);
		}
		return g;
	}

	public double getLocalClusteringCoefficient(K vertex){
		// implementation of
		// https://en.wikipedia.org/wiki/Clustering_coefficient#Network_average_clustering_coefficient
		final Set<K> neighbors = getNeighbors(vertex).stream().map(edge -> edge.getTarget()).collect(Collectors.toSet());
		if(neighbors.size() <= 1) return 0;
		
		double triangles = 0;
		
		for(K neighbor : neighbors)
			for(Edge<K> neighborOfNeighbor : getNeighbors(neighbor))
				if(neighbors.contains(neighborOfNeighbor.getTarget()))
					triangles++;
		
		return triangles / (neighbors.size() * (neighbors.size() - 1));
	}
	
	public double getGlobalClusteringCoefficient(){
		// implementation of
		// https://en.wikipedia.org/wiki/Clustering_coefficient#Network_average_clustering_coefficient
		final Collection<K> vertices = getVertices();
		if(vertices.size() == 0) return 0;
		double sum = 0;
		for(K vertex : vertices) sum += getLocalClusteringCoefficient(vertex);
		return sum / vertices.size();
	}
	
	@Override
	public Path<K> findPath(K src, K dst) {
		if(src.compareTo(dst) > 0){
			K tmp = src;
			src = dst;
			dst = tmp;
		}
		assert(src.compareTo(dst) <= 0);
		
		Path<K> path = cachedPaths.containsKey(src) ? cachedPaths.get(src).get(dst) : null;
		if(path == null){
			// TODO don't have the 1000 - weight trick hard-coded in Graph...
			path = dijkstras(src, dst, e -> 1000D - e.getWeight(), maxPathCost, maxPathLength);
			
			HashMap<K, Path<K>> innerMap = cachedPaths.get(src);
			if(innerMap == null) cachedPaths.put(src, innerMap = new HashMap<>());
			innerMap.put(dst, path);
		}
		return path;
	}

}
