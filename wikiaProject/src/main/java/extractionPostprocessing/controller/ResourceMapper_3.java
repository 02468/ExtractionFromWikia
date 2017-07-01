package extractionPostprocessing.controller;

import extractionPostprocessing.model.ResourceMapper;
import extractionPostprocessing.model.ResourceMapperInterface;
import extractionPostprocessing.model.SPARQLresult;
import utils.FileOperations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;

/**
 * Third mapper implementation.
 * - automatically maps files to <null>
 * - checks whether a resource exists before mapping it
 */
public class ResourceMapper_3 extends ResourceMapper {

    @Override
    public String mapSingleResource(String resourceToMap) {
        if(resourceToMap.contains("/File:")){
            return "<null>";
        } else {
            SPARQLresult result = DBpediaResourceService.getResourceAndRedirectInDBpedia(resourceToMap);

            if(result.resourceExists){
                if(result.redirectResource != null){
                    // redirect source found
                    return result.redirectResource;
                } else {
                    // -> no redirect resource -> use dbPediaResource
                    return(resourceToMap);
                }
            } else {
                // -> resource does not exist -> map to <null>
                return "<null>";
            }
        }
    }

}
