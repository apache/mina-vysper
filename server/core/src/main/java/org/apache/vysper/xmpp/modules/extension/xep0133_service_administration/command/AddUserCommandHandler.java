package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command;

import org.apache.commons.lang.StringUtils;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
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
public class AddUserCommandHandler extends PasswordCheckingCommandHandler {
    
    protected AccountManagement accountManagement;
    protected List<String> allowedDomains;

    public AddUserCommandHandler(AccountManagement accountManagement, List<String> allowedDomains) {
        this.accountManagement = accountManagement;
        if (allowedDomains == null || allowedDomains.size() == 0) {
            throw new IllegalArgumentException("allowed domain list cannot be empty");
        }
        this.allowedDomains = allowedDomains;
    }

    public XMLElement process(List<XMLElement> commandElements, List<Note> notes) {
        if (commandElements == null || commandElements.size() == 0) {
            return sendForm();
        } else {
            return processForm(commandElements, notes);
        }
    }

    protected XMLElement sendForm() {
        final DataForm dataForm = createFormForm("Adding a User", "Fill out this form to add a user.");
        dataForm.addField(new Field("The Jabber ID for the account to be added", Field.Type.JID_SINGLE, "accountjid"));
        dataForm.addField(new Field("The password for this account", Field.Type.TEXT_PRIVATE, "password"));
        dataForm.addField(new Field("Retype password", Field.Type.TEXT_PRIVATE, "password-verify"));
        dataForm.addField(new Field("Email address", Field.Type.TEXT_SINGLE, "email"));
        dataForm.addField(new Field("Given name", Field.Type.TEXT_SINGLE, "given_name"));
        dataForm.addField(new Field("Family name", Field.Type.TEXT_SINGLE, "surname"));

        return DATA_FORM_ENCODER.getXML(dataForm);
    }

    protected XMLElement processForm(List<XMLElement> commandElements, List<Note> notes) {
        if (commandElements.size() != 1) {
            throw new IllegalStateException("must be an X element");
        }
        final DataFormParser dataFormParser = new DataFormParser(commandElements.get(0));
        final Map<String,Object> valueMap = dataFormParser.extractFieldValues();
        final Entity accountjid = (Entity)valueMap.get("accountjid");
        final String password = (String)valueMap.get("password");
        final String password2 = (String)valueMap.get("password-verify");

        if (accountjid == null || !allowedDomains.contains(accountjid.getDomain())) {
            notes.add(Note.error("new account must match one of this server's domains, e.g. " + allowedDomains.get(0)));
            return sendForm();
        }

        final boolean success = checkPassword(notes, accountjid, password, password2);
        if (!success) return sendForm();
        
        if (accountManagement.verifyAccountExists(accountjid)) {
            notes.add(Note.error("account already exists: " + accountjid));
            return sendForm();
        }

        try {
            accountManagement.addUser(accountjid, password);
        } catch (AccountCreationException e) {
            notes.add(Note.error("account creation failed for " + accountjid));
            return sendForm();
        }

        isExecuting = false;
        return null;
    }

}
