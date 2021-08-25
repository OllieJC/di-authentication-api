package uk.gov.di.domain;

import uk.gov.di.authentication.shared.domain.AuditableEvent;

public enum AccountManagementAuditableEvent implements AuditableEvent {
    ACCOUNT_MANAGEMENT_REQUEST_RECEIVED,
    ACCOUNT_MANAGEMENT_REQUEST_ERROR,
    ACCOUNT_MANAGEMENT_PHONE_NUMBER_UPDATED,
    ACCOUNT_MANAGEMENT_CONSENT_UPDATED,
    ACCOUNT_MANAGEMENT_TERMS_CONDS_ACCEPTANCE_UPDATED
}
