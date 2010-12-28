package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AbstractAdhocCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.Note;

import java.util.List;

/**
 */
public abstract class PasswordCheckingCommandHandler extends AbstractAdhocCommandHandler {
    protected boolean checkPassword(List<Note> notes, Entity accountjid, String password, String password2) {
        if (StringUtils.isBlank(password) || 
            password.equals(accountjid.getFullQualifiedName()) ||
            password.length() < 8) {
            notes.add(Note.error("password must have at least 8 chars and must not be the same as the new JID"));
            return false;
        }
        if (!password.equals(password2)) {
            notes.add(Note.error("passwords did not match"));
            return false;
        }
        return true;
    }
}
