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

