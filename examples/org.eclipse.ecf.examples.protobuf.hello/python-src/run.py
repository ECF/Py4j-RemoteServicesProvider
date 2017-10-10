'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge, _wait_for_sec
from osgiservicebridge import ECF_SERVICE_EXPORTED_ASYNC_INTERFACES

from osgiservicebridge.protobuf import protobuf_remote_service, protobuf_remote_service_method
from hellomsg_pb2 import HelloMsgContent

@protobuf_remote_service(objectClass=['org.eclipse.ecf.examples.protobuf.hello.IHello'],export_properties = { ECF_SERVICE_EXPORTED_ASYNC_INTERFACES: 'org.eclipse.ecf.examples.protobuf.hello.IHello' })
class HelloServiceImpl:
    
    @protobuf_remote_service_method(arg_type=HelloMsgContent,return_type=HelloMsgContent)
    def sayHello(self,pbarg):
        print("sayHello called with arg="+str(pbarg))
        resmsg = HelloMsgContent()
        resmsg.h = 'Another response from Python'
        resmsg.f = 'frompython'
        resmsg.to = 'tojava'
        resmsg.hellomsg = 'Greetings from Python!!'
        for x in range(0,5):
            resmsg.x.append(float(x))
        return resmsg
    
    
if __name__ == '__main__':
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    hsid = bridge.export(HelloServiceImpl())
    print("exported")
    _wait_for_sec(5)
    bridge.unexport(hsid)
    print("unexported")
    bridge.disconnect()
    print("disconnected...exiting")
