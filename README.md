Python.Java Remote Services
===========================

## NEW (6/1/2025) Remote Tools for Model Context Protocol (MCP) Servers

The [Model Context Protocol](https://modelcontextprotocol.io/introduction) is a new [specification](https://modelcontextprotocol.io/specification/2025-03-26) and [multi-language implementation](https://github.com/modelcontextprotocol) for allowing LLMs to integrate and interact with other services.

[Tools](https://modelcontextprotocol.io/docs/concepts/tools) are an important part of the specification, as they provide LLMs with the ability for LLMs to take actions, and provide standardized meta-data (aka tool descriptions) that the LLM can use decide upon and then take actions based upon the model context.

The creation of MCP servers has/is [grwoing very quickly](https://github.com/modelcontextprotocol/servers).  Currently, however, most of these implementations do not support the ability to add and remove tools from MCP servers dynamically, and require a new server (and a new set of implemented tools) to used by clients.

Enter Remote Tools for MCP Servers.  [This repo](https://github.com/ECF/Py4j-RemoteServicesProvider) makes available a [Remote Services distribution provider](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html) based upon Py4j and Python <-> Java RPC framework used by [Apache Spark](https://spark.apache.org/) and others.  Combined with [iPOPO](https://ipopo.readthedocs.io/en/3.0.0/), which is a component framework for Python, and includes RSA and a py4j distribution provider, it's now easy to use Remote Services to dynamically extend/enhance MCP python servers (FastMCP below) with new Java implemented tools.

### Example:  Remote ArtithmeticTools service

[Here is an example Java interface](https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.ai.mcp.toolservice.api/src/org/eclipse/ecf/examples/ai/mcp/toolservice/api/ArithmeticTools.java#L28) providing an ArithmeticTools declaration with MCP annotations:

```
public interface ArithmeticTools extends ToolGroupService {

	@Tool(description = "computes the sum of the two integer arguments")
	@ToolAnnotations(destructiveHint = true, title="howdy")
	@ToolResult(description = "the integer result for this tool")
	int add(@ToolParam(description = "a is the first argument") int a, @ToolParam(description = "b is the second argument") int b);

	@Tool(description = "return the product of the two given integer arguments named a and b")
	int multiply(@ToolParam(description = "a is the first argument") int a, @ToolParam(description = " b is the second argument") int b);

}
```

And [here](https://github.com/ECF/Py4j-RemoteServicesProvider/blob/master/examples/org.eclipse.ecf.examples.ai.mcp.toolservice/src/org/eclipse/ecf/examples/ai/mcp/toolservice/impl/ArithmeticToolsImpl.java#L15) is a simple implementation of this interface.  Notice that neither the service interface nor implementation class refer to any classes that are part of RS/RSA, the distribution provider, karaf, or any other framework.

### Exporting ArithmeticTools in Apache Karaf

In [Karaf 4.4.6]() this example and all supporting code can be installed with these two karaf console commands

```console
karaf@root()> feature:repo-add https://download.eclipse.org/rt/ecf/latest/site.p2/karaf-features.xml
```
then
```console
karaf@root()> feature:install ecf-rs-examples-python.java-mcp-toolservice
```
This will produce debug output to your karaf console 
```console
14:39:05.818;EXPORT_REGISTRATION;exportedSR=[org.eclipse.ecf.examples.ai.mcp.toolservice.api.ArithmeticTools];cID=URIID [uri=py4j://127.0.0.1:25333/java];rsId=1
--Endpoint Description---
<endpoint-descriptions xmlns="http://www.osgi.org/xmlns/rsa/v1.0.0">
  <endpoint-description>
    <property name="ecf.endpoint.id" value-type="String" value="py4j://127.0.0.1:25333/java"/>
    <property name="ecf.endpoint.id.ns" value-type="String" value="ecf.namespace.py4j"/>
    <property name="ecf.endpoint.ts" value-type="Long" value="1748727545801"/>
    <property name="ecf.rsvc.id" value-type="Long" value="1"/>
    <property name="endpoint.framework.uuid" value-type="String" value="82798710-8368-461b-b07d-0250e9e064cc"/>
    <property name="endpoint.id" value-type="String" value="74b58995-4e59-4936-8743-4599066c1b10"/>
    <property name="endpoint.service.id" value-type="Long" value="146"/>
    <property name="objectClass" value-type="String">
      <array>
        <value>org.eclipse.ecf.examples.ai.mcp.toolservice.api.ArithmeticTools</value>
      </array>
    </property>
    <property name="osgi.ds.satisfying.condition.target" value-type="String" value="(osgi.condition.id=true)"/>
    <property name="remote.configs.supported" value-type="String">
      <array>
        <value>ecf.py4j.host</value>
      </array>
    </property>
    <property name="remote.intents.supported" value-type="String">
      <array>
        <value>passByReference</value>
        <value>exactlyOnce</value>
        <value>ordered</value>
        <value>py4j</value>
        <value>py4j.async</value>
        <value>osgi.async</value>
        <value>osgi.private</value>
        <value>osgi.confidential</value>
      </array>
    </property>
    <property name="service.imported" value-type="String" value="true"/>
    <property name="service.imported.configs" value-type="String">
      <array>
        <value>ecf.py4j.host</value>
      </array>
    </property>
  </endpoint-description>
</endpoint-descriptions>
---End Endpoint Description
---
This output shows the remote service was exported via the py4j-remoteservices provider with all the RSA-specified remote service meta-data.  This means that
the Karaf process is now listening on Java port 25333 (default py4j port) for connections from MCP servers.

### Running the Python MCP Server

[iPOPO](https://ipopo.readthedocs.io/en/3.0.0/) provides a spec-compliant implementaion of the [Remote Service Admin specification)(https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.remoteserviceadmin.html).  This allow a python MCP server to dynamically discover and inject
remote services (like ArtithmeticTools) into a Python-base server such as FastMCP.

TODO

## NEW (4/28/2025) Bndtools Template for Python.Java Remote Services Development

There has been a new project template added to the [ECF Bndtools Workspace Template](https://github.com/ECF/bndtools.workspace) that uses the [ECF Python.Java Distribution Provider](https://github.com/ECF/Py4j-RemoteServicesProvider).  This distribution provider is based upon py4j, which supports high performance remote procedure call between python and java processes.

To try it out after installing Bndtools 7.1 and the ECF tools add ons

1. Create a new Bndtools Workspace using the [ECF Bndtools Workspace Template](https://github.com/ECF/bndtools.workspace)

![bndtoolsnewwkspace](https://github.com/user-attachments/assets/95ec5792-6bc2-4c88-990d-4e8d3350627e)

2. Create a new Bnd OSGi project

![bndtoolsnewproject](https://github.com/user-attachments/assets/fa2641e6-a074-4796-b761-f79999b9ba06)

3. Open the projectName.hellopython.javahost.bndrun file in the project directory
   
![bndtoolsbndrun](https://github.com/user-attachments/assets/9bf8a380-9ee7-4e48-ac49-1627cf3ace75)

4. Choose 'Resolve' and then 'Update'

5. Select Debug OSGi to start the example application (Java)

![bndtoolsdebug](https://github.com/user-attachments/assets/9fa2536f-9748-4f5f-94bc-b78374f436a8)

Running Python Example Program 

1. Install [iPOPO v 3.1.0](https://ipopo.readthedocs.io) in your Python (3.9 or greater) local environment

2. In a command shell or IDE, navigate to the project directory and run the run_python_example.py script

```
python run_python_example.py
```
The examples will output progress to their respective consoles as the remote services are made exported,
discovered, and imported by the java process or the python process.  

![bndtoolspython](https://github.com/user-attachments/assets/d5bbd4e4-d57c-412a-a198-fe16ed76a95d)

Most of the code that produces output is available in the example project. For java: src/main/java/.../hello/*.java 
and python: python-src/samples/rsa

## Using Python.Java Remote Services to Resolve Python Imports

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

