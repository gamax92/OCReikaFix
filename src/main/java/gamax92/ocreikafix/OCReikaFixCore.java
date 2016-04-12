package gamax92.ocreikafix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name(value = "OCReikaFix CoreMod")
@IFMLLoadingPlugin.SortingIndex(value = Integer.MAX_VALUE)
public class OCReikaFixCore implements IFMLLoadingPlugin {
	public static final Logger logger = LogManager.getLogger("OCReikaFix Core");

	public static void logMessage(Level level, String message) {
		logger.log(level, "[OCReikaFix] " + message);
	}

	private void loadDefaults() {
		HashSet<String> classes = OCReikaFixTransformer.classes;

		classes.clear();
		classes.add("Reika.DragonAPI.Base.TileEntityBase");
		classes.add("Reika.RotaryCraft.Base.TileEntity.RotaryCraftTileEntity");
		classes.add("Reika.ReactorCraft.Base.TileEntityReactorBase");
	}

	private void saveConfig(File configLocation) {
		HashSet<String> classes = OCReikaFixTransformer.classes;

		logMessage(Level.INFO, "Saving configuration ...");

		PrintWriter cfgOut = null;
		try {
			cfgOut = new PrintWriter(new FileWriter(configLocation));
			cfgOut.println("# Empty lines or lines starting with a '#' are ignored");
			cfgOut.println();

			for (String className : classes) {
				cfgOut.println(className);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (cfgOut != null) {
				cfgOut.close();
			}
		}
	}

	private void loadConfig(File configLocation) {
		HashSet<String> classes = OCReikaFixTransformer.classes;

		BufferedReader cfgIn = null;
		try {
			cfgIn = new BufferedReader(new FileReader(configLocation));
			String text = null;

			logMessage(Level.INFO, "Loading configuration ...");

			while ((text = cfgIn.readLine()) != null) {
				text = text.trim();
				if (!text.equals("") && !text.startsWith("#"))
					classes.add(text);
			}
		} catch (FileNotFoundException e) {
			logMessage(Level.ERROR, configLocation.getName() + " missing, using defaults");
			loadDefaults();
		} catch (IOException e) {
			e.printStackTrace();
			loadDefaults();
		} finally {
			try {
				if (cfgIn != null) {
					cfgIn.close();
				}
			} catch (IOException e) {
			}
		}
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { OCReikaFixTransformer.class.getName() };
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {

		if (data.containsKey("mcLocation")) {
			File configLocation = new File((File) data.get("mcLocation"), "config/OCReikaFix.cfg");
			loadConfig(configLocation);
			saveConfig(configLocation);
		} else {
			logMessage(Level.ERROR, "mcLocation key missing, using defaults");
			loadDefaults();
		}
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}