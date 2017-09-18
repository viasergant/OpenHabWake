package org.openhab.binding.wake.internal;

/**
 * states for receiving of Wake packet
 * 
 * @author Serhiy_Povoroznyuk
 *
 */
public enum RxState {
    BEGIN,
    STARTPACKET,
    ADDRESS,
    COMMAND,
    DATA,
    CRC
}
