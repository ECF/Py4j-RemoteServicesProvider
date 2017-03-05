# Py4j-RemoteServicesProvider
An ECF Remote Services/RSA Provider that uses Py4j as the transport.   This allows easy and dynamic rpc between Java and Python objects.   For example, to make an OSGi service available for access in Python it's only necessary to add an OSGi-standard service property.

<pre>
@Component(property = { "service.exported.interfaces=*", // RS standard service property
                        "service.exported.configs=ecf.py4j.host"})   //  RS standard service property
public class EvalImpl implements Eval {
	public double eval(String expression) throws Exception {
           .. impl of Eval service
        }
}
</pre>

Using ECF's RSA standard implementation, this eval service will be dynamically injected into a Python application.

A Python impl can also be dynamically injected into the Java OSGi Service Registry.

<pre>
    Py4jServiceBridge.export(EvalProvider(),rsaProps)
</pre>

where the EvalProvider class is declared

<pre>
class EvalProvider(object):
    
    def eval(self, expression):
        'parse expression, evaluate (parse function above) and return as float/Double'
        return float(parse(expression))
    
    class Java:
        implements = ['com.acme.prime.eval.api.Eval']
</pre>

Making the export call as above will make the Python-provided EvalProvider instance available to OSGi/Java consumers of the com.acme.prime.eval.api.Eval service via (e.g.) Declarative Services injection.
