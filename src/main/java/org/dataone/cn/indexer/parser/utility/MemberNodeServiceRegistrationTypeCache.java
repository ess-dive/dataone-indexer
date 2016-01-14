package org.dataone.cn.indexer.parser.utility;

import java.util.ArrayList;
import java.util.Collection;

import org.dataone.cn.indexer.convert.MemberNodeServiceRegistrationType;
import org.dataone.cn.indexer.convert.MemberNodeServiceRegistrationTypeDocumentService;
import org.dataone.configuration.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

public class MemberNodeServiceRegistrationTypeCache {

    private static final int REFRESH_INTERVAL_MINUTES = Settings.getConfiguration().getInt("dataone.mn.registration.serviceType.cacheRefreshMinutes", 1);
    private static long lastRefreshTime = 0;
    private static Collection<MemberNodeServiceRegistrationType> serviceTypes = new ArrayList<MemberNodeServiceRegistrationType>();
    
    @Autowired
    MemberNodeServiceRegistrationTypeDocumentService mnServiceRegistrationTypeDocumentService;
    
    
    public Collection<MemberNodeServiceRegistrationType> getServiceTypes() {
        
        long refreshIntervalMillis = REFRESH_INTERVAL_MINUTES * 60 * 1000;
        long expectedRefreshTime = lastRefreshTime + refreshIntervalMillis;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime > expectedRefreshTime) {
            Document doc = mnServiceRegistrationTypeDocumentService.getMemberNodeServiceRegistrationTypeDocument();
            serviceTypes = MemberNodeServiceRegistrationTypesParser.parseServiceTypes(doc);
            lastRefreshTime = currentTime;
        }
        
        return serviceTypes;
    }

}