'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge
from osgiservicebridge.protobuf import ProtobufServiceImpl, protobufmethod, PB_SERVICE_EXPORTED_CONFIGS_DEFAULT

from hellomsg_pb2 import HelloMsgContent

class HelloServiceImpl(ProtobufServiceImpl):
    
    @protobufmethod(arg_type=HelloMsgContent)
    def sayHello(self,pbarg):
        print("sayHello called with arg="+str(pbarg))
        resmsg = HelloMsgContent()
        resmsg.h = 'phere'
        resmsg.f = 'pslewis'
        resmsg.to = 'jslewis'
        resmsg.hellomsg = 'hi from python'
        for x in range(0,5):
            resmsg.x.append(float(x))
        return resmsg
    
    class Java:
        implements = ['org.eclipse.ecf.examples.protobuf.hello.IHello']
        service_exported_configs = PB_SERVICE_EXPORTED_CONFIGS_DEFAULT

if __name__ == '__main__':
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    bridge.export(HelloServiceImpl())
    print("exported")
