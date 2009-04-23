package org.apache.vysper.xmpp.addressing.stringprep;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.compliance.SpecCompliant.ComplianceStatus;

/**
 * This class is used to prepare a Resource Identifier for further usage.
 * 
 * see RFC3920:3.4
 * see RFC3920:Appendix B
 * see http://www.ietf.org/rfc/rfc3920.txt
 * @author Gerolf Seitz (gseitz@apache.org)
 * 
 */
public class ResourcePrep extends StringPrep {

	private static final ResourcePrep INSTANCE = new ResourcePrep();

	/**
	 * Applies the Resourceprep profile to the given resource.
	 * 
	 * @param resource
	 *            the resource to prepare
	 * @return the prepared resource
	 * @throws StringPrepViolationException
	 *             in case the Resourceprep profile can't be applied
	 */
	public static String prepare(String resource)
			throws StringPrepViolationException {
		return INSTANCE.prepareString(resource);
	}

	private ResourcePrep() {

	}

	@Override
	@SpecCompliant(spec = "RFC3920", section = "B.3", status = ComplianceStatus.FINISHED)
	protected Map<String, String> buildMapping() {
		return StringPrepConstants.B_1_CommonlyMappedtoNothing;
	}

	@Override
	@SpecCompliant(spec = "RFC3920", section = "B.5", status = ComplianceStatus.FINISHED)
	protected Set<String> buildProhibitedSet() {
		Set<String> set = new HashSet<String>();
		set.addAll(StringPrepConstants.C_1_2_NonAsciiSpaceCharacters);
		set.addAll(StringPrepConstants.C_2_1_AsciiControlCharacters);
		set.addAll(StringPrepConstants.C_2_2_NonAsciiControlCharacters);
		set.addAll(StringPrepConstants.C_3_PrivateUse);
		set.addAll(StringPrepConstants.C_4_NonCharacterCodePoints);
		set.addAll(StringPrepConstants.C_5_SurrogateCodes);
		set.addAll(StringPrepConstants.C_6_InappropriateForPlainText);
		set
				.addAll(StringPrepConstants.C_7_InappropriateForCanonicalRepresentation);
		set
				.addAll(StringPrepConstants.C_8_ChangeDisplayPropertiesOrAreDeprecated);
		set.addAll(StringPrepConstants.C_9_TaggingCharacters);

		return set;
	}
}
