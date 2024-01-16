package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicPolicyHelperTest {

  @Mock
  private CamelContext context;
  @Mock
  private XMLRoutesDefinitionLoader xmlRoutesDefinitionLoader;
  @Mock
  private ExtendedCamelContext extendedCamelContext;
  @Mock
  private ModelCamelContext modelCamelContext;
  @Mock
  private RoutesDefinition routesDefinition;
  @Mock
  private RouteDefinition routeDefinition;
  @Mock
  private RouteBuilder routeBuilder;
  private InputStream is;

  @Test
  void loadRouteDefinitions_withEmptyRouteDefinitions() throws Exception {
    is = new ByteArrayInputStream("Test".getBytes());
    when(context.adapt(ExtendedCamelContext.class)).thenReturn(extendedCamelContext);
    when(extendedCamelContext.getXMLRoutesDefinitionLoader()).thenReturn(xmlRoutesDefinitionLoader);
    when(xmlRoutesDefinitionLoader.loadRoutesDefinition(context, is)).thenReturn(Optional.empty());
    assertThrows(ChassisSystemException.class, () -> DynamicPolicyHelper.loadRouteDefinitions(context, is));
  }


  @Test
  void loadRouteDefinitions_withRouteDefinitions() throws Exception {
    is = new ByteArrayInputStream("Test".getBytes());
    when(context.adapt(ExtendedCamelContext.class)).thenReturn(extendedCamelContext);
    when(extendedCamelContext.getXMLRoutesDefinitionLoader()).thenReturn(xmlRoutesDefinitionLoader);
    when(xmlRoutesDefinitionLoader.loadRoutesDefinition(context, is)).thenReturn(Optional.of(routesDefinition));
    RoutesDefinition actual = DynamicPolicyHelper.loadRouteDefinitions(context, is);
    assertNotNull(actual);
  }

  @Test
  void loadRouteDefinitionsIntoCamelContext_withEmptyRouteDefinitions() throws Exception {
    is = new ByteArrayInputStream("Test".getBytes());
    when(context.adapt(ExtendedCamelContext.class)).thenReturn(extendedCamelContext);
    when(extendedCamelContext.getXMLRoutesDefinitionLoader()).thenReturn(xmlRoutesDefinitionLoader);
    when(xmlRoutesDefinitionLoader.loadRoutesDefinition(context, is)).thenReturn(Optional.empty());
    assertThrows(ChassisSystemException.class, () -> DynamicPolicyHelper.loadRouteDefinitionsIntoCamelContext(context, is));
  }

  @Test
  void loadRouteDefinitionsIntoCamelContext_withRouteDefinitions() throws Exception {
    is = new ByteArrayInputStream("Test".getBytes());
    when(context.adapt(ExtendedCamelContext.class)).thenReturn(extendedCamelContext);
    when(context.adapt(ModelCamelContext.class)).thenReturn(modelCamelContext);
    when(extendedCamelContext.getXMLRoutesDefinitionLoader()).thenReturn(xmlRoutesDefinitionLoader);
    when(routesDefinition.getRoutes()).thenReturn(singletonList(routeDefinition));
    when(xmlRoutesDefinitionLoader.loadRoutesDefinition(context, is)).thenReturn(Optional.of(routesDefinition));
    RoutesDefinition actual = DynamicPolicyHelper.loadRouteDefinitionsIntoCamelContext(context, is);
    assertNotNull(actual);
    verify(routesDefinition, times(1)).getRoutes();
    verify(xmlRoutesDefinitionLoader, times(1)).loadRoutesDefinition(context, is);
  }

  @Test
  void loadRouteDefinitionsIntoCamelContext_throwException() throws Exception {
    is = new ByteArrayInputStream("Test".getBytes());
    when(context.adapt(ExtendedCamelContext.class)).thenReturn(extendedCamelContext);
    when(context.adapt(ModelCamelContext.class)).thenThrow(RuntimeException.class);
    when(extendedCamelContext.getXMLRoutesDefinitionLoader()).thenReturn(xmlRoutesDefinitionLoader);
    when(routesDefinition.getRoutes()).thenReturn(singletonList(routeDefinition));
    when(xmlRoutesDefinitionLoader.loadRoutesDefinition(context, is)).thenReturn(
        Optional.of(routesDefinition));
    assertThrows(ChassisSystemException.class, () -> DynamicPolicyHelper.loadRouteDefinitionsIntoCamelContext(context, is));
    verify(context, times(1)).adapt(ModelCamelContext.class);
  }

  @Test
  void addRoutes_validateContextRoutes() throws Exception {
    DynamicPolicyHelper.addRoutes(context, routeBuilder);
    verify(context,times(1)).addRoutes(routeBuilder);
  }
}