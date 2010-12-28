package org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands;

/**
 * a command note
 */
public class Note {
    public static enum Type { info, warn, error }

    public static Note info(String text) {
        return new Note(Type.info, text);
    }
    
    public static Note warn(String text) {
        return new Note(Type.warn, text);
    }
    
    public static Note error(String text) {
        return new Note(Type.error, text);
    }
    
    protected Type type;
    protected String text;

    protected Note(Type type, String text) {
        this.type = type;
        this.text = text;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
