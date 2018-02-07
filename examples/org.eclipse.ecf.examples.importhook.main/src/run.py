'''
Created on Jan 26, 2018

@author: slewis
'''

from osgiservicebridge.bridge import Py4jServiceBridge, _wait_for_sec,\
    OSGIPythonModulePathHook

from osgiservicebridge.protobuf import ProtobufServiceRegistry

if __name__ == '__main__':
    # create service registry
    java_service_registry = ProtobufServiceRegistry()
    # pass service registry to service bridge
    bridge = Py4jServiceBridge(java_service_registry)
    # connect and setup the OSGIPythonModulePathHook as the path
    # hook.  The path hook allows subsequent imports
    # to be resolved by java ModuleResolver services.  See
    # the class org.eclipse.ecf.examples.importhook.module.ExampleBundleModuleResolver
    # which resolves the foo/bar 
    bridge.connect(path_hook=OSGIPythonModulePathHook(bridge))
    
    # this import is resolved via the org.eclipse.ecf.examples.importhook.module
    # bundle
    from foo.bar.baz import Bar
    
    b = Bar()
    
    _wait_for_sec(20)
    bridge.disconnect()
    
