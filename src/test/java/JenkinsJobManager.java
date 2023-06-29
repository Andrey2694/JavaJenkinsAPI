import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class JenkinsJobManager {
    // credentials for login to Jenkins, need to be encrypted and put to another place with all creds
    String jenkinsUrl = "http://localhost:8081";
    String username = "testApi";
    String password = "testApi";

    public void changeChoiceParameter(String jobName, String key, String newValue) throws Exception {
        RestAssured.baseURI = jenkinsUrl;
        RestAssured.authentication = RestAssured.preemptive().basic(username, password);
        String configUrl = "/job/" + jobName + "/config.xml";

        // Get crumbs and cookies for sending any changes to Jenkins API
        CrumbData crumbData = getCrumbData();

        // Get the xml
        Response response = RestAssured.get(configUrl);
        String jobConfig = response.getBody().asString();

        // Parse XML
        String updatedXml = updateChoiceParameterXml(jobConfig, key, newValue);

        // Send updated xml to Jenkins API
        sendUpdatedXml(crumbData, updatedXml, configUrl);
    }

    public void changeGitData(String jobName, String gitUrl, String credentialsId, String branchName) throws Exception {
        RestAssured.baseURI = jenkinsUrl;
        RestAssured.authentication = RestAssured.preemptive().basic(username, password);
        String configUrl = "/job/" + jobName + "/config.xml";

        // Get crumbs and cookies for sending any changes to Jenkins API
        CrumbData crumbData = getCrumbData();

        // Get the xml
        Response response = RestAssured.get(configUrl);
        String jobConfig = response.getBody().asString();

        // Parse XML
        String updatedXml = updateGitXml(jobConfig, gitUrl, credentialsId, branchName);

        // Send updated xml to Jenkins API
        sendUpdatedXml(crumbData, updatedXml, configUrl);
    }

    private void sendUpdatedXml(CrumbData crumbData, String updatedXml, String configUrl) {
        RestAssured.given()
                .header("Content-Type", "text/xml")
                .header(crumbData.getCrumbField(), crumbData.getCrumb())
                .cookies(crumbData.getCookies())
                .body(updatedXml)
                .post(configUrl);
    }

    private String documentToString(Document document) throws TransformerException {
        TransformerFactory tf;
        tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    private CrumbData getCrumbData() {
        String crumbUrl = "/crumbIssuer/api/json";
        Response crumbResponse = RestAssured.get(crumbUrl);

        Map<String, String> cookies = crumbResponse.getCookies();
        String crumb = crumbResponse.jsonPath().getString("crumb");
        String crumbField = crumbResponse.jsonPath().getString("crumbRequestField");

        return new CrumbData(cookies, crumb, crumbField);
    }

    private String updateGitXml(String jobConfig, String gitUrl, String credentialsId, String branchName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(jobConfig)));

        // Update the URL
        NodeList urlNodes = document.getElementsByTagName("url");
        if (urlNodes.getLength() > 0) {
            Element urlElement = (Element) urlNodes.item(0);
            urlElement.setTextContent(gitUrl);
        }

        // Update the credentialsId
        NodeList credentialsIdNodes = document.getElementsByTagName("credentialsId");
        if (credentialsIdNodes.getLength() > 0) {
            Element credentialsIdElement = (Element) credentialsIdNodes.item(0);
            credentialsIdElement.setTextContent(credentialsId);
        }

        // Update the branchName
        Element branchesElement = (Element) document.getElementsByTagName("branches").item(0);
        NodeList branchNameNode = branchesElement.getElementsByTagName("name");
        if (branchNameNode.getLength() > 0) {
            Element branchNameElement = (Element) branchNameNode.item(0);
            branchNameElement.setTextContent(branchName);
        }

        return documentToString(document);
    }

    private String updateChoiceParameterXml(String jobConfig, String key, String choiceParameter) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(jobConfig)));

        // Update the choiceParameter
        NodeList nodeList = document.getElementsByTagName("name");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element nameElement = (Element) nodeList.item(i);
            String nameValue = nameElement.getTextContent();
            if (nameValue.equals(key)) {
                Node choicesNode = nameElement.getNextSibling();
                while (choicesNode != null && choicesNode.getNodeType() != Node.ELEMENT_NODE) {
                    choicesNode = choicesNode.getNextSibling();
                }
                if (choicesNode != null && choicesNode.getNodeName().equals("choices")) {
                    NodeList stringNodes = ((Element) choicesNode).getElementsByTagName("string");
                    for (int j = 0; j < stringNodes.getLength(); j++) {
                        Element stringElement = (Element) stringNodes.item(j);
                        stringElement.setTextContent(choiceParameter);
                    }
                    break;
                }
            }
        }
        return documentToString(document);
    }

    private static class CrumbData {
        private final Map<String, String> cookies;
        private final String crumb;
        private final String crumbField;

        public CrumbData(Map<String, String> cookies, String crumb, String crumbField) {
            this.cookies = cookies;
            this.crumb = crumb;
            this.crumbField = crumbField;
        }

        public Map<String, String> getCookies() {
            return cookies;
        }

        public String getCrumb() {
            return crumb;
        }

        public String getCrumbField() {
            return crumbField;
        }
    }
}
