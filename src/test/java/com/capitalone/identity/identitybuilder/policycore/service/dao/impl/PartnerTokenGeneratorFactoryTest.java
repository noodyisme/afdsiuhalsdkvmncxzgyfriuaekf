package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;

@ExtendWith(MockitoExtension.class)
public class PartnerTokenGeneratorFactoryTest {

    @Mock
    private DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;

    @InjectMocks
    private PartnerTokenGeneratorFactory factory;

    @Test
    public void testGenerateTokenSameServiceName() throws IOException, URISyntaxException {
        PartnerTokenGenerator tokenGenerator = factory.getPartnerTokenGenerator("Partner_Token");
        Assertions.assertNotNull(tokenGenerator,"Token Generator should not be null");
        PartnerTokenGenerator tokenGenerator2 = factory.getPartnerTokenGenerator("Partner_Token");
        Assertions.assertEquals(tokenGenerator, tokenGenerator2, "Both token generators should be same.");
    }

    @Test
    public void testGenerateTokenDifferentServiceName() throws IOException, URISyntaxException {
        PartnerTokenGenerator tokenGenerator = factory.getPartnerTokenGenerator("Partner_Token");
        Assertions.assertNotNull(tokenGenerator,"Token Generator should not be null");
        PartnerTokenGenerator tokenGenerator2 = factory.getPartnerTokenGenerator("Partner_Token_2");
        Assertions.assertNotEquals(tokenGenerator, tokenGenerator2, "Both token generators should not be same.");
    }
}
