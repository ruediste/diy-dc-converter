package com.github.ruediste;

/**
 * Sent by the FW Controller to trigger the generation of a Blob Message
 */
public class TriggerBlobMessage implements InterfaceMessage {
    @Datatype.uint16
    int dummy; // required, a single byte won't go through
}
