Python.Java Remote Services
===========================

## NEW: Using Python.Java Remote Services to Resolve Python Imports

As part of recent work on [iPOPO RSA](https://github.com/tcalmant/ipopo), an implementation of a python import hook [pep-302](https://peps.python.org/pep-0302/) was created so using remote services java bundles could resolve python packages.  On the java side, the [https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/bundles/org.eclipse.ecf.provider.direct/src/org/eclipse/ecf/provider/direct/ModuleResolver.java](ModuleResolver) service interface exposes the methods called by a python import hook to resolve a python module.

[Here](examples/org.eclipse.ecf.examples.importhook.module/src/org/eclipse/ecf/examples/importhook/module/ExampleBundleModuleResolver.java) is an example ModuleResolver service implementation for a python package named 'foo'.  Note that the PATH_PREFIX component property points to ['/python-src'](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/examples/org.eclipse.ecf.examples.importhook.module/python-src/) inside built bundle.

### To run this 'foo' package ModuleResolver in [Karaf 4.4.6+](https://karaf.apache.org/download)
```
start karaf
...
# add ECF Karaf repo
karaf@root()> repo-add https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
Adding feature url https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
```
### Install the Karaf feature that has the 'foo' package example Module Resolver
```
karaf@root()> feature:install ecf-rs-examples-python-importhook
(few seconds pass for download and install)
karaf@root()>
```
By default, a Python.Java gateway will be started and be listening for python connections on localhost:25333 (unless configured otherwise).  
The 'foo' package ModuleResolver remote service will be available for resolving the 'foo' package.

### Start iPopo Example ImportHook Application
The iPOPO project has a python-side Python.Java Distribution Provider, and a sample application to connect to the Java server at localhost:25333 and then resolve and run the code in the 'foo' package.  This sample application is [here](https://github.com/tcalmant/ipopo/blob/v3/samples/run_rsa_py4j_importhook.py).

To start from shell (python 3.10+):
```
python -m samples.run_rsa_py4j_importhook
```
If the Karaf server is running the python sample application will produce output like this
```
Attempting connect to Python.Java OSGi server listening at 25333...
** Pelix Shell prompt **
$ If the py4j localhost connect above succeeds, the code import for package foo.bar.baz
will be resolved by the OSGi server with an a active instance of a ModuleResolver
service implementation from the example in this bundle: org.eclipse.ecf.examples.importhook.module
The code for this bundle is here: 
https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/examples/org.eclipse.ecf.examples.importhook.module
There are instructions for running an instance of this server in the RSA importhook tutorial at
https://ipopo.readthedocs.io/en/v3/tutorials/index.html

...importing Bar class from foo.bar.baz package

foo imported
imported bar
baz loaded
foobar loaded

...Bar class imported
...creating an instance of Bar...

Foo.init
Bar.init

...Bar instance created.  The print output between the lines starting with '...' is from foo package code
```
The messages 'foo imported, imported bar' are produced from running the python code returned by the ModuleResolver service from [/python-src](https://github.com/ECF/Py4j-RemoteServicesProvider/tree/master/examples/org.eclipse.ecf.examples.importhook.module/python-src).

If the Karaf server is not running/listending on localhost:25333, it will produce a connect error.  Here is an example of such a connect error.
```
Attempting connect to Python.Java OSGi server listening at 25333...
** Pelix Shell prompt **
$ Component 'py4j-distribution-provider': error calling @ValidateComponent callback
Traceback (most recent call last):
...
py4j.protocol.Py4JNetworkError: An error occurred while trying to connect to the Java server (127.0.0.1:25333)

```
## Remote Services Example:  Java Server, Python Consumer

### Starting the Karaf-Based HelloImpl Server

This example implementation [HelloImpl.java](https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.hello.javahost/src/org/eclipse/ecf/examples/hello/javahost/HelloImpl.java) shows a small [OSGi Async Remote Service](https://docs.osgi.org/specification/osgi.cmpn/8.0.0/service.remoteservices.html#d0e1496).

To install and start this example service in [Karaf 4.4.6+](https://karaf.apache.org/download) type the following into the karaf console:

```
  karaf@root()>feature:repo-add https://download.eclipse.org/rt/ecf/latest/karaf-features.xml
  karaf@root()>feature:install ecf-rs-examples-python.java-hello
```

After installing all bundles and components, and exporting the HelloImpl service, the Karaf console will print out the OSGi standard export registration and endpoint desription

```
  19:20:03.679;EXPORT_REGISTRATION;exportedSR=[org.eclipse.ecf.examples.hello.IHello];cID=URIID [uri=py4j://127.0.0.1:25333/java];rsId=1
--Endpoint Description---
&lt;endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0"&gt;
  &lt;endpoint-description&lt;
    &lt;property name="ecf.endpoint.id" value-type="String" value="py4j://127.0.0.1:25333/java"/&gt;
...continues with the entire endpoint description  
```

The **py4j://127.0.0.1:25333/java** as the value for **ecf.endpoint.id** property indicates that a listener has been opened on 127.0.0.1 (localhost) using port 25333 and waiting for connections from clients.

### Starting the iPOPO Python Consumer Sample Application
```
  python -m samples.run_rsa_py4j_consumer
```
On the Python side there should be console output
```
** Pelix Shell prompt **
$ Python IHello service consumer received sync response: Java says: Hi PythonSync, nice to see you
done with sayHelloAsync method
done with sayHelloPromise method
promise response: JavaPromise says: Hi PythonPromise, nice to see you
async response: JavaAsync says: Hi PythonAsync, nice to see you
```
On the Java/Karaf server-side should be console output
```
Java.sayHello called by PythonSync with message: 'Hello Java'
Java.sayHelloAsync called by PythonAsync with message: 'Hello Java'
Java.sayHelloPromise called by PythonPromise with message: 'Hello Java'```
```
## Support

For support please open an issue on this repo, [iPOPO repo](https://github.com/tcalmant/ipopo/issues), or contact [scottslewis@gmail.com](mailto:scottslewis@gmail.com)

LICENSE
=======

Python.Java Remote Services is distributed with the Apache2 license. See LICENSE in this directory for more
information.

