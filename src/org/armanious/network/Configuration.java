package org.armanious.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Configuration {

	public static final class GeneralConfig {

		public final String activeDirectory;
		public final String imageDirectory;

		public final String primaryGeneSetGroupFile;
		public final String secondaryGeneSetGroupFile;
		public final String projectName;

		public final String proteinInteractomeFile;
		public final String proteinAliasesFile;

		public final boolean verboseOutput;

		// public final boolean multiThreaded = false;

		private static void downloadURLToFile(String urlPath, String file) throws IOException {
			System.out.println("Downloading " + urlPath + " to file " + file);
			final URL url = new URL(urlPath);
			final URLConnection c = url.openConnection();
			c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
			
			final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			final BufferedInputStream bis = new BufferedInputStream(c.getInputStream());
			final byte[] buffer = new byte[4096];
			int read;
			while((read = bis.read(buffer, 0, buffer.length)) != -1){
				bos.write(buffer, 0, read);
			}
			bis.close();
			bos.close();
		}

		public GeneralConfig(Map<String, String> map){
			String activeDirectory = map.getOrDefault("activeDirectory", "");
			if(!activeDirectory.isEmpty() && !activeDirectory.endsWith(File.separator))
				activeDirectory += File.separator;
			this.activeDirectory = activeDirectory;

			String imageDirectory = map.getOrDefault("imageDirectory", activeDirectory + "images");
			if(!imageDirectory.isEmpty() && !imageDirectory.endsWith(File.separator))
				imageDirectory += File.separator;
			if(!new File(imageDirectory).isAbsolute())
				imageDirectory = activeDirectory + imageDirectory;
			this.imageDirectory = imageDirectory;
			
			
			String primaryGeneSetGroupFile = map.get("primaryGeneSetGroupFile");
			if(primaryGeneSetGroupFile != null)
				if(!new File(primaryGeneSetGroupFile).isAbsolute())
					primaryGeneSetGroupFile = activeDirectory + primaryGeneSetGroupFile;
			this.primaryGeneSetGroupFile = primaryGeneSetGroupFile;
			
			String secondaryGeneSetGroupFile = map.get("secondaryGeneSetGroupFile");
			if(secondaryGeneSetGroupFile != null)
				if(!new File(secondaryGeneSetGroupFile).isAbsolute())
					secondaryGeneSetGroupFile = activeDirectory + secondaryGeneSetGroupFile;
			this.secondaryGeneSetGroupFile = secondaryGeneSetGroupFile;
			
			
			String projectName = map.get("projectName");
			if(projectName == null){
				if(!activeDirectory.isEmpty()){
					projectName = new File(activeDirectory).getName();
				}else if(primaryGeneSetGroupFile != null){
					projectName = primaryGeneSetGroupFile.contains(".") ? primaryGeneSetGroupFile.substring(0, primaryGeneSetGroupFile.indexOf('.')) : primaryGeneSetGroupFile;
				}
			}
			this.projectName = projectName;

			String version = "10.5";
			try{
				final Pattern p = Pattern.compile(">\\s*(\\d+\\.\\d+)\\s*<");
				final URL url = new URL("https://string-db.org");
				try(final BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))){
					String s;
					while((s = br.readLine()) != null){
						if(s.toLowerCase().contains("version")){
							s += br.readLine();
							final Matcher m = p.matcher(s);
							if(m.find())
								version = m.group(1);
							break;
						}
					}
				}
			}catch(IOException ignored){}

			proteinInteractomeFile = map.getOrDefault("proteinInteractomeFile",
					new File(System.getProperty("user.dir"), "9606.protein.links.v" + version + ".txt.gz").getPath());
			proteinAliasesFile = map.getOrDefault("proteinAliasesFile",
					new File(System.getProperty("user.dir"), "9606.protein.aliases.v" + version + ".txt.gz").getPath());
			try {
				if(!new File(proteinInteractomeFile).exists())
					downloadURLToFile("https://stringdb-static.org/download/protein.links.v" + version + "/9606.protein.links.v" + version + ".txt.gz", proteinInteractomeFile);
									  //"https://stringdb-static.org/download/protein.aliases.v10.5/9606.protein.aliases.v10.5.txt.gz"
				if(!new File(proteinAliasesFile).exists())
					downloadURLToFile("https://stringdb-static.org/download/protein.aliases.v10.5/9606.protein.aliases.v10.5.txt.gz", proteinAliasesFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			verboseOutput = Boolean.parseBoolean(map.getOrDefault("verboseOutput", "false"));

			//multiThreaded = Boolean.parseBoolean(map.getOrDefault("multiThreaded", "false"));
		}

	}

	public static final class AnalysisConfig {

		public final boolean reusePreviousData;
		public final boolean calculateGraphDifferences;

		public final double minInteractomeConfidence;

		public final double maxPathCost;
		public final int maxPathLength;

		//public final boolean layoutAndRender;
		public final double fractionOfNodesToRender;
		public final int maxNodesToRender;

		public AnalysisConfig(Map<String, String> map){
			reusePreviousData = Boolean.parseBoolean(map.getOrDefault("reusePreviousData", "true"));
			calculateGraphDifferences = Boolean.parseBoolean(map.getOrDefault("calculateGraphDifferences", "true"));

			minInteractomeConfidence = Double.parseDouble(map.getOrDefault("minInteractomeConfidence", "0"));

			maxPathCost = Double.parseDouble(map.getOrDefault("maxPathCost", "200"));
			maxPathLength = Integer.parseInt(map.getOrDefault("maxPathLength", "5"));

			//layoutAndRender = Boolean.parseBoolean(map.getOrDefault("layoutAndRender", "true"));
			fractionOfNodesToRender = Double.parseDouble(map.getOrDefault("fractionOfNodesToRender", "1"));
			maxNodesToRender = Integer.parseInt(map.getOrDefault("maxNodesToRender", String.valueOf(Integer.MAX_VALUE)));
		}

	}

	public static final class ForceDirectedLayoutConfig {

		public final double repulsionConstant;
		public final double attractionConstant;
		public final double deltaThreshold;

		public final long maxIterations;
		public final long maxTime;

		public final double minNodeRadius;
		public final double maxNodeRadius;

		public ForceDirectedLayoutConfig(Map<String, String> map){
			repulsionConstant = Double.parseDouble(map.getOrDefault("repulsionConstant", "0.5"));
			attractionConstant = Double.parseDouble(map.getOrDefault("attractionConstant", "0.001"));
			deltaThreshold = Double.parseDouble(map.getOrDefault("deltaThreshold", "0.001"));
			maxIterations = Long.parseLong(map.getOrDefault("maxIterations", "10000"));
			maxTime = Long.parseLong(map.getOrDefault("maxTime", String.valueOf(Long.MAX_VALUE)));

			minNodeRadius = Double.parseDouble(map.getOrDefault("minNodeRadius", "15"));
			maxNodeRadius = Double.parseDouble(map.getOrDefault("maxNodeRadius", "-1"));
		}

	}

	public static final class RendererConfig {

		public final boolean transparentBackground;
		public final String imageExtension;
		public final boolean drawGeneSymbols;
		public final String fontName;
		public final int fontSize;
		//public final boolean dynamicallySizedFont = false;
		
		public final int minNodeAlpha;
		public final int minEdgeAlpha;
		
		public final String backgroundColor;
		
		public final String defaultNodeColor;
		public final String primaryGroupNodeColor;
		public final String secondaryGroupNodeColor;
		public final String bothGroupsNodeColor;
		public final boolean varyNodeAlphaValues;
		public final boolean varyEdgeAlphaValues;

		public RendererConfig(Map<String, String> map){
			transparentBackground = Boolean.parseBoolean(map.getOrDefault("transparentBackground", "true"));
			imageExtension = map.getOrDefault("imageExtension", "png");
			drawGeneSymbols = Boolean.parseBoolean(map.getOrDefault("drawGeneSymbols", "true"));
			fontName = map.getOrDefault("fontName", "Dialog");
			fontSize = Integer.parseInt(map.getOrDefault("fontSize", "12"));
			//dynamicallySizedFont = Boolean.parseBoolean(map.getOrDefault("dynamicallySizedFont", "false"));
			

			minNodeAlpha = Integer.parseInt(map.getOrDefault("minNodeAlpha", "50"));
			minEdgeAlpha = Integer.parseInt(map.getOrDefault("minEdgeAlpha", "50"));
			
			backgroundColor = map.getOrDefault("backgroundColor", "(255,255,255)");
			defaultNodeColor = map.getOrDefault("defaultNodeColor", "(255,0,0)"); //red
			primaryGroupNodeColor = map.getOrDefault("primaryGroupNodeColor", "(255,200,0)"); //orange
			secondaryGroupNodeColor = map.getOrDefault("secondaryGroupNodeColor", "(0,0,255)"); //blue
			bothGroupsNodeColor = map.getOrDefault("bothGroupsNodeColor", "(0,255,0)"); //green
			varyNodeAlphaValues = Boolean.parseBoolean(map.getOrDefault("varyNodeAlphaValues", "true"));
			varyEdgeAlphaValues = Boolean.parseBoolean(map.getOrDefault("varyEdgeAlphaValues", "true"));

		}

	}

	public final GeneralConfig generalConfig;
	public final AnalysisConfig analysisConfig;
	public final ForceDirectedLayoutConfig forceDirectedLayoutConfig;
	public final RendererConfig rendererConfig;

	private Configuration(Map<String, String> map){
		generalConfig = new GeneralConfig(map);
		analysisConfig = new AnalysisConfig(map);
		forceDirectedLayoutConfig = new ForceDirectedLayoutConfig(map);
		rendererConfig = new RendererConfig(map);
		
		final Set<String> keySetCopy = new HashSet<>(map.keySet());
		for(Class<?> clazz : this.getClass().getDeclaredClasses()){
			for(Field field : clazz.getDeclaredFields()){
				keySetCopy.remove(field.getName());
			}
		}
		for(String key : keySetCopy)
			System.err.println("Warning: unknown parameter " + key + " provided.");
	}

	public static Configuration defaultConfiguration(String primaryGeneSetGroupFile){
		return fromArgs("primaryGeneSetGroupFile=" + primaryGeneSetGroupFile);
	}

	public static Configuration fromFile(File file) throws IOException {
		try(final BufferedReader br = new BufferedReader(new FileReader(file))){
			final Map<String, String> map = new HashMap<>();
			String s;
			while((s = br.readLine()) != null){
				if(s.startsWith("#") || s.startsWith("//")) continue;
				final int idx = s.indexOf('=');
				if(idx == -1)
					throw new RuntimeException("Configuration file " + file + " is invalid: \"" + s + "\"");

				final String prevVal = map.put(s.substring(0, idx), s.substring(idx + 1));
				if(prevVal != null)
					throw new RuntimeException("Configuration file " + file + " is invalid: key \"" + s.substring(0, idx) + "\" appears more than once");
			}
			return Configuration.fromMap(map);
		}
	}

	public static Configuration fromArgs(String...args){
		final Map<String, String> map = new HashMap<>();
		for(String arg : args){
			final int idx = arg.indexOf('=');
			if(idx == -1)
				throw new RuntimeException("Argument \"" + arg + "\" is invalid");
			map.put(arg.substring(0, idx), arg.substring(idx + 1));
		}
		return Configuration.fromMap(map);
	}

	public static Configuration fromMap(Map<String, String> map){
		return new Configuration(map);
	}

}
