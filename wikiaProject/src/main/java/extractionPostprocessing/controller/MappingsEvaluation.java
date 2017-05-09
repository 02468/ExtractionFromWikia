package extractionPostprocessing.controller;

import java.io.File;
import java.util.*;

import extractionPostprocessing.model.Evaluator;
import extractionPostprocessing.util.IOHandler;

import java.util.logging.Logger;


public class MappingsEvaluation {

    private static Logger logger = Logger.getLogger(MappingsEvaluation.class.getName());

    private static Evaluator mappingsEvaluator;


    public static void evaluateAllMappings() {
        HashMap<Double, Integer> weightedAccuracies = new HashMap<>();
        double overallAccuracy = 0;
        int totalMappings = 0;
        String pathToRootDirectory = ResourceBundle.getBundle("config").getString("pathToRootDirectory");

        File root = new File(pathToRootDirectory);
        if (root.isDirectory()) {
            for (File directory : root.listFiles()) {
                if (directory.isDirectory()) {
                    Evaluator evaluator = evaluateMappingsForOneWiki(directory);
                    if (evaluator != null) {
                        logger.info("Accuracy: " + evaluator.getAccuracy() + "% of: " + directory.getName());
                        weightedAccuracies.put(evaluator.getAccuracy(), evaluator.getTotalMappings());
                    }
                }
            }

            for (int mappings : weightedAccuracies.values()) {
                totalMappings += mappings;
            }

            // calculate weighted accuracy by using mappings/totalMappings as weight
            for (double accuracy : weightedAccuracies.keySet()) {
                overallAccuracy += accuracy * weightedAccuracies.get(accuracy) / totalMappings;
            }

        }
        logger.info("Overall accuracy: " + overallAccuracy + "% of " + weightedAccuracies.size() + " wikis.");
    }


    private static Evaluator evaluateMappingsForOneWiki(File wikiPath) {
        return evaluateMappingsForOneWiki(wikiPath.getAbsolutePath());
    }


    private static Evaluator evaluateMappingsForOneWiki(String wikiPath) {
        HashMap<String, String> dbPediaMappings;
        HashMap<String, String> manualMappings;

        IOHandler ioHandler = new IOHandler();
        String dbPediaMappingFileName = ResourceBundle.getBundle("config").getString("mappingfilename");
        String manualMappingFileName = ResourceBundle.getBundle("config").getString("manualmappingfilename");

        File mappingFile = new File(wikiPath + "/" + dbPediaMappingFileName);
        File manualMappingFile = new File(wikiPath + "/" + manualMappingFileName);

        if (!mappingFile.exists()) {
            return null;
        }

        // if there is no manual mapping file with the name specified in the properties file use the file that ends
        // with the specified name
        if(!manualMappingFile.exists()){
            // check whether there is a file ending with the specified name
            File directory = new File(wikiPath);
            if(directory.isDirectory()){
                for (File f:directory.listFiles()) {
                    if(f.getName().endsWith(manualMappingFileName)){
                        manualMappingFile = f;
                    }
                }
            } else {
                // wikiPath is not a directory
                return null;
            }
        }

        int truePositives = 0;
        int trueNegatives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;
        int totalMapping = 0;

        try {
            dbPediaMappings = ioHandler.getExtractorMappings(mappingFile);
            manualMappings = ioHandler.getExtractorMappings(manualMappingFile);

            for (String resource : manualMappings.keySet()) {
                if (dbPediaMappings.containsKey(resource)) {
                    totalMapping++;
                    if (manualMappings.get(resource).toLowerCase().equals(dbPediaMappings.get(resource).toLowerCase())) {
                        truePositives++;
                    }
                }
            }

        } catch (Exception ex) {
            logger.severe(ex.getMessage());
        }

        mappingsEvaluator = new Evaluator(falseNegatives, falsePositives, truePositives, trueNegatives, totalMapping);

        return mappingsEvaluator;
    }

}
