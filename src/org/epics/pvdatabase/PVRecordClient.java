/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvdatabase;


/**
 * PVRecordClient is implemented by any code that attaches to a record.
 * @author mrk
 * @since 2015.01.20
 *
 */
public interface PVRecordClient {
    /**
     * Detach from record.
     * This is called when the record is being destroyed.
     * The client does not have to call PVREcord.unregisterClient.
     * @param pvRecord The record from which the client must detach.
     */
    void detach(PVRecord pvRecord);
}
