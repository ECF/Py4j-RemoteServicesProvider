'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge
from hellomsg_pb2 import HelloMsgContent
from functools import wraps

def pbdecorator(func):
    @wraps(func)
    def decorate(*args,**kwargs):
        argClass = HelloMsgContent
        if len(args) > 1 and argClass:
            argInst = argClass()
            argInst.ParseFromString(args[1])
        respb = func(args[0],argInst)
        resBytes = None
        if respb:
            resBytes = respb.SerializeToString()
        return resBytes
    return decorate

class PBService(object):
    
    def _raw_bytes_from_java(self,methodName,serializedArgs):
        lastdot = methodName.rfind('.')
        if lastdot >= 0:
            methodName = methodName[lastdot+1:]
        return getattr(self,methodName)(serializedArgs)
    
    class Java:
        implements = ['org.eclipse.ecf.examples.protobuf.hello.IHello']
        service_exported_configs = ['ecf.py4j.host.python.pb']

class MyClass(PBService):
    
    @pbdecorator
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
    inst = MyClass()
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    bridge.export(MyClass())
    print("exported")
