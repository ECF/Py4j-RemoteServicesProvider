# Py4j-RemoteServicesProvider
An ECF Remote Services/RSA Provider that uses Py4j as the transport.   This allows easy and dynamic rpc between Java and Python objects.  Python-Implementations can be exposed to Java consumers as OSGi services, and Java-based OSGi services can be exposed to Python consumers.

## Python-Implemented OSGi Services

See [here](https://wiki.eclipse.org/Tutorial:_Python_for_OSGi_Services) for a tutorial based on this use case.

## Java-Implemented OSGi Services accessed from Python
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

Using [ECF's RSA](https://wiki.eclipse.org/Eclipse_Communication_Framework_Project#OSGi_Remote_Services) implementation, a proxy for this eval service instance will be injected into a Python application, allowing it to call eval on this OSGi service instance.

## Download and Install
### Java Components

#### Dependencies

The Py4j Remote Services Provider depends upon the ECF implementation of the OSGi Remote Service Admin (RSA).   The latest version is available at [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.eclipse.ecf%22) and as an Eclipse p2 repository or zip as described by [this download page](https://www.eclipse.org/ecf/downloads.php).

A recent build of the Java components (OSGi bundles) exist in binary form in the plugins subdirectory underneath [this directory](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/features/org.eclipse.ecf.provider.py4j.feature/build):https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/features/org.eclipse.ecf.provider.py4j.feature/build

A P2 Repository is also available in that same directory...i.e:  https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/features/org.eclipse.ecf.provider.py4j.feature/build.  Add this URL with name (e.g.):  ECF Py4j Provider.

Also present is a [Karaf](http://karaf.apache.org/) Features repository:  https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/features/org.eclipse.ecf.provider.py4j.feature/build/karaf-features.xml

Of course you may also build the components from source via Maven.   After importing projects, the target platform should be set to the contents of this file:  https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/releng/org.eclipse.ecf.provider.py4j.releng.target/ecf-oxygen.target.   This assumes the use of Java8 and Eclipse Oxygen or newer. 

For support please file an issue on this repo, or contact [scottslewis@gmail.com](mailto:scottslewis@gmail.com)

### Python Components

The Python OSGi Service Bridge may be installed via pip:

<pre>
pip install osgiservicebridge
</pre>

The Python OSGi Service Bridge depends upon both [Py4j](https://www.py4j.org/) 0.10.6+ and [Google Protocol Buffers version 3.3.1](https://developers.google.com/protocol-buffers/).   pip will install these dependencies via the command above.

The OSGi Service Bridge Python source code is in [the python/osgiservicebridge](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/python/osgiservicebridge) project.   This package should be installed into Python 2.7 or Python 3.3+ prior to running the example (in, e.g. the run.py in examples/org.eclipse.ecf.examples.protobuf.hello/python-src directory).

