Python.Java Remote Services
===========================

##Quickstart

One easy way to demonstrate the utility of this distribution provider is to have a Java-based [OSGi Remote Service](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteservices.html) able to be used by a Python process.

### Example Karaf-Based OSGi Remote Service

This example  [HelloImpl.java](https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.hello.javahost/src/org/eclipse/ecf/examples/hello/javahost/HelloImpl.java) from this repository shows a small OSGi Async Remote Service with three simple service methods.

To install and start this example service in [Karaf 4.4.6](https://karaf.apache.org/download) paste or type the following into the karaf console:

<pre>
  karaf@root()> feature:install ecf-rs-examples-python.java-hello
</pre>

After downloading and installing all OSGi and ECF components, and exporting the HelloImpl service, the Karaf console will print out the export registration, along with the endpoint desription in the OSGi standard format.

<pre>
  19:20:03.679;EXPORT_REGISTRATION;exportedSR=[org.eclipse.ecf.examples.hello.IHello];cID=URIID [uri=py4j://127.0.0.1:25333/java];rsId=1
--Endpoint Description---
&lt;endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0"&gt;
  &lt;endpoint-description&lt;
    &lt;property name="ecf.endpoint.id" value-type="String" value="py4j://127.0.0.1:25333/java"/&gt;
...continues with the entire endpoint description  
</pre>

The py4j://127.0.0.1:25333/java ad the value for ecf.endpoint.id property indicates that a listener has been opened on 127.0.0.1 (localhost) using port 25333 and waiting for connections from clients.

The example [python client program](examples/org.eclipse.ecf.examples.hello.pythonclient/src/run.py) can then be run 
by first installing the osgiservicebridge

<pre>
  pip install osgiservicebridge
</pre>

Then the python hello service client can be run by going to the [examples/org.eclipse.ecf.examples.hello.pythonclient/src](examples/org.eclipse.ecf.examples.hello.pythonclient/src) directory and running the program.

<pre>
  python run.py
</pre>

On the Karaf/Java side you should see something like this output to the console

<pre>
  Java.sayHello called by from.python with message: 'this is a big hello from Python!!!'
</pre>

and on the Python side it should have output like this

<pre>
  Py4j-RemoteServicesProvider\examples\org.eclipse.ecf.examples.hello.pythonclient\src>python run.py
bridge created
service_imported endpointid=02ea388d-417d-48ce-8372-ed9ed0714bb4;proxy=org.eclipse.ecf.examples.hello.javahost.HelloImpl@59f0e37d;endpoint_props={'objectClass': ['org.eclipse.ecf.examples.hello.IHello'], 'osgi.ds.satisfying.condition.target': '(osgi.condition.id=true)', 'ecf.endpoint.id': 'py4j://127.0.0.1:25333/java', 'endpoint.service.id': 115, 'ecf.endpoint.ts': 1720059603648, 'osgi.basic.timeout': 50000, 'remote.configs.supported': ['ecf.py4j.host'], 'remote.intents.supported': ['passByReference', 'exactlyOnce', 'ordered', 'py4j', 'py4j.async', 'osgi.async', 'osgi.private', 'osgi.confidential'], 'service.intents': 'osgi.async', 'ecf.endpoint.id.ns': 'ecf.namespace.py4j', 'endpoint.id': '02ea388d-417d-48ce-8372-ed9ed0714bb4', 'endpoint.framework.uuid': '291bc8b0-2058-466d-b98a-ab79803180f7', 'service.imported': 'true', 'service.imported.configs': ['ecf.py4j.host'], 'ecf.rsvc.id': 1}
response=Java says: Hi from.python, nice to see you
bridge connected
service_unimported endpointid=02ea388d-417d-48ce-8372-ed9ed0714bb4;proxy=org.eclipse.ecf.examples.hello.javahost.HelloImpl@59f0e37d;endpoint_props={'objectClass': ['org.eclipse.ecf.examples.hello.IHello'], 'osgi.ds.satisfying.condition.target': '(osgi.condition.id=true)', 'ecf.endpoint.id': 'py4j://127.0.0.1:25333/java', 'endpoint.service.id': 115, 'ecf.endpoint.ts': 1720059603648, 'osgi.basic.timeout': 50000, 'remote.configs.supported': ['ecf.py4j.host'], 'remote.intents.supported': ['passByReference', 'exactlyOnce', 'ordered', 'py4j', 'py4j.async', 'osgi.async', 'osgi.private', 'osgi.confidential'], 'service.intents': 'osgi.async', 'ecf.endpoint.id.ns': 'ecf.namespace.py4j', 'endpoint.id': '02ea388d-417d-48ce-8372-ed9ed0714bb4', 'endpoint.framework.uuid': '291bc8b0-2058-466d-b98a-ab79803180f7', 'service.imported': 'true', 'service.imported.configs': ['ecf.py4j.host'], 'ecf.rsvc.id': 1}
disconnected...exiting
</pre>

===========================
An Remote Services Distribution Provider for [OSGi R7 Remote Services](https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html).  This allows dynamic remote procedure call between Java and Python objects.  Python-Implementations can be exposed to Java consumers as OSGi services, and Java-based OSGi services can be exposed to Python consumers.

## NEW: Support for OSGi R7 Async Remote Services
OSGi R7 Remote Services includes support for [Asynchronous Remote Services](https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html#d0e1407) supporting Remote Services with return values of CompletableFuture, Future, or OSGi's Promise (Java) that will be executed asynchronously.

## NEW: Bndtools templates to run Python.Java Hello Example app and Python.Java Protobuf Hello Example app.
There is now [support for using ECF Remote Services impl with Bndtools](https://wiki.eclipse.org/Bndtools_Support_for_Remote_Services_Development).  There are now templates in the [bndtools.workspace](https://github.com/ECF/bndtools.workspace) that will run the Python.Java Hello and Protobuf Hello Examples.  Here are the instructions for using the Hello template (also appears in help window when selecting the ECF Python.Java Hello Example template:
This template will create a bndrun for launching the Python.Java Hello example application.
Source code repo: https://github.com/ECF/Py4j-RemoteServicesProvider

Once the resulting bndrun file is resolved and the program is run, output like this will appear in console
<pre>
g! SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Jul 15, 2018 7:54:36 PM py4j.GatewayServer fireServerStarted
INFO: Gateway Server Started
19:54:36.814;EXPORT_REGISTRATION;exportedSR=[org.eclipse.ecf.examples.hello.IHello];cID=URIID [uri=py4j://127.0.0.1:25333/java];rsId=1
--Endpoint Description---
<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
  <endpoint-description>
  ...
</pre>

This means that the Java gateway listener is waiting for connections from a Python process.

Instructions for the ECF Python.Java Protobuf Hello Example template:

This template will create a bndrun for launching the Python.Java Protobuf Hello example application.
Source code repo: https://github.com/ECF/Py4j-RemoteServicesProvider
Once the resulting bndrun file is resolved and the program is run, output like this will appear in console

<pre>
g! SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Jul 15, 2018 7:54:36 PM py4j.GatewayServer fireServerStarted
INFO: Gateway Server Started
19:54:36.814;EXPORT_REGISTRATION;exportedSR=[org.eclipse.ecf.examples.protobuf.hello.IHello];cID=URIID [uri=py4j://127.0.0.1:25333/java];rsId=1
--Endpoint Description---
<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
  <endpoint-description>
  ...
</pre>

## Download and Install
### Java Components

### Dependencies

The Py4j Remote Services Provider depends upon the ECF implementation of the OSGi Remote Service Admin (RSA).   The latest version is available at [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.eclipse.ecf%22) and as an Eclipse p2 repository or zip as described by [this download page](https://www.eclipse.org/ecf/downloads.php).

A recent build of the Java components (OSGi bundles) exist in [this directory](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/build):https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/build

A P2 Repository is also available in that same directory...i.e:  https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/build.  Add this URL with name (e.g.):  ECF Py4j Provider.

For support please file an issue on this repo, or contact [scottslewis@gmail.com](mailto:scottslewis@gmail.com)

### Python Components

Python OSGi Service Bridge may be installed via pip:

<pre>
pip install osgiservicebridge
</pre>

The Python OSGi Service Bridge depends upon both [Py4j](https://www.py4j.org/) 0.10.7+ and [Google Protocol Buffers version 3.5.1](https://developers.google.com/protocol-buffers/).   pip will install these dependencies via the command above.

The OSGi Service Bridge Python source code is in [the python/osgiservicebridge](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/python/osgiservicebridge) project.   This package should be installed into Python 3.3+ prior to running the example (in, e.g. the run.py in examples/org.eclipse.ecf.examples.protobuf.hello/python-src directory).

LICENSE
=======

Python.Java Remote Services is distributed with the Apache2 license. See LICENSE in this directory for more
information.

