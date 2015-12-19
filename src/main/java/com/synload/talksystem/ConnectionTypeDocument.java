package com.synload.talksystem;

import java.util.UUID;

public class ConnectionTypeDocument extends ConnectionDocument {

    public ConnectionTypeDocument(String type, UUID chain) {
        super(type, chain);
    }

    public ConnectionTypeDocument() {
        super( "communicationSocket", UUID.randomUUID());
    }
    

}
