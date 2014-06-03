package me.drton.jmavlib.mavlink;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 03.06.14 Time: 12:31
 */
public class MAVLinkSchema {
    private final MAVLinkMessageDefinition[] definitions = new MAVLinkMessageDefinition[256];
    private final Map<String, MAVLinkMessageDefinition> definitionsByName = new HashMap<String, MAVLinkMessageDefinition>();

    public MAVLinkSchema() {
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
