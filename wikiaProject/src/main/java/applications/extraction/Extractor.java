package applications.extraction;

import applications.extraction.model.WikiaWikiProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import loggingService.MessageLogger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.log4j.Level;
import utils.*;

import java.io.PrintWriter;


/**
 * This class will perform the extraction of the previously downloaded wikis.
 */
public class Extractor {

    private static File extractionFrameworkDirectory;
    private String extractionDefaultPropertiesFilePath;
    private HashMap<String, WikiaWikiProperties> wikisPropertiesSet;
    private String pathToRootDirectory;
    private HashMap<String, String> dumpURLsMapping;
    private static MessageLogger logger = new MessageLogger();
    private static final String MODULE = "Extraction";
    private static final String CLASS = Extractor.class.getName();


    public Extractor() {

        // get the path to the DBpedia applications.extraction framework
        String filepath = System.getProperty("user.dir") + "//lib//dbpedia-extraction-framework";
        extractionFrameworkDirectory = new File(filepath);
        extractionDefaultPropertiesFilePath = extractionFrameworkDirectory.getAbsolutePath()
                + "\\dump\\applications.extraction.default.properties";
        pathToRootDirectory = ResourceBundle.getBundle("config").getString("pathToRootDirectory");
    }

    public void extractAllWikis() {

        logger.logMessage(Level.INFO,MODULE,CLASS,"Unarchiving all dumps");
        unarchiveDownloadedDumps();

        logger.logMessage(Level.INFO,MODULE,CLASS,"Creating folder structure for DBpedia extractor");
        createDbpediaExtractionStructure();

        logger.logMessage(Level.INFO,MODULE,CLASS,"Calling DBpediaExtractor");
        callDbPediaExtractorToExtractFile();

        logger.logMessage(Level.INFO,MODULE,CLASS,"Moving files for evaluation");
        moveExtractFilesforEvaluation();
    }


    /**
     * This methods unarchives all the downloaded dumps found in /<root>/downloadedWikis/7z and /<root>/downloadedWikis/bz.
     * The extracted files can be found in /<root>/downloadedWikis/decompressed
     */
    public void unarchiveDownloadedDumps() {
        try {
            String wikisFilePath =
                    ResourceBundle.getBundle("config").getString("pathToRootDirectory")
                            + "//downloadedWikis//downloaded//";
            File downloadedWikisFolder = new File(wikisFilePath);
            Extraction7zip extractor7Zip = new Extraction7zip();
            ExtractionGZip extractorGZip = new ExtractionGZip();

            logger.logMessage(Level.DEBUG,MODULE,CLASS,"Downloaded Wikis Path : " + wikisFilePath);

            if (downloadedWikisFolder.isDirectory()) {
                File[] downloadedWikisFormats = downloadedWikisFolder.listFiles();
                for (File downloadedWikisFormat : downloadedWikisFormats) {
                    if (downloadedWikisFormat.isDirectory()) {
                        if (downloadedWikisFormat.getName().endsWith("7z")) {
                            logger.logMessage(Level.INFO,MODULE,CLASS,"Unarchiving files in 7z format");
                            extractor7Zip.extractAll7ZipFilesIntoDesignatedFolder();
                        } else if (downloadedWikisFormat.getName().endsWith("gz")) {

                            logger.logMessage(Level.INFO,MODULE,CLASS,"Unarchiving files in gz format");
                            extractorGZip.extractAllGZipFilesIntoDesignatedFolder();
                        }
                    }
                }
            }


        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }
    }

