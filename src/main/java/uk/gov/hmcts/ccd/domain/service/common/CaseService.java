package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

// TODO CaseService and CaseDataService could probably be merged together.
@Service
public class CaseService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CaseDataService caseDataService;
    private final CaseDetailsRepository caseDetailsRepository;
    private final UIDService uidService;

    @Autowired
    public CaseService(CaseDataService caseDataService,
                       @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                       UIDService uidService) {
        this.caseDataService = caseDataService;
        this.caseDetailsRepository = caseDetailsRepository;
        this.uidService = uidService;
    }

    /**
     * Generate a SHA1 hash of the case data serialised as JSON.
     *
     * @param caseDetails Case whose data will be hashed
     * @return SHA1 hash of the given case data
     */
    public String hashData(CaseDetails caseDetails) {
        final JsonNode jsonData = MAPPER.convertValue(caseDetails.getData(), JsonNode.class);
        return DigestUtils.sha1Hex(jsonData.toString());
    }

    /**
     * @param caseTypeId     caseTypeId of new case details
     * @param jurisdictionId jurisdictionId of new case details
     * @return <code>CaseDetails</code> - new case details object
     */
    public CaseDetails createNewCaseDetails(String caseTypeId, String jurisdictionId, Map<String, JsonNode> data) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(caseTypeId);
        caseDetails.setJurisdiction(jurisdictionId);
        caseDetails.setData(data == null ? Maps.newHashMap() : data);
        return caseDetails;
    }

    public CaseDetails clone(CaseDetails source) {
        final CaseDetails clone;

        try {
            clone = source.shallowClone();
        } catch (CloneNotSupportedException e) {
            // Trivial exception as DetailsCase implements Cloneable interface
            throw new IllegalArgumentException("Case details cannot be cloned", e);
        }

        // Deep cloning of mutable properties
        clone.setData(caseDataService.cloneDataMap(source.getData()));
        clone.setDataClassification(caseDataService.cloneDataMap(source.getDataClassification()));

        return clone;
    }

    public CaseDetails getCaseDetails(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }
        final CaseDetails caseDetails = caseDetailsRepository.findByReference(Long.valueOf(caseReference));
        if (caseDetails == null) {
            throw new ResourceNotFoundException("No case exist with id=" + caseReference);
        }
        return caseDetails;
    }

}
