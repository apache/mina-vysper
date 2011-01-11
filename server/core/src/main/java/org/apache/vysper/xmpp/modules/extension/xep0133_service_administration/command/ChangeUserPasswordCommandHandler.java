package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AbstractAdhocCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.Note;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormParser;
import org.apache.vysper.xmpp.stanza.dataforms.Field;

import java.util.List;
import java.util.Map;

/**
 */
public class ChangeUserPasswordCommandHandler extends PasswordCheckingCommandHandler {
    
    protected AccountManagement accountManagement;

    /**
     * if not NULL, the user will only be able this JID's (his own) password
     */
    protected Entity constrainedJID;

    public ChangeUserPasswordCommandHandler(AccountManagement accountManagement, Entity constrainedJID) {
        this.accountManagement = accountManagement;
        this.constrainedJID = constrainedJID;
    }

    public XMLElement process(List<XMLElement> commandElements, List<Note> notes) {
        if (commandElements == null || commandElements.size() == 0) {
            return sendForm();
        } else {
            return processForm(commandElements, notes);
        }
    }

    protected XMLElement sendForm() {
        final DataForm dataForm = createFormForm("Changing a User Password", "Fill out this form to change a user&apos;s password.");
        if (constrainedJID == null) {
            dataForm.addField(new Field("The Jabber ID whose password will be changed.", Field.Type.JID_SINGLE, "accountjid"));
        } else {
            dataForm.addField(new Field("The Jabber ID whose password will be changed.", Field.Type.JID_SINGLE, "accountjid", constrainedJID.getFullQualifiedName()));
        }
        dataForm.addField(new Field("The new password for this account", Field.Type.TEXT_PRIVATE, "password"));
        dataForm.addField(new Field("Retype new password", Field.Type.TEXT_PRIVATE, "password-verify"));

        return DATA_FORM_ENCODER.getXML(dataForm);
    }
    
    protected XMLElement processForm(List<XMLElement> commandElements, List<Note> notes) {
        if (commandElements.size() != 1) {
            throw new IllegalStateException("must be an X element");
        }
        final DataFormParser dataFormParser = new DataFormParser(commandElements.get(0));
        final Map<String,Object> valueMap = dataFormParser.extractFieldValues();
        final Entity accountjid;
        if(valueMap.get("accountjid") instanceof Entity) {
            accountjid = (Entity) valueMap.get("accountjid");
        } else {
            accountjid = EntityImpl.parseUnchecked((String) valueMap.get("accountjid"));
        }
        final String password = (String)valueMap.get("password");
        final String password2 = (String)valueMap.get("password-verify");

        final boolean success = checkPassword(notes, accountjid, password, password2);
        if (!success) return sendForm();

        if (constrainedJID != null && !constrainedJID.equals(accountjid)) {
            notes.add(Note.error("password change only allowed for " + constrainedJID.getFullQualifiedName()));
            return sendForm();
        }
        
        try {
            accountManagement.changePassword(accountjid, password);
        } catch (AccountCreationException e) {
            notes.add(Note.error("changing password failed for " + accountjid));
            return sendForm();
        }

        isExecuting = false;
        return null;
    }

}