    /**
     * Method will generate a properties file and save it within the DBpedia applications.extraction framework
     */
    private WikiaWikiProperties extractPropertiesForaWiki(String wikiFilePath) {
        WikiaWikiProperties wikiProperties = null;

        try {
            String line = "";
            String fileContents = "";
            String languageCode = "";
            String wikiName = "";
            String wikiPath = wikiFilePath;
            String wikiBaseURL;
            int lineNumber = 0;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date lastModifiedDate;
            long wikiSize;

            if (dumpURLsMapping == null) {
                IOoperations io = new IOoperations();
                dumpURLsMapping = io.readDumpsURL();
            }


            File wikiFile = new File(wikiFilePath);

            logger.logMessage(Level.INFO,MODULE,CLASS,"Getting Properties for wiki: " + wikiFilePath);

            lastModifiedDate = simpleDateFormat.parse(simpleDateFormat.format(wikiFile.lastModified()));

            logger.logMessage(Level.DEBUG,MODULE,CLASS,"Total Size: " + (wikiFile.length() / 1024) + " KB");

            FileReader fr = new FileReader(wikiFile);

            BufferedReader br = new BufferedReader(fr);

            while ((line = br.readLine()) != null && lineNumber <= 20) {
                fileContents += line;
                lineNumber++;
            }

            if (fileContents.length() > 0) {

                languageCode = fileContents.substring(fileContents.indexOf("xml:lang=") + 10, fileContents.indexOf(">", fileContents.indexOf("xml:lang=") + 10) - 1);


                if ((fileContents.indexOf("<sitename>") + 10) <=
                        fileContents.indexOf("</sitename>", fileContents.indexOf("<sitename>") + 10) - 1) {
                    wikiName = (fileContents.substring(fileContents.indexOf("<sitename>") + 10, fileContents.indexOf("</sitename>", fileContents.indexOf("<sitename>") + 10))).trim().replace(" ", "_");
                } else {
                    wikiName = "";
                }

                wikiSize = (wikiFile.length() / 1024);

                if (dumpURLsMapping.get(wikiFile.getName()) != null) {
                    wikiBaseURL = dumpURLsMapping.get(wikiFile.getName());
                } else
                    wikiBaseURL = "";


                wikiProperties = new WikiaWikiProperties(wikiName, languageCode, wikiPath, lastModifiedDate, wikiSize, wikiBaseURL);

            }
            br.close();
            fr.close();


        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }

        return wikiProperties;

    }

    /**
     * This function reads downloaded wikis
     *
     * @return Hashmap contains key as wiki name and value as obejct containing
     * wiki properties
     */
    public HashMap<String, WikiaWikiProperties> extractPropertiesForAllWikis() {

        //WikiaWikisProperties
        HashMap<String, WikiaWikiProperties> wikiProperties = new HashMap<String, WikiaWikiProperties>();
        int index = 0;

        String wikisFilePath =
                ResourceBundle.getBundle("config").getString("pathToRootDirectory")
                        + "//downloadedWikis//decompressed//";

        try {
            File wikisFilesFolder = new File(wikisFilePath);

            //get list of wikis in a folder
            File[] wikiFiles = wikisFilesFolder.listFiles();

            logger.logMessage(Level.INFO,MODULE,CLASS,"Total Files: " + wikiFiles.length);

            for (File wikiFile : wikiFiles) {

                if (wikiFile.isFile() && wikiFile.getName().endsWith(".xml")) {

                    WikiaWikiProperties properties = extractPropertiesForaWiki(wikiFile.getPath());

                    if (wikiProperties != null) {
                        //wikiProperties.put(properties.getWikiName(), properties);
                        wikiProperties.put(properties.getWikiPath().substring(properties.getWikiPath().lastIndexOf("/", properties.getWikiPath().length())), properties);
                    }

                } else {
                    logger.logMessage(Level.FATAL,MODULE,CLASS,"File is not valid XML : " + wikiFile.getName());
                    break;
                }
            }

        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }
        return wikiProperties;
    }

