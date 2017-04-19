'''
Created on Apr 11, 2017

@author: slewis
'''
from osgiservicebridge.bridge import Py4jServiceBridge, flushfile

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
from osgiservicebridge.protobuf import ProtobufServiceImpl, protobufmethod, PB_SERVICE_EXPORTED_CONFIGS_DEFAULT
#must also refer to arg_type from protoc-generated protocol buffers file
from hellomsg_pb2 import HelloMsgContent

'''
Create HelloServiceImpl class.  This class should inherit from the ProtobufServiceImpl.
This class should have methods defined whose names correspond to the 
java interfaces(s) specified in the Java.implements property (list).
These methods should be decorated with the @protobufmethod decorator 
to declare the arg_type.  
'''
class HelloServiceImpl(ProtobufServiceImpl):
    '''
    This method implements the IHello service interface declared in Java.  
    FQName = org.eclipse.ecf.examples.protobuf.hello.IHello
    The @protobufmethod is a decorator that wraps the associated method
    and allows the arg_type to be specified as a protocol buffers message
    type.  Note that arg_type must be specified, and must be a generated
    type
    '''
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
        '''
        This Java static class must have a implements attribute that defines an
        array of strings corresponding to the java interfaces that this class implements.
        The implements attribute must be an array of str and must have at least on
        string.
        '''
        implements = ['org.eclipse.ecf.examples.protobuf.hello.IHello']
        '''
        The service_exported_configs allows us to specify the remote service distribution
        provider, and in this case we used the ID of the protocol buffers provider
        defined in PB_SERVICE_EXPORTED_CONFIGS_DEFAULT.  This is an array of strings.
        Note that any attribute with name 'foo_bar_baz' will be converted during
        export to specify the remote service properties specified by the OSGi RSA 
        spec.  For example:  service_exported_configs will be automatically converted
        to 'service.exported.configs' = <value> and placed into the exported properies
        '''
        service_exported_configs = PB_SERVICE_EXPORTED_CONFIGS_DEFAULT

if __name__ == '__main__':
    bridge = Py4jServiceBridge()
    print("bridge created")
    bridge.connect()
    print("bridge connected")
    bridge.export(HelloServiceImpl())
    print("exported")
