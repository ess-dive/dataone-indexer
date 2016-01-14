package org.dataone.cn.indexer.convert;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "../../index/test-context.xml" })
public class MemberNodeServiceRegistrationTypeDocumentServiceTest {

    @Autowired
    MemberNodeServiceRegistrationTypeDocumentService serviceTypeDocService;

    public MemberNodeServiceRegistrationTypeDocumentServiceTest() {
    }

    @Test
    public void testInjection() {
        Assert.assertNotNull(serviceTypeDocService);
        Assert.assertNotNull(serviceTypeDocService.getServiceTypeDocUrl());
        Assert.assertNotNull(serviceTypeDocService.getHttpClientFactory());
    }

    @Test
    public void testGetDocument() {
        Document doc = serviceTypeDocService.getMemberNodeServiceRegistrationTypeDocument();
        Assert.assertNotNull(doc);
    }

}