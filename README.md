Python.Java Remote Services
===========================

## Quickstart Example

One easy way to demonstrate the utility of this distribution provider is to have a Java-based [OSGi Remote Service](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteservices.html) able to be used by a Python process.

### Karaf-Based OSGi Remote Service

This example  [HelloImpl.java](https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.hello.javahost/src/org/eclipse/ecf/examples/hello/javahost/HelloImpl.java) from this repository shows a small OSGi Async Remote Service with three simple service methods.

To install and start this example service in [Karaf 4.4.6](https://karaf.apache.org/download) paste or type the following into the karaf console:

<pre>
  karaf@root()&gt; feature:repo-add https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
  karaf@root()&gt; feature:install ecf-rs-examples-python.java-hello
</pre>

After installing all OSGi and ECF components, and exporting the HelloImpl service, the Karaf console will print out the OSGi standard export registration and  endpoint desription

<pre>
  19:20:03.679;EXPORT_REGISTRATION;exportedSR=[org.eclipse.ecf.examples.hello.IHello];cID=URIID [uri=py4j://127.0.0.1:25333/java];rsId=1
--Endpoint Description---
&lt;endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0"&gt;
  &lt;endpoint-description&lt;
    &lt;property name="ecf.endpoint.id" value-type="String" value="py4j://127.0.0.1:25333/java"/&gt;
...continues with the entire endpoint description  
</pre>

The **py4j://127.0.0.1:25333/java** as the value for **ecf.endpoint.id** property indicates that a listener has been opened on 127.0.0.1 (localhost) using port 25333 and waiting for connections from clients.

The example [python client program](examples/org.eclipse.ecf.examples.hello.pythonclient/src/run.py) can then be run 
by first installing the osgiservicebridge

<pre>
  pip install osgiservicebridge
</pre>

Then the python hello service client can be run by going to the [examples/org.eclipse.ecf.examples.hello.pythonclient/src](examples/org.eclipse.ecf.examples.hello.pythonclient/src) directory and running the pythonclient program.

<pre>
  python run.py
</pre>

On the Karaf/Java side you should see something like this output to the console

<pre>
  Java.sayHello called by from.python with message: 'this is a big hello from Python!!!'
</pre>

and on the Python side it will have console output like this

<pre>
bridge created
service_imported endpointid=02ea388d-417d-48ce-8372-ed9ed0714bb4;proxy=org.eclipse.ecf.examples.hello.javahost.HelloImpl@59f0e37d;endpoint_props={'objectClass': 
...more endpoint description contents
response=Java says: Hi from.python, nice to see you
bridge connected
service_unimported endpointid=02ea388d-417d-48ce-8372-ed9ed0714bb4;proxy=org.eclipse.ecf.examples.hello.javahost.HelloImpl@59f0e37d;endpoint_props={'objectClass': 
...more endpoint description contents
disconnected...exiting
</pre>

This example shows the use of standard OSGi Remote Services to enable aribitrary Python -> Java remote procedure call.  It is also possible to have Java -> Python rpc (Java clients to Python implemented remote services)

## Protobuf-Serialization

This codebase to use [Google Protocol Buffers](https://protobuf.dev/) for high-performance serialization/deserialization, as well as support for client, server, and/or bidirectional streaming.

### Dependencies

The Py4j Remote Services Provider depends upon the ECF implementation of the OSGi Remote Service Admin (RSA).   The latest version is available as an Eclipse p2 repository as described by [this download page](https://www.eclipse.org/ecf/downloads.php).

A recent build of this repo's java components (OSGi bundles) exist in [this directory](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/build):https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/build

A P2 Repository is also available in that same directory...i.e:  https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/build.  Add this URL with name (e.g.):  ECF Py4j Provider.

For support please file an issue on this repo, or contact [scottslewis@gmail.com](mailto:scottslewis@gmail.com)

### Python Components

Python OSGi Service Bridge may be installed via pip:

<pre>
pip install osgiservicebridge
</pre>

The OSGi Service Bridge Python source code is in [the python/osgiservicebridge](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/python/osgiservicebridge) project.   This package should be installed into Python 3.3+ prior to running the example (in, e.g. the run.py in ]examples/org.eclipse.ecf.examples.protobuf.hello/python-src](examples/org.eclipse.ecf.examples.protobuf.hello/python-src) directory).

### Python Impl of OSGi Bundle and Service Layers

A full Python implementation of OSGi bundle and service layer, with Remote Services and Remote Service Admin implementations built-in is also available via the [iPopo](https://ipopo.readthedocs.io/en/1.0.1/) project.

LICENSE
=======

Python.Java Remote Services is distributed with the Apache2 license. See LICENSE in this directory for more
information.

