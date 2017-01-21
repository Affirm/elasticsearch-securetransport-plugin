package org.elasticsearch.plugin.transport.secure;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.TransportModule;

/**
 * Created by gregsterin on 12/21/16.
 */
public class SecureTransportPlugin extends Plugin {
    @Override
    public String name() {
        return "transport-secure";
    }

    @Override
    public String description() {
        return "Encrypts Elasticsearch Transport TCP layer with SSL.";
    }

    public void onModule(TransportModule module) {
        module.addTransport("secure", SecureTransport.class);
    }
}
