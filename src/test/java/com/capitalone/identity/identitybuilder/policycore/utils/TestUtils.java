package com.capitalone.identity.identitybuilder.policycore.utils;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;

import java.io.File;

/**
 * @author plz569
 *
 * Test utilities to simplify interactions with Camel Context
 */
public class TestUtils {

  public static final String DIRECT_START = "direct:start";
  public static final String DIRECT_END = "direct:end";
  public static final String TEST_ROUTE = "test-route";
  public static final String JWT = "crypto-jwt";
  public static final String PROFILE_REF_ID = "cof-profile-ref-id";
  public static final String PROCESSOR_NODE = "processorNode";


  public static MockEndpoint mockEndpoint(CamelContext camelContext, String mockEndpointUri) {
    return camelContext.getEndpoint(mockEndpointUri, MockEndpoint.class);
  }

  public static void mockEndpointAndSkip(CamelContext camelContext, String routeId, String mockEndpointUri) throws Exception {
    AdviceWith.adviceWith(camelContext, routeId, b ->
            b.mockEndpointsAndSkip(mockEndpointUri));
  }

  /**
   * Recursively delete a directory, useful to zapping test data
   *
   * @param file the directory to be deleted
   */
  public static void deleteDirectory(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File child : files) {
        deleteDirectory(child);
      }
    }

    file.delete();
  }

  /**
   * create the directory
   *
   * @param file the directory to be created
   */
  public static void createDirectory(String file) {
    File dir = new File(file);
    dir.mkdirs();
  }
}

