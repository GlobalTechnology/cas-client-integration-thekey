package me.thekey.cas.client.validation;

import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TheKeyProxyTicketValidator extends Cas20ProxyTicketValidator {
    private static final String XMLNS_CAS = "http://www.yale.edu/tp/cas";

    private DocumentBuilder xmlDocumentBuilder = null;
    private XPath xpathEngine = null;

    public TheKeyProxyTicketValidator(final String casServerUrlPrefix) {
        super(casServerUrlPrefix);
    }

    /**
     * @return the XmlDocumentBuilder
     * @throws ParserConfigurationException
     */
    private DocumentBuilder getXmlDocumentBuilder() throws ParserConfigurationException {
        // create an XML DocumentBuilder if one isn't already defined
        if (this.xmlDocumentBuilder == null) {
            final DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            this.xmlDocumentBuilder = xmlFactory.newDocumentBuilder();
        }

        return this.xmlDocumentBuilder;
    }

    /**
     * @return the xpathEngine
     */
    private XPath getXpathEngine() {
        // create the Xpath engine for use with the XML parser if one doesn't exist yet
        if (this.xpathEngine == null) {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(final String prefix) {
                    if (prefix == null) {
                        throw new IllegalArgumentException("No prefix provided");
                    } else if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
                        return XMLConstants.XML_NS_URI;
                    } else if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                        return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                    } else if (prefix.equals("cas")) {
                        return XMLNS_CAS;
                    } else {
                        return XMLConstants.NULL_NS_URI;
                    }
                }

                public String getPrefix(String arg0) {
                    return null;
                }

                public Iterator<?> getPrefixes(String arg0) {
                    return null;
                }

            });

            this.xpathEngine = xPath;
        }

        return this.xpathEngine;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jasig.cas.client.validation.Cas20ServiceTicketValidator#
     * extractCustomAttributes(java.lang.String)
     */
    @Override
    protected Map<String, Object> extractCustomAttributes(final String xml) {
        try {
            final Document dom = this.getXmlDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
            final XPath xpath = this.getXpathEngine();
            final NodeList attrList = (NodeList) xpath.evaluate(
                    "/cas:serviceResponse/cas:authenticationSuccess/cas:attributes/*", dom, XPathConstants.NODESET);

            if (attrList.getLength() > 0) {
                final HashMap<String, Object> attrs = new HashMap<>();

                for (int i = 0; i < attrList.getLength(); i++) {
                    final Node attrXml = attrList.item(i);
                    if (attrXml instanceof Element) {
                        attrs.put(attrXml.getLocalName(), attrXml.getTextContent());
                    }
                }

                return attrs;
            }
        } catch (final SAXException | IOException | ParserConfigurationException | XPathExpressionException ignored) {
        }

        return Collections.emptyMap();
    }
}
