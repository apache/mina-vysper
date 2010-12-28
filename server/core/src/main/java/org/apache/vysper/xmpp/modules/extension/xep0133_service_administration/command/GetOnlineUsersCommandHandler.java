package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.command;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.AbstractAdhocCommandHandler;
import org.apache.vysper.xmpp.modules.extension.xep0050_adhoc_commands.Note;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.Field;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;

import java.util.List;

/**
 */
public class GetOnlineUsersCommandHandler extends AbstractAdhocCommandHandler {
    
    protected final ResourceRegistry resourceRegistry;

    public GetOnlineUsersCommandHandler(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
    }


    public XMLElement process(List<XMLElement> commandElements, List<Note> notes) {
        final long sessionCount = resourceRegistry.getSessionCount();

        final DataForm dataForm = createResultForm();
        dataForm.addField(new Field("The number of online users", Field.Type.FIXED, "onlineusersnum", Long.toString(sessionCount)));

        isExecuting = false;

        return DATA_FORM_ENCODER.getXML(dataForm);
    }

}
