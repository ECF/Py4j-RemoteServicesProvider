'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge

class MyClass(object):
    
    def _raw_bytes_from_java(self,methodName,serializedArgs):
        print("methodName="+str(methodName))
        print("serializedArgs="+str(serializedArgs))
    
    class Java:
        implements = ['org.eclipse.ecf.examples.protobuf.hello.IHello']

if __name__ == '__main__':
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    bridge.export(MyClass())
    print("exported")
