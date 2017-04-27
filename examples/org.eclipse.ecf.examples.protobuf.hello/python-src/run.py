'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge, flushfile, _wait_until_done

#turn on logging for osgiservicebridge.protobuf
import logging
logger = logging.getLogger("osgiservicebridge.protobuf")
logger.setLevel(logging.DEBUG)
logger.addHandler(logging.StreamHandler())
#also turn on logging for osgiservicebridge.bridge
logger = logging.getLogger("osgiservicebridge.bridge")
logger.setLevel(logging.DEBUG)
logger.addHandler(logging.StreamHandler())
#turn on sys.out flush so we see logging output in java
import sys
sys.stdout = flushfile(sys.stdout)

#create HelloServiceImpl class.  Must inherit from ProtobufServiceImpl
from osgiservicebridge.protobuf import ProtoBufRemoteService, ProtoBufRemoteServiceMethod
#must also refer to arg_type from protoc-generated protocol buffers file
from hellomsg_pb2 import HelloMsgContent


@ProtoBufRemoteService(objectClass=['org.eclipse.ecf.examples.protobuf.hello.IHello'])
class HelloServiceImpl:
    
    @ProtoBufRemoteServiceMethod(arg_type=HelloMsgContent)
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
    
    
if __name__ == '__main__':
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    bridge.export(HelloServiceImpl())
    print("exported")
    # waits until the process is terminated
    _wait_until_done()
