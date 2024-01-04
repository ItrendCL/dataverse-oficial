package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FakeDOIProvider extends DOIProvider {

    private static final Logger logger = Logger.getLogger(FakeDOIProvider.class.getCanonicalName());
    static final String TYPE = "FAKE";

    public FakeDOIProvider(String providerAuthority, String providerShoulder, String identifierGenerationStyle,
            String datafilePidFormat, String managedList, String excludedList) {
        super(providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat, managedList, excludedList);
    }

    //Only need to check locally
    public boolean isGlobalIdUnique(GlobalId globalId) {
        try {
            return ! alreadyRegistered(globalId, false);
        } catch (Exception e){
            //we can live with failure - means identifier not found remotely
        }
        return true;
    }
    
    @Override
    public boolean alreadyRegistered(GlobalId globalId, boolean noProviderDefault) {
        boolean existsLocally = !pidProviderService.isGlobalIdLocallyUnique(globalId);
        return existsLocally ? existsLocally : noProviderDefault;
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public List<String> getProviderInformation() {
        return List.of("FAKE", "https://dataverse.org");
    }

    @Override
    public String createIdentifier(DvObject dvo) throws Throwable {
        return "fakeIdentifier";
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvo) {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvo) throws Exception {
        return "fakeModifyIdentifierTargetURL";
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {
        // no-op
    }

    @Override
    public boolean publicizeIdentifier(DvObject studyIn) {
        return true;
    }
    
    @Override
    protected String getProviderKeyName() {
        return "FAKE";
    }

    @Override
    public String getProviderType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    public static String getType() {
        return TYPE;
    }

}
