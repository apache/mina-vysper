package org.apache.vysper.xmpp.modules.servicediscovery.management;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.dataforms.DataForm;
import org.apache.vysper.xmpp.stanza.dataforms.DataFormEncoder;
import org.apache.vysper.compliance.SpecCompliant;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceCoverage.COMPLETE;
import static org.apache.vysper.compliance.SpecCompliant.ComplianceStatus.FINISHED;

/**
 * this adds support for Service Discovery Extensions, which allows adding x-DataForms to info responses 
 */
@SpecCompliant(spec = "XEP-0128", status = FINISHED, coverage = COMPLETE)
public class InfoDataForm implements InfoElement {

    private static final Integer CLASS_ID = new Integer(3);
    protected static final DataFormEncoder DATA_FORM_ENCODER = new DataFormEncoder();

    protected XMLElement dataFormXML;

    public InfoDataForm(DataForm dataForm) {
        dataFormXML = DATA_FORM_ENCODER.getXML(dataForm);
    }

    public Integer getElementClassId() {
        return CLASS_ID;
    }

    public void insertElement(StanzaBuilder stanzaBuilder) {
        stanzaBuilder.addPreparedElement(dataFormXML);
    }
}
