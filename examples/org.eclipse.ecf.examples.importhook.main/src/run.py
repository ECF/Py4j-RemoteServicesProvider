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
    # connect
    bridge.connect(path_hook=OSGIPythonModulePathHook(bridge))
    # import package from java
    from foo.bar.baz import Bar
    # create instance
    bar = Bar()
    
    _wait_for_sec(20)
    
    bridge.disconnect()
    
