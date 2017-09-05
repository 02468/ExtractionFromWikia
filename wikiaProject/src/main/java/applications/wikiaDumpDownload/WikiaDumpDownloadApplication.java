package applications.wikiaDumpDownload;

import applications.wikiaDumpDownload.controller.WikiaDumpDownloadThreadImpl;

import java.util.Arrays;
import java.util.List;

/**
 * This class represents the application to download dumps of Wikia wikis.
 *
 * Please specify the directory for the downloaded files in resources > config.properties
 * Before running this code you have to run wikiaStatisticsApplication to retrieve a list of all wikis.
 *
 * You can either specify the urls of dumps as a list to download or download all available wikis.
 *
 * If needed, you can unzip all downloaded 7zip files.
 */
public class WikiaDumpDownloadApplication {

    public static void main(String[] args) {

        // download list of urls
        List<String> urls = Arrays.asList("http://harrypotter.wikia.com", "http://gameofthrones.wikia.com");
        WikiaDumpDownloadThreadImpl.downloadWikiaDumps(urls);

        // download all available wikis
//        if (WikiaDumpDownloadThreadImpl.checkPrerequisites()) {
//            WikiaDumpDownloadThreadImpl.downloadWikiaDumps();
//        }



    }

}