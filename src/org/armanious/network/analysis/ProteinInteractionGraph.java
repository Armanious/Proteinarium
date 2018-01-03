package org.armanious.network.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.armanious.graph.Graph;

public class ProteinInteractionGraph extends Graph<Protein> {
	
	public ProteinInteractionGraph(double threshold, String interactomeFile, Map<String, Protein> proteinMap) throws IOException {
		InputStream is = new FileInputStream(interactomeFile);
		if(interactomeFile.endsWith(".gz"))
			is = new GZIPInputStream(is);
		load(threshold, new BufferedReader(new InputStreamReader(is)), proteinMap);
	}

	private void load(double threshold, BufferedReader in, Map<String, Protein> proteinMap) throws IOException {
		System.out.println("Loading protein interaction graph...");
		String s;
		while((s = in.readLine()) != null){
			final String[] parts = s.split(" ");
			if(parts[0].length() == 0 || parts[0].charAt(0) != '9') //only 9606 i.e. humans
				continue;
			final int weight = Integer.parseInt(parts[2]);
			if(weight >= threshold){
				final Protein a = proteinMap.get(parts[0]); //Protein.getProtein(parts[0], true);
				final Protein b = proteinMap.get(parts[1]); //Protein.getProtein(parts[1], true);
				if(a != null && b != null)
					addEdge(a, b, weight);
			}
		}
		in.close();
		System.out.println("Loaded protein interaction graph");
	}

}
