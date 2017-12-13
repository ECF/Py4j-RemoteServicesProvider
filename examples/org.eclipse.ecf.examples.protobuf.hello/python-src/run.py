'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge, _wait_for_sec
from osgiservicebridge import ECF_SERVICE_EXPORTED_ASYNC_INTERFACES

from osgiservicebridge.protobuf import protobuf_remote_service, protobuf_remote_service_method,\
    PythonServiceExporter
from hellomsg_pb2 import HelloMsgContent

import logging
import sys
timing = logging.getLogger("timing.osgiservicebridge.protobuf")

timing.setLevel(logging.DEBUG)
ch = logging.StreamHandler(stream = sys.stdout)
ch.setLevel(logging.DEBUG)

timing.addHandler(ch)

from osgiservicebridge.protobuf import ProtobufServiceRegistry

def create_hellomsgcontent(message):
    resmsg = HelloMsgContent()
    resmsg.h = 'Another response from Python'
    resmsg.f = 'frompython'
    resmsg.to = 'tojava'
    resmsg.hellomsg = message
    for x in range(0,5):
        resmsg.x.append(float(x))
    return resmsg

@protobuf_remote_service(objectClass=['org.eclipse.ecf.examples.protobuf.hello.IHello'],export_properties = { ECF_SERVICE_EXPORTED_ASYNC_INTERFACES: 'org.eclipse.ecf.examples.protobuf.hello.IHello' })
class HelloServiceImpl:
    
    def __init__(self,props):
        self.props = props
        
    @protobuf_remote_service_method(arg_type=HelloMsgContent,return_type=HelloMsgContent)
    def sayHello(self,pbarg):
        print("sayHello called with arg="+str(pbarg))
        return create_hellomsgcontent('responding back to java hello ')

if __name__ == '__main__':
    # create service registry
    java_service_registry = ProtobufServiceRegistry()
    # pass service registry to service bridge
    bridge = Py4jServiceBridge(java_service_registry)
    # connect
    bridge.connect()
    # export a new PythonServiceExporter
    exporter_id = bridge.export(PythonServiceExporter(bridge))
    
        #wait for a few seconds
    _wait_for_sec(5)

    # export a new instance of HelloServiceImpl.   This will trigger java HelloImpl to be exported back to us
    #hello_id = bridge.export(HelloServiceImpl())
    #print("exported python IHello")
    #  the following lookup_services call returns an array
    # of 0 or more instances of osgiservicebridge.bridge.JavaRemoteService class
    rsvcs = java_service_registry.lookup_services('org.eclipse.ecf.examples.protobuf.hello.IHello')
    if len(rsvcs) > 0:
        theproxy = rsvcs[0].get_proxy()
        hellomsg = create_hellomsgcontent('saying hello from python to java service')
        print("making java IHello service request")
        response = theproxy.sayHello(hellomsg)
        print("proxy response="+str(response))
    else:
        print('could not get proxy for java IHello service')
    #wait for a few seconds
    _wait_for_sec(5)

    ##bridge.unexport(hello_id)
    bridge.unexport(exporter_id)
    print("unexported")
    bridge.disconnect()
    print("disconnected...exiting")
