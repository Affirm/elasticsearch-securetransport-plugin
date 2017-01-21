Elasticsearch Secure Transport Plugin
===================

Node to node communication in Elasticsearch happens over unencrypted TCP channels by default.
This plugin adds a Netty SslHandler to the ChannelPipeline for TCP channels, enabling all node-to-node communication
to be encrypted and verified using a JKS keystore and truststore.

----------

Supported Versions
-------------------
* Elasticsearch 2.4

Please open an issue if you need support for a different Elasticsearch version. Pull requests are encouraged.


Installing from Source
-------------

```
git clone https://github.com/Affirm/elasticsearch-securetransport-plugin
cd elasticsearch-securetransport-plugin
mvn package
$ES_HOME/bin/plugin install target/elasticsearch-transport-secure-2.4.2.zip
```

Usage
-------------------
The simplest way to use the plugin is to generate a single java keystore, and use that keystore as the keystore and
truststore for each node in your cluster

```
keytool -genkey -keyalg RSA -alias securetransport -keystore keystore.jks -keypass mypass -storepass mypass -validity 365 -keysize 2048 -dname "CN=yourdomain.com, OU=YourOU, O=YourO, L=Location, S=STATE, C=COUNTRY"
```

The certs in the keystore will need to be rotated after a year. You can change the validity to make it valid for longer.

1) Copy keystore.jks to $ES_HOME/plugins/transport-secure on each of your nodes.

2) Add the following properties to you elasticsearch.yml config file:
```
transport.type: secure
transport.secure.keystore.path: /usr/share/elasticsearch/plugins/transport-secure/keystore.jks
transport.secure.keystore.password: mypass
transport.secure.keystore.keypassword: mypass
transport.secure.truststore.path: /usr/share/elasticsearch/plugins/transport-secure/keystore.jks
transport.secure.truststore.password: mypass
```

Restart your nodes to start using secure transport. You can verify the secure transport is being used when you see
this message in the logs:
```
[2017-01-10 00:05:48,213][INFO ][plugin.transport.secure  ] [mynode] Secure ES plugin loaded
```

Java Keystore and Truststore
-------------------
In the example above, we use the same jks store for the keystore and truststore on all of our nodes.
This creates a simple PKI setup where each node uses the same private key for encryption, so all traffic between nodes
is encrypted with the same keys. SSL verification via the truststore uses the same jks store, so it verifies that this
is in fact the key being used.

If other setups are required (ex, using a CA for verification, with different keypairs on each node), this should be
configured in the keystore and truststore that each node uses in the config.

Safely Migrating from Unencrypted to Encrypted Transport
-------------------
During a deployment to use encrypted transport, new nodes that start using encrypted transport will be unable to
communicate with old nodes using unencrypted transport. These issues will resolve themselves when all the nodes are
updated. To ensure a safe deployment, follow the following steps
(as recommended in https://www.elastic.co/guide/en/elasticsearch/guide/current/_rolling_restarts.html):

1) Disable shard allocation
```
PUT /_cluster/settings
{
    "transient" : {
        "cluster.routing.allocation.enable" : "none"
    }
}
```
2) Do a rolling deployment to each node in your cluster

3) Re enable shard allocation
```
PUT /_cluster/settings
{
    "transient" : {
        "cluster.routing.allocation.enable" : "all"
    }
}
```
4) Wait for the cluster to return to green state

