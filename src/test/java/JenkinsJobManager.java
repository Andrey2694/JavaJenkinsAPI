
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class JenkinsJobManager {
    public static void changeChoiceParameter(String jobName, String key, String newValue) throws Exception {
        String jenkinsUrl = "http://localhost:8081";
        String username = "testApi";
        String password = "testApi";

        RestAssured.baseURI = jenkinsUrl;
        RestAssured.authentication = RestAssured.preemptive().basic(username, password);
        String configUrl = "/job/" + jobName + "/config.xml";

        // Get crumbs and cookies for sending any changes to Jenkins API
        String crumbUrl = "/crumbIssuer/api/json";
        Response crumbResponse = RestAssured.get(crumbUrl);
        Map<String, String> cookies = crumbResponse.getCookies();
        String crumb = crumbResponse.jsonPath().getString("crumb");
        String crumbField = crumbResponse.jsonPath().getString("crumbRequestField");

        // Get the xml
        Response response = RestAssured.get(configUrl);
        String jobConfig = response.getBody().asString();

        // Parse the XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(jobConfig)));

        // Find the element with the matching key
        NodeList nodeList = document.getElementsByTagName("name");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element nameElement = (Element) nodeList.item(i);
            String nameValue = nameElement.getTextContent();
            if (nameValue.equals(key)) {
                // Find the corresponding choices element
                Node choicesNode = nameElement.getNextSibling();
                while (choicesNode != null && choicesNode.getNodeType() != Node.ELEMENT_NODE) {
                    choicesNode = choicesNode.getNextSibling();
                }
                if (choicesNode != null && choicesNode.getNodeName().equals("choices")) {
                    NodeList stringNodes = ((Element) choicesNode).getElementsByTagName("string");
                    for (int j = 0; j < stringNodes.getLength(); j++) {
                        Element stringElement = (Element) stringNodes.item(j);
                        stringElement.setTextContent(newValue);
                    }
                    break;
                }
            }
        }
        String updatedXml = documentToString(document);

        // Send updated xml to Jenkins API
        RestAssured.given()
                .header("Content-Type", "text/xml")
                .header(crumbField, crumb)
                .cookies(cookies)
                .body(updatedXml)
                .post(configUrl);
    }

    private static String documentToString(Document document) throws Exception {
        TransformerFactory tf;
        tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}
