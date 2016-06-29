# Py4j-RemoteServicesProvider
An ECF Remote Services/RSA Provider that uses Py4j as the underlying transport.   This allows easy interaction between Java and Python via OSGi Remote Services.   For example, to make an OSGi service available for access in Python it's only necessary to add an OSGi-standard service property.

<pre>
@Component(immediate=true, 
property = { "service.exported.interfaces=*", 
             "service.exported.configs=ecf.py4j.host"})
public class EvalImpl implements Eval {
	public double eval(String expression) throws Exception {
           .. impl of Eval service
        }
}
</pre>

Using the OSGi Service Registry and ECF's RSA standard implementation, this eval service will be dynamically injected into the Python runtime.

A Python service impl can also be dynamically injected into the Java OSGi Service Registry.

<pre>
    remoteServiceProvider.registerService(svcinstance,rsaProps)
</pre>

where the svcinstance class is declared

<pre>
class EvalProvider(object):
    
    def eval(self, expression):
        'parse expression, evaluate (parse function above) and return as float/Double'
        return float(parse(expression))
    
    class Java:
        implements = ['com.acme.prime.eval.api.Eval']
</pre>
