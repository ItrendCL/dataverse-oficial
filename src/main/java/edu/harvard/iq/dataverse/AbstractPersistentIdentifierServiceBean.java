package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.text.SimpleDateFormat;

import javax.ejb.EJB;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractPersistentIdentifierServiceBean implements PersistentIdentifierServiceBean {

    private static final Logger logger = Logger.getLogger(AbstractPersistentIdentifierServiceBean.class.getCanonicalName());

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    SystemConfig systemConfig;

    @Override
    public String getIdentifierForLookup(String protocol, String authority, String separator, String identifier) {
        logger.log(Level.FINE,"getIdentifierForLookup");
        return protocol + ":" + authority + separator + identifier;
    }

    @Override
    public Map<String, String> getMetadataFromStudyForCreateIndicator(Dataset datasetIn) {
        logger.log(Level.FINE,"getMetadataFromStudyForCreateIndicator");
        HashMap<String, String> metadata = new HashMap<>();

        metadata = addBasicMetadata(datasetIn, metadata);
        metadata.put("datacite.publicationyear", generateYear(datasetIn));
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }

    protected Map<String, String> getUpdateMetadataFromDataset(Dataset datasetIn) {
        logger.log(Level.FINE,"getUpdateMetadataFromDataset");
        HashMap<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(datasetIn, metadata);
        return metadata;
    }
    
    protected HashMap<String, String> addBasicMetadata(Dataset datasetIn, HashMap<String, String> metadata){
        String authorString = datasetIn.getLatestVersion().getAuthorsStr();

        if (authorString.isEmpty()) {
            authorString = ":unav";
        }

        String producerString = dataverseService.findRootDataverse().getName();

        if(producerString.isEmpty()) {
            producerString = ":unav";
        }
        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", datasetIn.getLatestVersion().getTitle());
        metadata.put("datacite.publisher", producerString);
        
        
        return metadata;
    }

    @Override
    public HashMap<String, String> getMetadataFromDatasetForTargetURL(Dataset datasetIn) {
        logger.log(Level.FINE,"getMetadataFromDatasetForTargetURL");
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(datasetIn));
        return metadata;
    }

    protected String getTargetUrl(Dataset datasetIn) {
        logger.log(Level.FINE,"getTargetUrl");
        return systemConfig.getDataverseSiteUrl() + Dataset.TARGET_URL + datasetIn.getGlobalIdString();
    }

    @Override
    public String getIdentifierFromDataset(Dataset dataset) {
        logger.log(Level.FINE,"getIdentifierFromDataset");
        return dataset.getGlobalIdString();
    }
    
    protected String generateYear (Dataset datasetIn){
        if (datasetIn.isReleased()) {
            return datasetIn.getPublicationDateFormattedYYYYMMDD().substring(0, 4);
        }
        return new SimpleDateFormat("yyyy").format(datasetIn.getCreateDate()); 
    }

}
