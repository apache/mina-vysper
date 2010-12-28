package org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands;

import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormEncoder;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.apache.vysper.xmpp.uuid.JVMBuiltinUUIDGenerator;
import org.apache.vysper.xmpp.uuid.UUIDGenerator;

/**
 */
public abstract class AbstractAdhocCommandHandler implements AdhocCommandHandler {
    
    private static UUIDGenerator SESSION_ID_GENERATOR = new JVMBuiltinUUIDGenerator();
    protected static final DataFormEncoder DATA_FORM_ENCODER = new DataFormEncoder();
    
    protected boolean isExecuting = true;
    protected boolean isPrevAllowed = false;
    protected boolean isNextAllowed = false;
    protected String sessionId = SESSION_ID_GENERATOR.create();

    public boolean isExecuting() {
        return isExecuting;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isPrevAllowed() {
        return isPrevAllowed;
    }

    public boolean isNextAllowed() {
        return isNextAllowed;
    }

    protected void addFormTypeField(DataForm dataForm) {
        dataForm.addField(new Field("", Field.Type.HIDDEN, Field.FORM_TYPE, NamespaceURIs.XEP0133_SERVICE_ADMIN));
    }

    protected DataForm createFormForm(final String title, final String instruction) {
        final DataForm dataForm = new DataForm();
        dataForm.setTitle(title);
        dataForm.addInstruction(instruction);
        dataForm.setType(DataForm.Type.form);
        addFormTypeField(dataForm);
        return dataForm;
    }

    protected DataForm createResultForm() {
        final DataForm dataForm = new DataForm();
        dataForm.setType(DataForm.Type.result);
        addFormTypeField(dataForm);
        return dataForm;
    }
}
