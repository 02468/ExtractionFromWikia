package applications.wikiaStatistics.model;


import loggingService.MessageLogger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.log4j.Level;

import java.io.PrintWriter;

/**
 * This class represents a statistics object with various information.
 */
public class MetadataStatistics {

    private HashMap<String, Integer> languageCounts; // For mapping language (key) to the number of wikis in that language (value)
    private HashMap<String, Integer> topicCounts;
    private HashMap<String, Integer> hubCounts;
    private int numberOfArticles;
    private int numberOfPages;

    private static MessageLogger logger=new MessageLogger();
    private static final String MODULE="wikiaStatistics";
    private static final String CLASS=MetadataStatistics.class.getName();


    /**
     * Constructor
     * Initialize variables
     */
    public MetadataStatistics() {
        this.languageCounts = initializeHashMap();
        topicCounts = new HashMap<String, Integer>();
        hubCounts = new HashMap<String, Integer>();
        this.numberOfArticles = 0;
        this.numberOfPages = 0;
    }


    public HashMap<String, Integer> getHubCounts() {
        return hubCounts;
    }

    public HashMap<String, Integer> getLanguageCounts() {
        return languageCounts;
    }

    public HashMap<String, Integer> getTopicCounts() { return topicCounts; }

    public void setLanguageCounts(HashMap<String, Integer> languageCounts) {
        this.languageCounts = languageCounts;
    }

    public int getNumberOfArticles() {
        return numberOfArticles;
    }

    public void setNumberOfArticles(int numberOfArticles) {
        this.numberOfArticles = numberOfArticles;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public void setTopicCounts(HashMap<String, Integer> topicCounts) {
        this.topicCounts = topicCounts;
    }

    public void setHubCounts(HashMap<String, Integer> topicCounts) {
        this.hubCounts = hubCounts;
    }


    @Override
    public String toString() {
        return "MetadataStatistics: \n" +
                "Languages = " + languageCounts + "\n" +
                "Number of articles = " + numberOfArticles + "\n" +
                "Number of pages = " + numberOfPages + "\n" +
                "Hubs = " + hubCounts + "\n" +
                "Topics = " + topicCounts + "\n";
    }

    /**
     * Initialize the language HashMap with language codes of 'wikiaLanguageCodes' file and occurrence of zero.
     * Called by constructor.
     *
     * @return initialized HashMap
     */
    private HashMap<String, Integer> initializeHashMap() {
        BufferedReader bufferedReader;
        String readLine;
        String[] tokens;
        HashMap<String, Integer> languageCounts = new HashMap<String, Integer>();

        String directoryPath = ResourceBundle.getBundle("config").getString("pathToRootDirectory");

        try {
            bufferedReader = new BufferedReader(new FileReader("./wikiaProject/src/main/resources/files/wikiaLanguageCodes.csv"));

            while ((readLine = bufferedReader.readLine()) != null) {
                tokens = readLine.split(";");

                if (!tokens[0].isEmpty()) {
                    languageCounts.put(tokens[0], 0);
                }
            }

        } catch (IOException ioe) {
            logger.logMessage(Level.FATAL ,MODULE,CLASS,"HashMap has not been initialized." + ioe.toString());
        }

        return languageCounts;
    }

    /**
     * Limits number of language codes returned according to the specified occurrence (how many wikis exist for a code)
     *
     * @param occurrences
     */
    public void limitTopLanguages(int occurrences) {
        HashMap<String, Integer> limitedLanguages = new HashMap<String, Integer>();

        for (Map.Entry<String, Integer> entry : this.getLanguageCounts().entrySet()) {

            if(entry.getValue() >= occurrences) {
                // add entries with at minimum specified number of occurrences
                limitedLanguages.put(entry.getKey(), entry.getValue());
            }

        }

        this.setLanguageCounts(limitedLanguages);
    }


    /**
     *
     * @param fileContents
     */
    public void writeResultToFile(String fileContents) {

        String directoryPath = ResourceBundle.getBundle("config").getString("pathToRootDirectory");
        String filePath = directoryPath + "/statistics/wikislanguages.csv";

        try{
            //Initialize file Writer Objects
            PrintWriter fileWriter = new PrintWriter(filePath, "UTF-8");

            //Write contents to file
            fileWriter.write(fileContents);

            //Close file Writer
            fileWriter.close();
        }
        catch(Exception exception){

            logger.logMessage(Level.FATAL ,MODULE,CLASS,exception.getMessage());
        }
    }

}
