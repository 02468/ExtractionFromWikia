package extractionPostprocessing.controller;

import java.io.*;
import java.util.*;

import extractionPostprocessing.model.EvaluationResult;
import extractionPostprocessing.util.IOHandler;

import java.util.logging.Logger;

/**
 * After creating the mapping files using a mapper, the evaluation for that mapping can be performed using this class.
 */
public class MappingEvaluation {

    private static Logger logger = Logger.getLogger(MappingEvaluation.class.getName());

    private static EvaluationResult mappingsEvaluationResult;

    /**
     * Evaluate all mappings and print the result on the command line.
     * The results are also persisted in a file.
     */
    public static void evaluateAllMappings() {

        double overallAccuracy = 0;
        int totalMappings = 0;
        ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
        String pathToRootDirectory = ResourceBundle.getBundle("config").getString("pathToRootDirectory");
        StringBuffer aggregatedEvaluationResults = new StringBuffer();
        String evaluationResultLine = "";
        File root = new File(pathToRootDirectory);

        if (root.isDirectory()) {
            for (File directory : root.listFiles()) {
                if (directory.isDirectory()) {
                    EvaluationResult evaluationResult = evaluateMappingsForOneWiki(directory);
                    if (evaluationResult != null) {
                        evaluationResultLine = "Accuracy: " + evaluationResult.getAccuracy() + "% (" + directory.getName() + ")";
                        logger.info(evaluationResultLine);
                        aggregatedEvaluationResults.append(evaluationResultLine + "\n");
                        totalMappings += evaluationResult.getTotalMappings();
                        evaluationResults.add(evaluationResult);
                    }
                }
            }

            for(EvaluationResult e : evaluationResults){
                double e_accuracy = e.getAccuracy();
                int e_totalMappings = e.getTotalMappings();
                overallAccuracy += (e.getAccuracy() * ( (double) e.getTotalMappings() / totalMappings ));
            }

        } else {
            logger.severe("pathToRootDirectory is not a directory!");
        }
        evaluationResultLine = "Overall accuracy of " + evaluationResults.size() + " wikis: " + overallAccuracy + "%";
        logger.info(evaluationResultLine);
        aggregatedEvaluationResults.append(evaluationResultLine + "\n");

        // persist evaluation results to evaluation file:
        File evaluationFile = new File(pathToRootDirectory + "/evaluation_results.txt");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(evaluationFile));
            bw.write(aggregatedEvaluationResults.toString());
            bw.close();
        } catch (IOException ioe){
            logger.severe(ioe.toString());
        }

    }


    /**
     * Create evaluations for one wiki
     * @param wikiPath
     * @return
     */
    private static EvaluationResult evaluateMappingsForOneWiki(String wikiPath) {
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

        mappingsEvaluationResult = new EvaluationResult(falseNegatives, falsePositives, truePositives, trueNegatives, totalMapping);

        return mappingsEvaluationResult;
    }

    /**
     * overloaded method
     * @param wikiPath
     * @return
     */
    private static EvaluationResult evaluateMappingsForOneWiki(File wikiPath) {
        return evaluateMappingsForOneWiki(wikiPath.getAbsolutePath());
    }

}