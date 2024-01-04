package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomStringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public abstract class AbstractPidProvider implements PidProvider {

    private static final Logger logger = Logger.getLogger(AbstractPidProvider.class.getCanonicalName());

    public static String UNAVAILABLE = ":unav";
    
    PidProviderFactoryBean pidProviderService;

    private String protocol;

    private String authority;

    private String shoulder;

    private String identifierGenerationStyle;

    private String datafilePidFormat;

    private List<String> managedList;

    private List<String> excludedList;
    
    AbstractPidProvider(String protocol, String authority, String shoulder, String identifierGenerationStyle, String datafilePidFormat, String managedList, String excludedList) {
        this.protocol = protocol;
        this.authority = authority;
        this.shoulder = shoulder;
        this.identifierGenerationStyle = identifierGenerationStyle;
        this.datafilePidFormat = datafilePidFormat;
        this.managedList = Arrays.asList(managedList.split(",\\s")); 
        this.excludedList = Arrays.asList(excludedList.split(",\\s"));
        
    }
    
    @Override
    public Map<String, String> getMetadataForCreateIndicator(DvObject dvObjectIn) {
        logger.log(Level.FINE,"getMetadataForCreateIndicator(DvObject)");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        metadata.put("datacite.publicationyear", generateYear(dvObjectIn));
        metadata.put("_target", getTargetUrl(dvObjectIn));
        return metadata;
    }

    protected Map<String, String> getUpdateMetadata(DvObject dvObjectIn) {
        logger.log(Level.FINE,"getUpdateMetadataFromDataset");
        Map<String, String> metadata = new HashMap<>();
        metadata = addBasicMetadata(dvObjectIn, metadata);
        return metadata;
    }
    
    protected Map<String, String> addBasicMetadata(DvObject dvObjectIn, Map<String, String> metadata) {

        String authorString = dvObjectIn.getAuthorString();
        if (authorString.isEmpty() || authorString.contains(DatasetField.NA_VALUE)) {
            authorString = UNAVAILABLE;
        }

        String producerString = pidProviderService.getProducer();

        if (producerString.isEmpty() || producerString.equals(DatasetField.NA_VALUE)) {
            producerString = UNAVAILABLE;
        }

        String titleString = dvObjectIn.getCurrentName();

        if (titleString.isEmpty() || titleString.equals(DatasetField.NA_VALUE)) {
            titleString = UNAVAILABLE;
        }

        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", titleString);
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", generateYear(dvObjectIn));
        return metadata;
    } 
    
    protected Map<String, String> addDOIMetadataForDestroyedDataset(DvObject dvObjectIn) {
        Map<String, String> metadata = new HashMap<>();
        String authorString = UNAVAILABLE;
        String producerString = UNAVAILABLE;
        String titleString = "This item has been removed from publication";

        metadata.put("datacite.creator", authorString);
        metadata.put("datacite.title", titleString);
        metadata.put("datacite.publisher", producerString);
        metadata.put("datacite.publicationyear", "9999");
        return metadata;
    } 

    protected String getTargetUrl(DvObject dvObjectIn) {
        logger.log(Level.FINE,"getTargetUrl");
        return SystemConfig.getDataverseSiteUrlStatic() + dvObjectIn.getTargetUrl() + dvObjectIn.getGlobalId().asString();
    }
    
    @Override
    public String getIdentifier(DvObject dvObject) {
        GlobalId gid = dvObject.getGlobalId();
        return gid != null ? gid.asString() : null;
    }

    protected String generateYear (DvObject dvObjectIn){
        return dvObjectIn.getYearPublishedCreated(); 
    }
    
    public Map<String, String> getMetadataForTargetURL(DvObject dvObject) {
        logger.log(Level.FINE,"getMetadataForTargetURL");
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("_target", getTargetUrl(dvObject));
        return metadata;
    }
    
    @Override
    public boolean alreadyRegistered(DvObject dvo) throws Exception {
        if(dvo==null) {
            logger.severe("Null DvObject sent to alreadyRegistered().");
            return false;
        }
        GlobalId globalId = dvo.getGlobalId();
        if(globalId == null) {
            return false;
        }
        return alreadyRegistered(globalId, false);
    }

    public abstract boolean alreadyRegistered(GlobalId globalId, boolean noProviderDefault) throws Exception;

    /*
     * ToDo: the DvObject being sent in provides partial support for the case where
     * it has a different authority/protocol than what is configured (i.e. a legacy
     * Pid that can actually be updated by the Pid account being used.) Removing
     * this now would potentially break/make it harder to handle that case prior to
     * support for configuring multiple Pid providers. Once that exists, it would be
     * cleaner to always find the PidProvider associated with the
     * protocol/authority/shoulder of the current dataset and then not pass the
     * DvObject as a param. (This would also remove calls to get the settings since
     * that would be done at construction.)
     */
    @Override
    public DvObject generateIdentifier(DvObject dvObject) {

        String protocol = dvObject.getProtocol() == null ? getProtocol() : dvObject.getProtocol();
        String authority = dvObject.getAuthority() == null ? getAuthority() : dvObject.getAuthority();
        if (dvObject.isInstanceofDataset()) {
            dvObject.setIdentifier(generateDatasetIdentifier((Dataset) dvObject));
        } else {
            dvObject.setIdentifier(generateDataFileIdentifier((DataFile) dvObject));
        }
        if (dvObject.getProtocol() == null) {
            dvObject.setProtocol(protocol);
        }
        if (dvObject.getAuthority() == null) {
            dvObject.setAuthority(authority);
        }
        return dvObject;
    }
    
    //ToDo just send the DvObject.DType
    public String generateDatasetIdentifier(Dataset dataset) {
        //ToDo - track these in the bean
        String shoulder = getShoulder();

        switch (getIdentifierGenerationStyle()) {
            case "randomString":
                return generateIdentifierAsRandomString(dataset, shoulder);
            case "storedProcGenerated":
                return generateIdentifierFromStoredProcedureIndependent(dataset, shoulder);
            default:
                /* Should we throw an exception instead?? -- L.A. 4.6.2 */
                return generateIdentifierAsRandomString(dataset, shoulder);
        }
    }


    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network) also check for duplicate
     * in EZID if needed
     * @param userIdentifier
     * @param dataset
     * @return {@code true} if the identifier is unique, {@code false} otherwise.
     */
    public boolean isGlobalIdUnique(GlobalId globalId) {
        if ( ! pidProviderService.isGlobalIdLocallyUnique(globalId)) {
            return false; // duplication found in local database
        }

        // not in local DB, look in the persistent identifier service
        try {
            return ! alreadyRegistered(globalId, false);
        } catch (Exception e){
            //we can live with failure - means identifier not found remotely
        }

        return true;
    }

    /** 
     *   Parse a Persistent Id and set the protocol, authority, and identifier
     * 
     *   Example 1: doi:10.5072/FK2/BYM3IW
     *       protocol: doi
     *       authority: 10.5072
     *       identifier: FK2/BYM3IW
     * 
     *   Example 2: hdl:1902.1/111012
     *       protocol: hdl
     *       authority: 1902.1
     *       identifier: 111012
     *
     * @param identifierString
     * @param separator the string that separates the authority from the identifier.
     * @param destination the global id that will contain the parsed data.
     * @return {@code destination}, after its fields have been updated, or
     *         {@code null} if parsing failed.
     */
    @Override
    public GlobalId parsePersistentId(String fullIdentifierString) {
        // Occasionally, the protocol separator character ':' comes in still
        // URL-encoded as %3A (usually as a result of the URL having been 
        // encoded twice):
        fullIdentifierString = fullIdentifierString.replace("%3A", ":");
        
        int index1 = fullIdentifierString.indexOf(':');
        if (index1 > 0) { // ':' found with one or more characters before it
            String protocol = fullIdentifierString.substring(0, index1);
            GlobalId globalId = parsePersistentId(protocol, fullIdentifierString.substring(index1+1));
            return globalId;
        }
        logger.log(Level.INFO, "Error parsing identifier: {0}: ''<protocol>:'' not found in string", fullIdentifierString);
        return null;
    }

    protected GlobalId parsePersistentId(String protocol, String identifierString) {
        String authority;
        String identifier;
        if (identifierString == null) {
            return null;
        }
        int index = identifierString.indexOf('/');
        if (index > 0 && (index + 1) < identifierString.length()) {
            // '/' found with one or more characters
            // before and after it
            // Strip any whitespace, ; and ' from authority (should finding them cause a
            // failure instead?)
            authority = PidProvider.formatIdentifierString(identifierString.substring(0, index));
            if (PidProvider.testforNullTerminator(authority)) {
                return null;
            }
            identifier = PidProvider.formatIdentifierString(identifierString.substring(index + 1));
            if (PidProvider.testforNullTerminator(identifier)) {
                return null;
            }
            ToDo - test specific authority/shoulder and against managed and excluded lists (should be maps?)
            add unmanagedPermaProvider?
        } else {
            logger.log(Level.INFO, "Error parsing identifier: {0}: '':<authority>/<identifier>'' not found in string",
                    identifierString);
            return null;
        }
        return parsePersistentId(protocol, authority, identifier);
    }
    
    public GlobalId parsePersistentId(String protocol, String authority, String identifier) {
        logger.fine("Parsing: " + protocol + ":" + authority + getSeparator() + identifier + " in " + getProviderInformation().get(0));
        if(!PidProvider.isValidGlobalId(protocol, authority, identifier)) {
            return null;
        }
        return new GlobalId(protocol, authority, identifier, getSeparator(), getUrlPrefix(),
                getProviderInformation().get(0));
    }

    
    public String getSeparator() {
        //The standard default
        return "/";
    }

    @Override
    public String generateDataFileIdentifier(DataFile datafile) {
        String doiDataFileFormat = getDatafilePidFormat();
        
        String prepend = "";
        if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.DEPENDENT.toString())){
            //If format is dependent then pre-pend the dataset identifier 
            prepend = datafile.getOwner().getIdentifier() + "/";
            datafile.setProtocol(datafile.getOwner().getProtocol());
            datafile.setAuthority(datafile.getOwner().getAuthority());
        } else {
            //If there's a shoulder prepend independent identifiers with it
            prepend = getShoulder();
            datafile.setProtocol(getProtocol());
            datafile.setAuthority(getAuthority());
        }
 
        switch (getIdentifierGenerationStyle()) {
            case "randomString":
                return generateIdentifierAsRandomString(datafile, prepend);
            case "storedProcGenerated":
                if (doiDataFileFormat.equals(SystemConfig.DataFilePIDFormat.INDEPENDENT.toString())){ 
                    return generateIdentifierFromStoredProcedureIndependent(datafile, prepend);
                } else {
                    return generateIdentifierFromStoredProcedureDependent(datafile, prepend);
                }
            default:
                /* Should we throw an exception instead?? -- L.A. 4.6.2 */
                return generateIdentifierAsRandomString(datafile, prepend);
        }
    }
    

    /*
     * This method checks locally for a DvObject with the same PID and if that is OK, checks with the PID service.
     * @param dvo - the object to check (ToDo - get protocol/authority from this PidProvider object)
     * @param prepend - for Datasets, this is always the shoulder, for DataFiles, it could be the shoulder or the parent Dataset identifier
     */
    private String generateIdentifierAsRandomString(DvObject dvo, String prepend) {
        String identifier = null;
        do {
            identifier = prepend + RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (!isGlobalIdUnique(new GlobalId(dvo.getProtocol(), dvo.getAuthority(), identifier, this.getSeparator(), this.getUrlPrefix(), this.getProviderInformation().get(0))));

        return identifier;
    }

    /*
     * This method checks locally for a DvObject with the same PID and if that is OK, checks with the PID service.
     * @param dvo - the object to check (ToDo - get protocol/authority from this PidProvider object)
     * @param prepend - for Datasets, this is always the shoulder, for DataFiles, it could be the shoulder or the parent Dataset identifier
     */

    private String generateIdentifierFromStoredProcedureIndependent(DvObject dvo, String prepend) {
        String identifier; 
        do {
            String identifierFromStoredProcedure = pidProviderService.generateNewIdentifierByStoredProcedure();
            // some diagnostics here maybe - is it possible to determine that it's failing 
            // because the stored procedure hasn't been created in the database?
            if (identifierFromStoredProcedure == null) {
                return null; 
            }
            identifier = prepend + identifierFromStoredProcedure;
        } while (!isGlobalIdUnique(new GlobalId(dvo.getProtocol(), dvo.getAuthority(), identifier, this.getSeparator(), this.getUrlPrefix(), this.getProviderInformation().get(0))));
        
        return identifier;
    }
    
    /*This method is only used for DataFiles with DEPENDENT Pids. It is not for Datasets
     * 
     */
    private String generateIdentifierFromStoredProcedureDependent(DataFile datafile, String prepend) {
        String identifier;
        Long retVal;
        retVal = Long.valueOf(0L);
      //ToDo - replace loops with one lookup for largest entry? (the do loop runs ~n**2/2 calls). The check for existingIdentifiers means this is mostly a local loop now, versus involving db or PidProvider calls, but still...)
        
        // This will catch identifiers already assigned in the current transaction (e.g.
        // in FinalizeDatasetPublicationCommand) that haven't been committed to the db
        // without having to make a call to the PIDProvider
        Set<String> existingIdentifiers = new HashSet<String>();
        List<DataFile> files = datafile.getOwner().getFiles();
        for(DataFile f:files) {
            existingIdentifiers.add(f.getIdentifier());
        }
        
        do {
            retVal++;
            identifier = prepend + retVal.toString();

        } while (existingIdentifiers.contains(identifier) || !isGlobalIdUnique(new GlobalId(datafile.getProtocol(), datafile.getAuthority(), identifier, this.getSeparator(), this.getUrlPrefix(), this.getProviderInformation().get(0))));

        return identifier;
    }

    
    public class GlobalIdMetadataTemplate {


    private   String template;

    public GlobalIdMetadataTemplate(){
        try (InputStream in = GlobalIdMetadataTemplate.class.getResourceAsStream("datacite_metadata_template.xml")) {
            template = new String(in.readAllBytes(),StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "datacite metadata template load error");
            logger.log(Level.SEVERE, "String " + e.toString());
            logger.log(Level.SEVERE, "localized message " + e.getLocalizedMessage());
            logger.log(Level.SEVERE, "cause " + e.getCause());
            logger.log(Level.SEVERE, "message " + e.getMessage());
        }
    }

    private String xmlMetadata;
    private String identifier;
    private List<String> datafileIdentifiers;
    private List<String> creators;
    private String title;
    private String publisher;
    private String publisherYear;
    private List<DatasetAuthor> authors;
    private String description;
    private List<String[]> contacts;
    private List<String[]> producers;

    public List<String[]> getProducers() {
        return producers;
    }

    public void setProducers(List<String[]> producers) {
        this.producers = producers;
    }

    public List<String[]> getContacts() {
        return contacts;
    }

    public void setContacts(List<String[]> contacts) {
        this.contacts = contacts;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DatasetAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<DatasetAuthor> authors) {
        this.authors = authors;
    }


    public List<String> getDatafileIdentifiers() {
        return datafileIdentifiers;
    }

    public void setDatafileIdentifiers(List<String> datafileIdentifiers) {
        this.datafileIdentifiers = datafileIdentifiers;
    }

    public GlobalIdMetadataTemplate(String xmlMetaData) {
        this.xmlMetadata = xmlMetaData;
        Document doc = Jsoup.parseBodyFragment(xmlMetaData);
        Elements identifierElements = doc.select("identifier");
        if (identifierElements.size() > 0) {
            identifier = identifierElements.get(0).html();
        }
        Elements creatorElements = doc.select("creatorName");
        creators = new ArrayList<>();
        for (Element creatorElement : creatorElements) {
            creators.add(creatorElement.html());
        }
        Elements titleElements = doc.select("title");
        if (titleElements.size() > 0) {
            title = titleElements.get(0).html();
        }
        Elements publisherElements = doc.select("publisher");
        if (publisherElements.size() > 0) {
            publisher = publisherElements.get(0).html();
        }
        Elements publisherYearElements = doc.select("publicationYear");
        if (publisherYearElements.size() > 0) {
            publisherYear = publisherYearElements.get(0).html();
        }
    }

    public String generateXML(DvObject dvObject) {
        // Can't use "UNKNOWN" here because DataCite will respond with "[facet 'pattern'] the value 'unknown' is not accepted by the pattern '[\d]{4}'"
        String publisherYearFinal = "9999";
        // FIXME: Investigate why this.publisherYear is sometimes null now that pull request #4606 has been merged.
        if (this.publisherYear != null) {
            // Added to prevent a NullPointerException when trying to destroy datasets when using DataCite rather than EZID.
            publisherYearFinal = this.publisherYear;
        }
        xmlMetadata = template.replace("${identifier}", getIdentifier().trim())
                .replace("${title}", this.title)
                .replace("${publisher}", this.publisher)
                .replace("${publisherYear}", publisherYearFinal)
                .replace("${description}", this.description);
        StringBuilder creatorsElement = new StringBuilder();
        for (DatasetAuthor author : authors) {
            creatorsElement.append("<creator><creatorName>");
            creatorsElement.append(author.getName().getDisplayValue());
            creatorsElement.append("</creatorName>");

            if (author.getIdType() != null && author.getIdValue() != null && !author.getIdType().isEmpty() && !author.getIdValue().isEmpty() && author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {

                if (author.getIdType().equals("ORCID")) {
                    creatorsElement.append("<nameIdentifier schemeURI=\"https://orcid.org/\" nameIdentifierScheme=\"ORCID\">" + author.getIdValue() + "</nameIdentifier>");
                }
                if (author.getIdType().equals("ISNI")) {
                    creatorsElement.append("<nameIdentifier schemeURI=\"http://isni.org/isni/\" nameIdentifierScheme=\"ISNI\">" + author.getIdValue() + "</nameIdentifier>");
                }
                if (author.getIdType().equals("LCNA")) {
                    creatorsElement.append("<nameIdentifier schemeURI=\"http://id.loc.gov/authorities/names/\" nameIdentifierScheme=\"LCNA\">" + author.getIdValue() + "</nameIdentifier>");
                }
            }
            if (author.getAffiliation() != null && !author.getAffiliation().getDisplayValue().isEmpty()) {
                creatorsElement.append("<affiliation>" + author.getAffiliation().getDisplayValue() + "</affiliation>");
            }
            creatorsElement.append("</creator>");
        }
        xmlMetadata = xmlMetadata.replace("${creators}", creatorsElement.toString());

        StringBuilder contributorsElement = new StringBuilder();
        for (String[] contact : this.getContacts()) {
            if (!contact[0].isEmpty()) {
                contributorsElement.append("<contributor contributorType=\"ContactPerson\"><contributorName>" + contact[0] + "</contributorName>");
                if (!contact[1].isEmpty()) {
                    contributorsElement.append("<affiliation>" + contact[1] + "</affiliation>");
                }
                contributorsElement.append("</contributor>");
            }
        }
        for (String[] producer : this.getProducers()) {
            contributorsElement.append("<contributor contributorType=\"Producer\"><contributorName>" + producer[0] + "</contributorName>");
            if (!producer[1].isEmpty()) {
                contributorsElement.append("<affiliation>" + producer[1] + "</affiliation>");
            }
            contributorsElement.append("</contributor>");
        }

        String relIdentifiers = generateRelatedIdentifiers(dvObject);

        xmlMetadata = xmlMetadata.replace("${relatedIdentifiers}", relIdentifiers);

        xmlMetadata = xmlMetadata.replace("{$contributors}", contributorsElement.toString());
        return xmlMetadata;
    }

    private String generateRelatedIdentifiers(DvObject dvObject) {

        StringBuilder sb = new StringBuilder();
        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;
            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    if (!dataFile.getGlobalId().asString().isEmpty()) {
                        if (sb.toString().isEmpty()) {
                            sb.append("<relatedIdentifiers>");
                        }
                        sb.append("<relatedIdentifier relatedIdentifierType=\"DOI\" relationType=\"HasPart\">" + dataFile.getGlobalId() + "</relatedIdentifier>");
                    }
                }

                if (!sb.toString().isEmpty()) {
                    sb.append("</relatedIdentifiers>");
                }
            }
        } else if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            sb.append("<relatedIdentifiers>");
            sb.append("<relatedIdentifier relatedIdentifierType=\"DOI\" relationType=\"IsPartOf\""
                    + ">" + df.getOwner().getGlobalId() + "</relatedIdentifier>");
            sb.append("</relatedIdentifiers>");
        }
        return sb.toString();
    }

    public void generateFileIdentifiers(DvObject dvObject) {

        if (dvObject.isInstanceofDataset()) {
            Dataset dataset = (Dataset) dvObject;

            if (!dataset.getFiles().isEmpty() && !(dataset.getFiles().get(0).getIdentifier() == null)) {

                datafileIdentifiers = new ArrayList<>();
                for (DataFile dataFile : dataset.getFiles()) {
                    datafileIdentifiers.add(dataFile.getIdentifier());
                    int x = xmlMetadata.indexOf("</relatedIdentifiers>") - 1;
                    xmlMetadata = xmlMetadata.replace("{relatedIdentifier}", dataFile.getIdentifier());
                    xmlMetadata = xmlMetadata.substring(0, x) + "<relatedIdentifier relatedIdentifierType=\"hasPart\" "
                            + "relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>" + template.substring(x, template.length() - 1);

                }

            } else {
                xmlMetadata = xmlMetadata.replace("<relatedIdentifier relatedIdentifierType=\"hasPart\" relationType=\"doi\">${relatedIdentifier}</relatedIdentifier>", "");
            }
        }
    }

    public  String getTemplate() {
        return template;
    }

    public  void setTemplate(String templateIn) {
        template = templateIn;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getCreators() {
        return creators;
    }

    public void setCreators(List<String> creators) {
        this.creators = creators;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublisherYear() {
        return publisherYear;
    }

    public void setPublisherYear(String publisherYear) {
        this.publisherYear = publisherYear;
    }
}
    public String getMetadataFromDvObject(String identifier, Map<String, String> metadata, DvObject dvObject) {

        Dataset dataset = null;

        if (dvObject instanceof Dataset) {
            dataset = (Dataset) dvObject;
        } else {
            dataset = (Dataset) dvObject.getOwner();
        }

        GlobalIdMetadataTemplate metadataTemplate = new GlobalIdMetadataTemplate();
        metadataTemplate.setIdentifier(identifier.substring(identifier.indexOf(':') + 1));
        metadataTemplate.setCreators(Arrays.asList(metadata.get("datacite.creator").split("; ")));
        metadataTemplate.setAuthors(dataset.getLatestVersion().getDatasetAuthors());
        if (dvObject.isInstanceofDataset()) {
            metadataTemplate.setDescription(dataset.getLatestVersion().getDescriptionPlainText());
        }
        if (dvObject.isInstanceofDataFile()) {
            DataFile df = (DataFile) dvObject;
            String fileDescription = df.getDescription();
            metadataTemplate.setDescription(fileDescription == null ? "" : fileDescription);
        }

        metadataTemplate.setContacts(dataset.getLatestVersion().getDatasetContacts());
        metadataTemplate.setProducers(dataset.getLatestVersion().getDatasetProducers());
        metadataTemplate.setTitle(dvObject.getCurrentName());
        String producerString = pidProviderService.getProducer();
        if (producerString.isEmpty()  || producerString.equals(DatasetField.NA_VALUE) ) {
            producerString = UNAVAILABLE;
        }
        metadataTemplate.setPublisher(producerString);
        metadataTemplate.setPublisherYear(metadata.get("datacite.publicationyear"));

        String xmlMetadata = metadataTemplate.generateXML(dvObject);
        logger.log(Level.FINE, "XML to send to DataCite: {0}", xmlMetadata);
        return xmlMetadata;
    }

    @Override
    public boolean canManagePID() {
        //The default expectation is that PID providers are configured to manage some set (i.e. based on protocol/authority/shoulder) of PIDs
        return true;
    }
    
    @Override
    public void setPidProviderServiceBean(PidProviderFactoryBean pidProviderServiceBean) {
        this.pidProviderService = pidProviderServiceBean;
    }
    
    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getShoulder() {
        return shoulder;
    }

    @Override
    public String getIdentifierGenerationStyle() {
        return identifierGenerationStyle;
    }

    public String getDatafilePidFormat() {
        return datafilePidFormat;
    }

    public List<String> getManagedList() {
        return managedList;
    }

    public List<String> getExcludedList() {
        return excludedList;
    }
    
}
