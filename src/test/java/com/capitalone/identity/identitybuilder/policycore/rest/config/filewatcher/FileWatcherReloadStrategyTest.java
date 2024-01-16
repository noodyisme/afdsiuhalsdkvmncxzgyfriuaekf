package com.capitalone.identity.identitybuilder.policycore.rest.config.filewatcher;

import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.apache.camel.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.capitalone.identity.identitybuilder.policycore.utils.TestUtils.createDirectory;
import static com.capitalone.identity.identitybuilder.policycore.utils.TestUtils.mockEndpoint;
import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@Disabled
@CamelSpringBootTest
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = FileWatcherReloadStrategyTest.ContextConfig.class)
@UseAdviceWith
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FileWatcherReloadStrategyTest {

  private static Logger log = LogManager.getLogger(FileWatcherReloadStrategyTest.class);

  @Autowired
  private CamelContext context;

  @Autowired
  private ProducerTemplate template;

  @BeforeEach
  public void init() throws IOException {
      context.start();
    deleteDirectory("target/file-watcher-test");
    createDirectory("target/file-watcher-test");
  }

  @AfterAll
  public static void close() throws IOException {
    deleteDirectory("target/file-watcher-test");
  }

  @Test
  public void testAddNewRoute() throws Exception {

    context.start();

    // there are 0 routes to begin with
    assertEquals(0, context.getRoutes().size());

    // create an xml file with some routes
    FileUtil.copyFile(new File("src/test/resources/test/testRoute.xml"), new File("target/file-watcher-test/testRoute.xml"));

    // wait for that file to be processed
    // (is slow on osx, so wait up till 20 seconds)
    await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> assertEquals(1, context.getRoutes().size()));

    // and the route should work
    MockEndpoint mockEndpoint = mockEndpoint(context, "mock:bar");
    mockEndpoint.expectedMessageCount(1);
    template.sendBody("direct:bar", "Hello World");
    mockEndpoint.assertIsSatisfied();
  }

  @Test
  public void testUpdateExistingRoute() throws Exception {

    // the bar route is added two times, at first, and then when updated
    final CountDownLatch latch = new CountDownLatch(2);
    context.getManagementStrategy().addEventNotifier(new EventNotifierSupport() {
      @Override
      public void notify(CamelEvent event) throws Exception {
        latch.countDown();
      }

      @Override
      public boolean isEnabled(CamelEvent event) {
        log.info("Event occurred {} ", event);
        return event instanceof CamelEvent.RouteAddedEvent;
      }
    });

    context.addRoutes(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from("direct:bar").routeId("bar").to("mock:foo");
      }
    });

    context.start();

    assertEquals(1, context.getRoutes().size());

    // and the route should work sending to mock:foo
    MockEndpoint mockBar = mockEndpoint(context, "mock:bar");
    mockBar.expectedMessageCount(0);

    MockEndpoint mockFoo = mockEndpoint(context, "mock:foo");
    mockFoo.expectedMessageCount(1);

    template.sendBody("direct:bar", "Hello World");
    mockBar.assertIsSatisfied();
    mockFoo.assertIsSatisfied();

    mockBar.reset();
    mockFoo.reset();

    // create an xml file with some routes
    FileUtil.copyFile(new File("src/test/resources/test/testRoute.xml"), new File("target/file-watcher-test/testRoute.xml"));

    // wait for that file to be processed and remove/add the route
    // (is slow on osx, so wait up till 20 seconds)
    boolean done = latch.await(20, TimeUnit.SECONDS);
    assertTrue("Should reload file within 20 seconds", done);

    // and the route should be changed to route to mock:bar instead of mock:foo
    await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertEquals("mock:bar",
            ((ToDefinition) context.adapt(ModelCamelContext.class)
                    .getRouteDefinitions().get(0).getOutputs().get(0)).getUri()));

    mockBar = mockEndpoint(context, "mock:bar");
    mockBar.expectedMessageCount(1);

    mockFoo = mockEndpoint(context,"mock:foo");
    mockFoo.expectedMessageCount(0);

    template.sendBody("direct:bar", "Bye World");
    mockBar.assertIsSatisfied();
    mockFoo.assertIsSatisfied();
  }

  @Test
  public void testUpdateXmlRoute() throws Exception {

    // the bar route is added two times, at first, and then when updated
    final CountDownLatch latch = new CountDownLatch(2);
    context.getManagementStrategy().addEventNotifier(new EventNotifierSupport() {
      @Override
      public void notify(CamelEvent event) throws Exception {
        latch.countDown();
      }

      @Override
      public boolean isEnabled(CamelEvent event) {
        log.info("Event occurred {} ", event);
        return event instanceof CamelEvent.RouteAddedEvent;
      }
    });

    context.start();

    // there are 0 routes to begin with
    assertEquals(0, context.getRoutes().size());

    // create an xml file with some routes
    FileUtil.copyFile(new File("src/test/resources/test/testRoute.xml"), new File("target/file-watcher-test/testRoute.xml"));

    // wait for that file to be processed
    // (is slow on osx, so wait up till 20 seconds)
    await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> assertEquals(1, context.getRoutes().size()));

    // and the route should work
    MockEndpoint mockBar = mockEndpoint(context, "mock:bar");
    mockBar.expectedMessageCount(1);
    template.sendBody("direct:bar", "Hello World");
    mockBar.assertIsSatisfied();

    mockBar.reset();

    context.adapt(ModelCamelContext.class).getRouteDefinitions().get(0).routeDescription("initialRoute");

    // create an xml file with some routes
    FileUtil.copyFile(new File("src/test/resources/test/testUpdatedRoute.xml"), new File("target/file-watcher-test/testRoute.xml"));

    // wait for that file to be processed and remove/add the route
    // (is slow on osx, so wait up till 20 seconds)
    boolean done = latch.await(20, TimeUnit.SECONDS);
    assertTrue("Should reload file within 20 seconds", done);

    // and the route should work with the update
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertNull(
            context.adapt(ModelCamelContext.class).getRouteDefinitions().get(0).getDescription()));

    mockBar = mockEndpoint(context,"mock:bar");
    mockBar.expectedBodiesReceived("Bye Camel");
    template.sendBody("direct:bar", "Camel");
    mockBar.assertIsSatisfied();
  }

  @Configuration
  @EnableAutoConfiguration
  public static class ContextConfig {

    private FileWatcherReloadStrategy reloadStrategy;

    @SneakyThrows
    @Bean
    protected CamelContext createCamelContext() {
        CamelContext context = new DefaultCamelContext();
      reloadStrategy = new FileWatcherReloadStrategy();
      reloadStrategy.setFolder("target/file-watcher-test");
      // to make unit test faster
      reloadStrategy.setPollTimeout(100);
      context.addService(reloadStrategy, true, true);
      context.adapt(ModelCamelContext.class)
              .setDataFormats(Collections.singletonMap("jackson", new JsonDataFormat()));
      context.build();
      return context;
    }

      @Bean
      protected ProducerTemplate producerTemplate(CamelContext camelContext) {
          return camelContext.createProducerTemplate();
      }
  }
}
