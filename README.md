Python.Java Remote Services
===========================

## Quickstart Example

One way to demonstrate this distribution provider is to have a Java-based [OSGi Remote Service](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteservices.html) and call/use/consume it from a Python process or a Java process.  Just to be clear, with these same components (based upon OSGi Remote Services and Remote Service Admin specifications) it is completely possible to export a Python-implemented service, and consume the service from either a Java or Python process as well.  For simplification, the example below exports a remote service from Java, and imports/uses/consumes that service from Python.

### Karaf-Based OSGi Remote Service

This example  [HelloImpl.java](https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.hello.javahost/src/org/eclipse/ecf/examples/hello/javahost/HelloImpl.java) from this repository shows a small [OSGi Async Remote Service](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteservices.html#d0e1496) with three service methods.

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

To run the example [python client program](examples/org.eclipse.ecf.examples.hello.pythonclient/src/run.py) it's necessary to first install the osgiservicebridge python library (part of this codebase in [python/osgiservicebridge](python/osgiservicebridge)

Osgiservicebridge Install

Install via Pip

<pre>
  pip install osgiservicebridge
</pre>

Install Locally in [python/osgiservicebridge](python/osgiservicebridge) directory

<pre>
  python setup.py install develop
</pre>

The python hello service client can then be run by going to the [examples/org.eclipse.ecf.examples.hello.pythonclient/src](examples/org.eclipse.ecf.examples.hello.pythonclient/src) directory and starting the run.py example program.

<pre>
  python run.py
</pre>

After starting run.py, on the Karaf/Java side you should see something like this output to the console

<pre>
  Java.sayHello called by from.python with message: 'this is a big hello from Python!!!'
</pre>

and on the Python side there should be console output like this

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

## Protobuf-Serialization/Deserialization

This codebase to use [Google Protocol Buffers](https://protobuf.dev/) for high-performance serialization/deserialization, as well as support for client, server, and/or bidirectional streaming and [gRPC](https://grpc.io).

## Support

For support please open an issue on this repo, or contact [scottslewis@gmail.com](mailto:scottslewis@gmail.com)

### Python Components

The OSGi Service Bridge Python source code is in [the python/osgiservicebridge](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/python/osgiservicebridge) project.   This package should be installed into Python 3.3+ prior to running the pythonclient example.

### Python Impl of OSGi Bundle, Service, and Remote Service Admin in [iPopo v3](https://ipopo.readthedocs.io/en/v3/foreword.html)

A full Python implementation of OSGi bundle and service layer, along with Remote Services and Remote Service Admin implementations in python is also available via the [iPopo v3](https://ipopo.readthedocs.io/en/v3/foreword.html) project.   Note that this python based implementation of Remote Services and Remote Service Admin is makes Python and Java-based remote services completely interoperable, and allows the use of multi-language service discovery systems like [etcd3](https://etcd.io/) (discovery protocol for Kubernetes) and multi-language distribution systems such as [grpc](https://grpc.io/).

LICENSE
=======

Python.Java Remote Services is distributed with the Apache2 license. See LICENSE in this directory for more
information.

