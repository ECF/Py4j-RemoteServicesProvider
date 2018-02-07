'''
Created on Feb 6, 2018

@author: slewis
'''

from hellomsg_pb2 import HelloMsgContent
from osgiservicebridge.protobuf import protobuf_remote_service
from osgiservicebridge.protobuf import protobuf_remote_service_method
from osgiservicebridge import ECF_SERVICE_EXPORTED_ASYNC_INTERFACES

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

