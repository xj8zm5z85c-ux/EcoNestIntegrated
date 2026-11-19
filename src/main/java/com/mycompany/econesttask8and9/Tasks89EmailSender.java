package com.mycompany.econesttask8and9;

/** Sends a plain-text email through the configured mail provider. */
public interface Tasks89EmailSender {

    void send(String recipient, String subject, String body) throws Tasks89EmailDeliveryException;
}
