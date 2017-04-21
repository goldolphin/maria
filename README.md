# A simple HTTP protocol stack based on netty.

* Very light-weighted. There're no XMLs, Annotations, nor heavy dependency on reflection.
* Asynchronous server/client-side interfaces in Java 8 style.
* Shipped with a convenient and user-friendly RPC framework **Protoson** with features:
  * Automatically generate HTTP/CLI client implementation by java interface definition.
  * Automatically generate HTTP Controller by java interface definition and its implementation class.
  * Data are transferred in JSON-based format, which is AJAX-friendly.
  * Request/Response protocols are defined in Protobuf format.
  * **Protoson** connects RPC, AJAX and CLI together.
* High performance inherited from Netty 4.

### Straightforward sample code
* [HTTP server/client sample code](src/test/java/net/goldolphin/maria/HttpServerTest.java)
* [Protoson server/client sample code](src/test/java/net/goldolphin/maria/api/protoson/ProtosonTest.java)
