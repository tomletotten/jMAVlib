package me.drton.jmavlib.mavlink;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkSchema {
    private final MAVLinkMessageDefinition[] definitions = new MAVLinkMessageDefinition[256];
    private final Map<String, MAVLinkMessageDefinition> definitionsByName
            = new HashMap<String, MAVLinkMessageDefinition>();
    private DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    public MAVLinkSchema(String xmlFileName) throws ParserConfigurationException, IOException, SAXException {
        processXMLFile(xmlFileName);
    }

    private void processXMLFile(String xmlFileName) throws IOException, SAXException, ParserConfigurationException {
        File xmlFile = new File(xmlFileName);
        xmlBuilder.reset();
        Document doc = xmlBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        if (!root.getNodeName().equals("mavlink")) {
            throw new RuntimeException("Root element is not <mavlink>");
        }

        // Process includes
        NodeList includeElems = root.getElementsByTagName("include");
        for (int i = 0; i < includeElems.getLength(); i++) {
            String includeFile = includeElems.item(i).getTextContent();
            processXMLFile(new File(xmlFile.getParentFile(), includeFile).getPath());
        }

        NodeList msgElems = ((Element) root.getElementsByTagName("messages").item(0)).getElementsByTagName("message");
        for (int i = 0; i < msgElems.getLength(); i++) {
            Element msg = (Element) msgElems.item(i);
            int msgID = Integer.parseInt(msg.getAttribute("id"));
            String msgName = msg.getAttribute("name");
            NodeList fieldsElems = msg.getElementsByTagName("field");
            MAVLinkField[] fields = new MAVLinkField[fieldsElems.getLength()];
            int offset = MAVLinkMessage.DATA_OFFSET;
            for (int j = 0; j < fieldsElems.getLength(); j++) {
                Element fieldElem = (Element) fieldsElems.item(j);
                String[] typeStr = fieldElem.getAttribute("type").split("\\[");
                MAVLinkDataType fieldType = MAVLinkDataType.fromCType(typeStr[0]);
                int arraySize = 1;
                if (typeStr.length > 1) {
                    arraySize = Integer.parseInt(typeStr[1].split("\\]")[0]);
                }
                MAVLinkField field = new MAVLinkField(fieldType, arraySize, fieldElem.getAttribute("name"), offset);
                fields[j] = field;
                offset += field.size;
            }
            addMessageDefinition(new MAVLinkMessageDefinition(msgID, msgName, fields));
        }
    }

    public MAVLinkMessageDefinition getMessageDefinition(int msgID) {
        return definitions[msgID];
    }

    public MAVLinkMessageDefinition getMessageDefinition(String msgName) {
        return definitionsByName.get(msgName);
    }

    public void addMessageDefinition(MAVLinkMessageDefinition definition) {
        definitions[definition.id] = definition;
        definitionsByName.put(definition.name, definition);
    }
}
