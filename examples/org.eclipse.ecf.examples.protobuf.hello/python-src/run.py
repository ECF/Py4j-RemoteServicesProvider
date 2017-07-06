'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge, _wait_for_sec

#turn on logging for osgiservicebridge.protobuf
#import logging
#logger = logging.getLogger("osgiservicebridge.protobuf")
#logger.setLevel(logging.DEBUG)
#logger.addHandler(logging.StreamHandler())
#also turn on logging for osgiservicebridge.bridge
#logger = logging.getLogger("osgiservicebridge.bridge")
#logger.setLevel(logging.DEBUG)
#logger.addHandler(logging.StreamHandler())
#turn on sys.out flush so we see logging output in java
#logger = logging.getLogger("py4j")
#logger.setLevel(logging.DEBUG)
#logger.addHandler(logging.StreamHandler())
#import sys
#sys.stdout = flushfile(sys.stdout)

#create HelloServiceImpl class.  Must inherit from ProtobufServiceImpl
from osgiservicebridge.protobuf import protobuf_remote_service, protobuf_remote_service_method
#must also refer to arg_type from protoc-generated protocol buffers file
from hellomsg_pb2 import HelloMsgContent


@protobuf_remote_service(objectClass=['org.eclipse.ecf.examples.protobuf.hello.IHello'])
class HelloServiceImpl:
    
    @protobuf_remote_service_method(arg_type=HelloMsgContent)
    def sayHello(self,pbarg):
        print("sayHello called with arg="+str(pbarg))
        resmsg = HelloMsgContent()
        resmsg.h = 'response'
        resmsg.f = 'python'
        resmsg.to = 'java'
        resmsg.hellomsg = 'hi from python'
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
    _wait_for_sec(10)
    bridge.unexport(hsid)
    print("unexported")
    bridge.disconnect()
    print("disconnected...exiting")
