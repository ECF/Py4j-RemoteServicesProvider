Python.Java Remote Services
===========================
An Remote Services Distribution Provider for [OSGi R7 Remote Services](https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html).  This allows dynamic remote procedure call between Java and Python objects.  Python-Implementations can be exposed to Java consumers as OSGi services, and Java-based OSGi services can be exposed to Python consumers.

## Python Services accessed from Java

See [here](https://wiki.eclipse.org/Tutorial:_Python_for_OSGi_Services) for a tutorial based on this use case.

## Java Services accessed from Python
For example, to make an OSGi service available for access in Python it's only necessary to add an OSGi-standard service property.

<pre>
@Component(property = { "service.exported.interfaces=*", // RS standard service property
                        "service.exported.configs=ecf.py4j.host"})   //  RS standard service property
public class EvalImpl implements Eval {
	public double eval(String expression) throws Exception {
           ...Java impl here
        }
}
</pre>

Using [ECF's Remote Service Admin](https://wiki.eclipse.org/Eclipse_Communication_Framework_Project#OSGi_Remote_Services) implementation and the [IPOPO 0.8 and above](https://ipopo.readthedocs.io/en/0.7.0/), a proxy for service instance will be injected into a Python service consumer(s), allowing it to call the eval on this OSGi service instance.

## NEW: Support for OSGi R7 Async Remote Services
OSGi R7 Remote Services includes support for [Asynchronous Remote Services](https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html#d0e1407) supporting Remote Services with return values of CompletableFuture, Future, or OSGi's Promise (Java) that will be executed asynchronously.

## Download and Install
### Java Components

#### Dependencies

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

Python.Java Remote Services is distributed with the Apache3 license. See LICENSE in this directory for more
information.

