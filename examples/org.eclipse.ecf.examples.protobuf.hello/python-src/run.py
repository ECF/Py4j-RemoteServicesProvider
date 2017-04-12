'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge
from hellomsg_pb2 import HelloMsgContent

class MyClass(object):
    
    
    def _raw_bytes_from_java(self,methodName,serializedArgs):
        #print("methodName="+str(methodName))
        #print("serializedArgs="+str(serializedArgs))
        pbarg = HelloMsgContent()
        pbarg.ParseFromString(serializedArgs)
        print("input arg="+str(pbarg))
        
        # do something with pbarg
        
        # now create a response
        msgcontent = HelloMsgContent()
        msgcontent.h = 'phere'
        msgcontent.f = 'pslewis'
        msgcontent.to = 'jslewis'
        msgcontent.hellomsg = 'hi from python'
        for x in range(0,5):
            msgcontent.x.append(float(x))
        return msgcontent.SerializeToString()
    
    class Java:
        implements = ['org.eclipse.ecf.examples.protobuf.hello.IHello']
        service_exported_configs = ['ecf.py4j.host.python.pb']

if __name__ == '__main__':
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    bridge.export(MyClass())
    print("exported")
