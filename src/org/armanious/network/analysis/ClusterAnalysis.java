package org.armanious.network.analysis;

import java.text.DecimalFormat;
import java.util.Arrays;

public class ClusterAnalysis {
	
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat();
	private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat();
	static {
		DECIMAL_FORMAT.setMinimumFractionDigits(6);
		DECIMAL_FORMAT.setMaximumFractionDigits(6);
		DECIMAL_FORMAT.setMinimumIntegerDigits(1);
		
		PERCENTAGE_FORMAT.setMinimumFractionDigits(4);
		PERCENTAGE_FORMAT.setMaximumFractionDigits(4);
		PERCENTAGE_FORMAT.setMultiplier(100);
	}
	
	private final String clusterId;
	private final PhylogeneticTreeNode node;
	private final double normalizedHeight;
	private final int numberGroup1;
	private final int numberGroup2;
	private final double weightGroup1;
	private final double weightGroup2;
	private final double maxDissimilarity;
	private final double pValue;
	private final int numDigits;
	private final String patientsString;
	
	public ClusterAnalysis(String clusterId, PhylogeneticTreeNode cluster, DistanceMatrix<PhylogeneticTreeNode> distances, GeneSetMap group1, GeneSetMap group2, FisherExact fe, double maxHeight){
		final PhylogeneticTreeNode[] nodeLeafs = cluster.getLeaves();
		
		double maxDissimilarity = 0;
		double weightGroup1 = 0;
		int numberGroup1 = 0;
		for(int i = 0; i < nodeLeafs.length - 1; i++){
			if(group1.getGeneSetMap().containsKey(nodeLeafs[i].getLabel())){
				weightGroup1 += nodeLeafs[i].getWeight();
				numberGroup1++;
			}
			for(int j = i + 1; j < nodeLeafs.length; j++){
				final double distance = distances.getDistance(nodeLeafs[i], nodeLeafs[j]);
				if(distance > maxDissimilarity)
					maxDissimilarity = distance;
			}
		}
		
		this.clusterId = clusterId;
		this.node = cluster;
		this.normalizedHeight = cluster.getHeight() / maxHeight;
		this.numberGroup1 = numberGroup1;
		this.numberGroup2 = nodeLeafs.length - numberGroup1;
		this.weightGroup1 = weightGroup1;
		this.weightGroup2 = cluster.getWeight() - weightGroup1;
		this.maxDissimilarity = maxDissimilarity;
		
		int totalGroup1 = group1.getGeneSetMap().size();
		int totalGroup2 = group2.getGeneSetMap().size();
		
		//System.out.println(clusterId + " FE Test: " + numberGroup1 + ", " + (totalGroup1 - numberGroup1) + ", "  
		//		+ numberGroup2 + ", " + (totalGroup2 - numberGroup2));
		
		this.pValue = fe.getTwoTailedP(
				numberGroup1, group1.getGeneSetMap().size() - numberGroup1,
				numberGroup2, group2.getGeneSetMap().size() - numberGroup2);
		
		this.numDigits = (int) Math.ceil(Math.log10(totalGroup1 + totalGroup2));
		
		final StringBuilder patientsString = new StringBuilder();
		final String[] patientLabels = new String[nodeLeafs.length];
		for(int i = 0; i < patientLabels.length; i++)
			patientLabels[i] = nodeLeafs[i].getLabel();
		Arrays.sort(patientLabels);
		for(String label : patientLabels)
			patientsString.append(label).append(", ");
		this.patientsString = patientsString.substring(0, patientsString.length() - 2);
		
		
	}
	
	private static String pad(Object o, int numChars){
		final StringBuilder sb = new StringBuilder(String.valueOf(o));
		if(sb.length() > numChars){
			System.err.println(sb.toString());
			System.err.println(numChars);
		}
		assert(sb.length() <= numChars);
		if(sb.length() == numChars) return sb.toString();
		final char[] padding = new char[numChars - sb.length()];
		Arrays.fill(padding, ' ');
		return sb.insert(0, padding).toString();
	}
	
	public String getClusterId(){
		return clusterId;
	}
	
	public String getPrintableString(){
		final StringBuilder sb = new StringBuilder();
		sb.append("Cluster analysis of ").append(clusterId).append("\n\t")
		.append("Normalized height = ").append(DECIMAL_FORMAT.format(normalizedHeight)).append("\n\t")
		.append("Number in Group 1 = ").append(numberGroup1)
		.append(" (").append(DECIMAL_FORMAT.format(weightGroup1 * 100D / (weightGroup1 + weightGroup2))).append("%)\n\t")
		.append("Number in Group 2 = ").append(numberGroup2)
			.append(" (").append(DECIMAL_FORMAT.format(weightGroup2 * 100D / (weightGroup1 + weightGroup2))).append("%)\n\t")
		.append("Max dissimilarity = ").append(DECIMAL_FORMAT.format(maxDissimilarity)).append("\n\t")
		.append("p-value = ").append(DECIMAL_FORMAT.format(pValue)).append("\n\t")
		.append("Patients = ").append(patientsString);
		return sb.toString();
	}
	
	public String getCompactString(){
		return new StringBuilder()
				.append(pad(clusterId, numDigits + 1)).append('\t')
				.append(DECIMAL_FORMAT.format(normalizedHeight)).append('\t')
				.append(pad(numberGroup1, numDigits)).append('\t')
				.append(pad(numberGroup2, numDigits)).append('\t')
				.append(pad(PERCENTAGE_FORMAT.format(weightGroup1 / (weightGroup1 + weightGroup2)), 9)).append("%\t")
				.append(pad(PERCENTAGE_FORMAT.format(weightGroup2 / (weightGroup1 + weightGroup2)), 9)).append("%\t")
				.append(DECIMAL_FORMAT.format(maxDissimilarity)).append('\t')
				.append(DECIMAL_FORMAT.format(pValue)).append('\t')
				.append(patientsString)//.append('\t')
				.toString();
	}
	
	public static String getCopmactStringHeaders(){
		return "Cluster Id\tNormalized Height\tNumber in Group 1\tNumber in Group 2\tGroup 1 Weight Percentage\tGroup 2 Weight Percentage\tMax Dissimilarity\tp-value"
				.replace("\t", " | ");
	}

	public PhylogeneticTreeNode getNode() {
		return node;
	}

	public double getpValue() {
		return pValue;
	}

	public double getNormalizedHeight() {
		return normalizedHeight;
	}
	
	/*
	 *   
Cluster Id       Num Group 1 Weight Group 1     Max Dissimilarity
         Height      Num Group 2         Weight Group 2         p-value 
  C1	0.376525	 96	 47	  50.0000%	  50.0000%	0.905882	1.000000	
  C2	0.369184	 95	 46	  50.2759%	  49.7241%	0.905882	0.444401	
  
  
  
  
Cluster Id            Num Group 2                Max Dissimilarity
         Height            Weight Group 1                       p-value
                Num Group 1             Weight Group 3
  C1	0.376525	 96	 47	  50.0000%	  50.0000%	0.905882	1.000000	
  C2	0.369184	 95	 46	  50.2759%	  49.7241%	0.905882	0.444401	
	 */

}
