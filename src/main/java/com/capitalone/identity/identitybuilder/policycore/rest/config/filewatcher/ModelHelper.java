package com.capitalone.identity.identitybuilder.policycore.rest.config.filewatcher;

import org.apache.camel.CamelContext;
import org.apache.camel.Expression;
import org.apache.camel.model.Constants;
import org.apache.camel.model.ExpressionNode;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.spi.NamespaceAware;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.util.ObjectHelper;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.camel.model.ProcessorDefinitionHelper.filterTypeInOutputs;

public class ModelHelper {

    private ModelHelper() {
    }

    /**
     * Marshal the xml to the model definition
     *
     * @param context the CamelContext, if <tt>null</tt> then {@link org.apache.camel.spi.ModelJAXBContextFactory} is not in use
     * @param node the xml node
     * @throws Exception is thrown if an error is encountered unmarshalling from xml to model
     */
    public static RoutesDefinition loadRoutesDefinition(CamelContext context, Node node) throws Exception {
        JAXBContext jaxbContext = getJAXBContext(context);

        Map<String, String> namespaces = new LinkedHashMap<>();

        Document dom = node instanceof Document ? (Document) node : node.getOwnerDocument();
        extractNamespaces(dom, namespaces);

        Binder<Node> binder = jaxbContext.createBinder();
        Object result = binder.unmarshal(node);

        if (result == null) {
            throw new JAXBException("Cannot unmarshal to RoutesDefinition using JAXB");
        }

        // can either be routes or a single route
        RoutesDefinition answer;
        if (result instanceof RouteDefinition) {
            RouteDefinition route = (RouteDefinition) result;
            answer = new RoutesDefinition();
            applyNamespaces(route, namespaces);
            answer.getRoutes().add(route);
        } else if (result instanceof RoutesDefinition) {
            answer = (RoutesDefinition) result;
            for (RouteDefinition route : answer.getRoutes()) {
                applyNamespaces(route, namespaces);
            }
        } else {
            throw new IllegalArgumentException("Unmarshalled object is an unsupported type: " + ObjectHelper.className(result) + " -> " + result);
        }

        return answer;
    }

    private static JAXBContext getJAXBContext(CamelContext context) throws Exception {
        JAXBContext jaxbContext;
        if (context == null) {
            jaxbContext = createJAXBContext();
        } else {
            jaxbContext = (JAXBContext) context.adapt(SpringCamelContext.class).getModelJAXBContextFactory().newJAXBContext();
        }
        return jaxbContext;
    }

    private static JAXBContext createJAXBContext() throws JAXBException {
        // must use classloader from CamelContext to have JAXB working
        return JAXBContext.newInstance(Constants.JAXB_CONTEXT_PACKAGES, CamelContext.class.getClassLoader());
    }

    private static void applyNamespaces(RouteDefinition route, Map<String, String> namespaces) {
        filterTypeInOutputs(route.getOutputs(), ExpressionNode.class).forEach(expressionNode -> {
            NamespaceAware na = getNamespaceAwareFromExpression(expressionNode);
            if (na != null) {
                na.setNamespaces(namespaces);
            }
        });
    }

    private static NamespaceAware getNamespaceAwareFromExpression(ExpressionNode expressionNode) {
        ExpressionDefinition ed = expressionNode.getExpression();

        NamespaceAware na = null;
        Expression exp = ed.getExpressionValue();
        if (exp instanceof NamespaceAware) {
            na = (NamespaceAware) exp;
        } else if (ed instanceof NamespaceAware) {
            na = (NamespaceAware) ed;
        }

        return na;
    }

    /**
     * Extract all XML namespaces from the root element in a DOM Document
     *
     * @param document    the DOM document
     * @param namespaces  the map of namespaces to add new found XML namespaces
     */
    private static void extractNamespaces(Document document, Map<String, String> namespaces) throws JAXBException {
        NamedNodeMap attributes = document.getDocumentElement().getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node item = attributes.item(i);
            String nsPrefix = item.getNodeName();
            if (nsPrefix != null && nsPrefix.startsWith("xmlns")) {
                String nsValue = item.getNodeValue();
                String[] nsParts = nsPrefix.split(":");
                if (nsParts.length == 1) {
                    namespaces.put(nsParts[0], nsValue);
                } else if (nsParts.length == 2) {
                    namespaces.put(nsParts[1], nsValue);
                } else {
                    // Fallback on adding the namespace prefix as we find it
                    namespaces.put(nsPrefix, nsValue);
                }
            }
        }
    }

}
