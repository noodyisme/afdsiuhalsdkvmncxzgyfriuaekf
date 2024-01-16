package com.capitalone.identity.identitybuilder.policycore.externalization.events;

import org.apache.logging.log4j.Logger;


public interface EntityLoadResult {

    void visit(EntityLoadEventVisitor visitor);

    /**
     * Identical to {@link #visit(EntityLoadEventVisitor)}, but catches and logs any
     * exceptions thrown during the visitor method dispatch. Note: use this method only
     * for non-critical functionality. Use {@link #visit(EntityLoadEventVisitor)}
     * directly to handle exceptions.
     *
     * @param errorLogger for logging the error if it occurs
     */
    default void visitCatching(EntityLoadEventVisitor visitor, Logger errorLogger) {
        try {
            visit(visitor);
        } catch (Exception e) {
            errorLogger.error("Caught exception in EntityLoadOperationResult visitor.", e);
        }
    }
}