    /**
     * Create structure expected by DBpedia extractor for applications.extraction
     */
    public void createDbpediaExtractionStructure() {

        try {

            wikisPropertiesSet = extractPropertiesForAllWikis();
            String downloadDirectoryForExtraction = ResourceBundle.getBundle("config").getString("pathToRootDirectory")
                    + "//dbPediaExtractionFormat//";
            String wikiSourceFileName = ResourceBundle.getBundle("config").getString("wikiSourceFileName");
            String[] languageCodestoExtract = ResourceBundle.getBundle("config").getString("languages").split(",");

            WikiaWikiProperties wikiProperties = null;
            String languageCode;
            String wikiFilePath;
            File languageDirectory = null;
            File dateDirectory = null;
            int index = 1;
            String DATE_FORMAT_NOW = "YYYYMMdd";
            Calendar calender = Calendar.getInstance();
            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_NOW);
            String currentDate = dateFormatter.format(calender.getTime());


            File extractionDirectory = new File(downloadDirectoryForExtraction);

            if (!extractionDirectory.exists()) extractionDirectory.mkdir();

            for (String wikiName : wikisPropertiesSet.keySet()) {

                wikiProperties = wikisPropertiesSet.get(wikiName);

                //Get properties
                languageCode = wikiProperties.getLanguageCode();

                if (Arrays.asList(languageCodestoExtract).contains(languageCode)) {

                    String siteName = wikiProperties.getWikiName();
                    wikiFilePath = wikiProperties.getWikiPath();

                    //dateFolderName = wikiName.substring(0,wikiName.indexOf("_"));
                    //dateFolderName = Integer.toString(index);

                    languageDirectory = new File(downloadDirectoryForExtraction + "/" + languageCode + "wiki_");

                    if (! languageDirectory.exists())
                        languageDirectory.mkdir();

                    dateDirectory = new File(downloadDirectoryForExtraction + "/" + languageCode + "wiki_" + "/" + index);

                    if (! dateDirectory.exists())
                        dateDirectory.mkdir();

                    copyFileFromOneDirectorytoAnotherDirectory(wikiFilePath, downloadDirectoryForExtraction + "/" + languageCode + "wiki_" + "/" + index + "/" +
                            languageCode + "wiki-" + currentDate + "-" + wikiSourceFileName);
                    createWikiPropertiesFile(downloadDirectoryForExtraction + "/" + languageCode + "wiki_" + "/" + index + "/", wikiProperties);
                    index++;
                }
            }

        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }

    }


    /**
     * Copies one file from one directory to another directory
     *
     * @param sourceFilePath
     * @param targetFilePath
     */

    public void copyFileFromOneDirectorytoAnotherDirectory(String sourceFilePath, String targetFilePath) {

        try {
            File sourceFile = new File(sourceFilePath);
            File targetFile = new File(targetFilePath);

            if (!targetFile.exists()) targetFile.createNewFile();

            FileInputStream sourceFileInputStream = new FileInputStream(sourceFile);
            FileOutputStream targetFileOutputStream = new FileOutputStream(targetFile);

            byte[] buffer = new byte[1024];

            int length;

            //copy the file content in bytes
            while ((length = sourceFileInputStream.read(buffer)) > 0) {

                targetFileOutputStream.write(buffer, 0, length);
            }

            sourceFileInputStream.close();
            targetFileOutputStream.close();

        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }
    }


    /**
     * This method will call DBpedia extractor to extract all download wikis
     */
    public void callDbPediaExtractorToExtractFile() {
        try {
            String downloadDirectoryForExtraction = ResourceBundle.getBundle("config").getString("pathToRootDirectory")
                    + "//dbPediaExtractionFormat//";
            String pathToExtractionFramework = System.getProperty("user.dir") + "//lib//dbpedia-extraction-framework//dump";
            String dbPediaExtractorBatchFile;
            String DATE_FORMAT_NOW = "YYYYMMdd";
            Calendar calender = Calendar.getInstance();
            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_NOW);
            String current_date = dateFormatter.format(calender.getTime());
            String date = "";
            String extractionPropertiesFile;

            CommandLine cmdLine = null;
            DefaultExecutor executor = null;
            IOoperations iOoperations = new IOoperations();

            logger.logMessage(Level.INFO,MODULE,CLASS,"Generating extraction properties file");

            iOoperations.generateExtractionProperties();
            extractionPropertiesFile = this.getClass().getClassLoader().getResource("extraction.properties").getPath().toString();
            Executor persmissionExecutor = new DefaultExecutor();


            //Check operating system and trigger command accordingly
            if(utils.OSDetails.isWindows()){
                dbPediaExtractorBatchFile = this.getClass().getClassLoader().getResource("dbpediaextraction.bat").getPath();
            }
            else if(utils.OSDetails.isUnix()){
                dbPediaExtractorBatchFile = this.getClass().getClassLoader().getResource("dbpediaextraction.sh").getPath();

                // granting execution rights
                persmissionExecutor.execute(CommandLine.parse("chmod 777 " + dbPediaExtractorBatchFile));
            }
            else{
                dbPediaExtractorBatchFile = this.getClass().getClassLoader().getResource("dbpediaextraction.sh").getPath();

                // granting execution rights
                persmissionExecutor.execute(CommandLine.parse("chmod 777 " + dbPediaExtractorBatchFile));
            }

            String batchCommand = dbPediaExtractorBatchFile +" " + pathToExtractionFramework+" " +  extractionPropertiesFile;


            File downloadedWikisDirectory = new File(downloadDirectoryForExtraction);
            File[] languageCodesFolders = downloadedWikisDirectory.listFiles();

            for (File languageCodeFolder : languageCodesFolders) {
                if (languageCodeFolder.isDirectory() && ! languageCodeFolder.getName().toLowerCase().equals("commonswiki")) {

                    File renamedLanguageCodeFolder = new File(languageCodeFolder.getAbsolutePath().substring(0, languageCodeFolder.getAbsolutePath().length() - 1));
                    languageCodeFolder.renameTo(renamedLanguageCodeFolder);
                    File[] dateFolders = renamedLanguageCodeFolder.listFiles();

                    for (File wikiDirectory : dateFolders) {

                        if (! iOoperations.checkIfFileExists(wikiDirectory.getAbsolutePath(), "*-complete")) {

                            File[] wikiFiles = wikiDirectory.listFiles();
                            String folderName = wikiDirectory.getAbsolutePath();
                            // File[] filesToExtract = renamedFolder.listFiles();

                            for (File fileForExtraction : wikiFiles) {
                                if (fileForExtraction.getName().endsWith(".xml")) {

                                    date = fileForExtraction.getName().substring(fileForExtraction.getName().indexOf("-") + 1,
                                            fileForExtraction.getName().indexOf("-", fileForExtraction.getName().indexOf("-") + 1));

                                    File commonsWikiDirectory = new
                                            File(downloadedWikisDirectory.getAbsoluteFile()
                                            + "//commonswiki//" + date + "//");

                                    if (commonsWikiDirectory.exists()) {
                                        commonsWikiDirectory.delete();
                                    }

                                    commonsWikiDirectory.mkdirs();

                                    copyFileFromOneDirectorytoAnotherDirectory(
                                            fileForExtraction.getAbsolutePath(),
                                            commonsWikiDirectory.getAbsolutePath()
                                                    + "//commonswiki-" + date + "-pages-current.xml");
                                }
                            }

                            File renamedFolder = new File(wikiDirectory.getParent() + "/" + date);
                            wikiDirectory.renameTo(renamedFolder);

                            try {
                                //call DBpedia extractor
                                cmdLine = CommandLine.parse(batchCommand);
                                executor = new DefaultExecutor();
                                executor.setExitValue(0);
                                int exitValue = executor.execute(cmdLine);
                            } catch (Exception ex) {
                                logger.logMessage(Level.ERROR,MODULE,CLASS,"DBpedia extraction framework failed for this wiki!");
                                ex.printStackTrace();
                            }

                            //rename folder to orignal name
                            renamedFolder.renameTo(new File(folderName));
                        }
                    }
                    renamedLanguageCodeFolder.renameTo(languageCodeFolder);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }

    }

    /**
     * This function extracts compressed files obtained from DBpedia extractor
     * and moves to separate directory for mapping and evaluation
     */
    public void moveExtractFilesforEvaluation() {

        String downloadDirectoryForExtraction =
                pathToRootDirectory + "//dbPediaExtractionFormat";

        String postProcessedFilesDirectoryPath =
                pathToRootDirectory + "//postProcessedWikis";

        ExtractionBz2 bz2Extractor = new ExtractionBz2();
        String wikiFolderName = "";

        try {

            File extractedWikiFolder = new File(downloadDirectoryForExtraction);

            //get list of wikis in a folder
            File[] listOfFolders = extractedWikiFolder.listFiles();

            for (File languageCodeFolder : listOfFolders) {
                if (languageCodeFolder.isDirectory() && !languageCodeFolder.getName().toLowerCase().equals("commonswiki")) {

                    File[] dateFolders = languageCodeFolder.listFiles();

                    for (File dateFolder : dateFolders) {

                        if (dateFolder.isDirectory()) {

                            WikiaWikiProperties properties = readWikiPropertiesFile(dateFolder.getAbsolutePath());

                            if (properties.getWikiBaseURL().equals("")) {
                                wikiFolderName = properties.getWikiName();
                            } else {
                                // onlx keep name (xxx) of Url: http://xxx.wikia.com
                                wikiFolderName = properties.getWikiBaseURL().substring(7, properties.getWikiBaseURL().length() - 10);
                            }

                            File extractedFilesFolder = new File(postProcessedFilesDirectoryPath + "//" + wikiFolderName);

                            if (! extractedFilesFolder.exists()) {
                                extractedFilesFolder.mkdirs();
                            }

                            File[] extractedFiles = dateFolder.listFiles();

                            logger.logMessage(Level.INFO,MODULE,CLASS,"Moving files of " + wikiFolderName);

                            for (File wikiFile : extractedFiles) {
                                if (wikiFile.getName().endsWith(".bz2")) {
                                    bz2Extractor.extract(wikiFile.getAbsolutePath(),
                                            postProcessedFilesDirectoryPath + "//" + wikiFolderName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }
    }


    /**
     * This function creates a properties file for a wiki so that it can be used
     * to create folders with proper names for evaluation
     *
     * @param wikiFolderPath - folder path for respective wiki
     * @param wikiProperties - properties of respective wiki (wiki name, language)
     */
    public void createWikiPropertiesFile(String wikiFolderPath, WikiaWikiProperties wikiProperties) {

        try {

            String newLineCharacter = OSDetails.getNewLineCharacter();

            PrintWriter fileWriter = new PrintWriter(wikiFolderPath + "//wiki.prop");

            logger.logMessage(Level.INFO,MODULE,CLASS,"Writing properties for wiki: " + wikiProperties.getWikiName());

            fileWriter.write("WikiName:" + wikiProperties.getWikiName() + newLineCharacter);
            fileWriter.write("LanguageCode:" + wikiProperties.getLanguageCode() + newLineCharacter);
            fileWriter.write("File Path:" + wikiProperties.getWikiPath() + newLineCharacter);
            fileWriter.write("BaseURL:" + wikiProperties.getWikiBaseURL() + newLineCharacter);

            fileWriter.close();

        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }
    }


    /**
     * This function reads properties of wiki
     *
     * @param wikiFolderPath - path of directory where wiki is present
     * @return properties of respective wiki (wiki name, language)
     */
    public WikiaWikiProperties readWikiPropertiesFile(String wikiFolderPath) {

        WikiaWikiProperties wikiProperties = new WikiaWikiProperties();

        try {

            String fileLine = "";
            FileReader fileReader = new FileReader(wikiFolderPath + "//wiki.prop" );

            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((fileLine = bufferedReader.readLine()) != null) {

                String key = fileLine.substring(0, fileLine.indexOf(":"));
                String value = fileLine.substring(fileLine.indexOf(":") + 1, fileLine.length());

                if (key.toLowerCase().equals("wikiname")) {

                    value = value.replace(":", "");
                    value = value.replace("@", "");
                    value = value.replace("*", "");
                    value = value.replace("/", "");
                    value = value.replace("\\", "");
                    value = value.replace("?", "");
                    value = value.replace("|", "");
                    value = value.replace("\"\"", "");
                    wikiProperties.setWikiName(value);

                } else if (key.toLowerCase().equals("languagecode")) {
                    wikiProperties.setLanguageCode(value);
                } else if (key.toLowerCase().equals("baseurl")) {
                    wikiProperties.setWikiBaseURL(value);
                    ;
                }
            }

            bufferedReader.close();
            fileReader.close();

        } catch (Exception ex) {
            logger.logMessage(Level.FATAL,MODULE,CLASS,ex.getMessage());
        }
        return wikiProperties;
    }

}
