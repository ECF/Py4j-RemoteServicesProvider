'''
OSGi service bridge Google protocol buffers (protobuf) support
:author: Scott Lewis
:copyright: Copyright 2016, Composent, Inc.
:license: Apache License 2.0
:version: 1.2.0
    Copyright 2017 Composent, Inc. and others
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
'''
from functools import wraps
from logging import getLogger as getLibLogger
import sys
import osgiservicebridge
from osgiservicebridge import ENDPOINT_PACKAGE_VERSION_

import time

from google.protobuf.message import Message
from google.protobuf.descriptor_pb2 import DescriptorProto
from google.protobuf.descriptor import MakeDescriptor
from google.protobuf.reflection import MakeClass
from osgiservicebridge.bridge import JavaRemoteServiceRegistry, Py4jServiceBridgeEventListener

from osgiservicebridge import ECF_SERVICE_EXPORTED_ASYNC_INTERFACES

from osgiservicebridge.exporter_pb2 import ExportRequest,ExportResponse,UnexportRequest,UnexportResponse

# Documentation strings format
__docformat__ = "restructuredtext en"

from osgiservicebridge.version import __version__ as __v__
# Version
__version__ = __v__

# ------------------------------------------------------------------------------
_logger = getLibLogger(__name__)
_timing = getLibLogger("timing."+__name__)
# ------------------------------------------------------------------------------

# PY2/PY3 version differentiation.
PY2 = sys.version_info[0] == 2
PY3 = sys.version_info[0] == 3
PY34 = sys.version_info[0:2] >= (3, 4)

JAVA_HOST_CONFIG_TYPE = 'ecf.py4j.host.pb';
PYTHON_HOST_CONFIG_TYPE = 'ecf.py4j.host.python.pb';
JAVA_CONSUMER_CONFIG_TYPE = 'ecf.py4j.consumer.pb';
PYTHON_CONSUMER_CONFIG_TYPE = 'ecf.py4j.consumer.python.pb';

PB_SERVICE_EXPORTED_CONFIG_DEFAULT=PYTHON_HOST_CONFIG_TYPE
PB_SERVICE_EXPORTED_CONFIGS_DEFAULT=[PB_SERVICE_EXPORTED_CONFIG_DEFAULT]

PB_SERVICE_RETURN_TYPE_ATTR = '_return_type'
PB_SERVICE_ARG_TYPE_ATTR = '_arg_type'
PB_SERVICE_SOURCE_ATTR = '_source'

def get_instance_method(instance, method_name):
    '''
    Return the method_name function for instance.  
    :param instance: the instance to get the method from.  Must not be None.
    :param method_name: the method name (str) of the method to return.  Must not be None.
    :return: method with method_name on instance, None if not present.
    '''
    return getattr(instance, method_name, None)

def get_instance_type(instance, method_name, func_attr_name):
    '''
    Return the class of the func_attr_name ('_return_type', or '_arg_type') on the method
    with method_name on instance.  
    :param instance:  the instance to query.  Must not be None.
    :param method_name:  the method_name to access
    :param func_attr_name:  the function attr name, either '_return_type' or '_arg_type' 
    that has the class of the function's return type or argument type.
    :return: the class of the function's return type or argument type based upon the func_attr_name
    parameter.  May be None if no function with given method_name on instance, or no
    attribute on function with name func_attr_name.
    '''
    f = get_instance_method(instance, method_name)
    if not f:
        return None
    return getattr(f, func_attr_name, None)

def get_instance_return_type(instance, method_name):
    '''
    Return the class for the return_type for method_name on instance.
    :param instance:  the instance to query.  Must not be None.
    :param method_name: the method_name to access
    :return: the class of the return type.  Will be None if no function with method_name
    or no return type attribute on function.
    '''
    return get_instance_type(instance, method_name, PB_SERVICE_RETURN_TYPE_ATTR)

def get_instance_arg_type(instance, method_name):
    '''
    Return the class for the arg_type  for method_name on instance.
    :param instance:  the instance to query.  Must not be None.
    :param method_name: the method_name to access.  Must not be None.
    :return: the class of the argument type.  Will be None if no function with method_name
    or no return type attribute on function.
    '''
    return get_instance_type(instance, method_name, PB_SERVICE_ARG_TYPE_ATTR)

def create_return_instance(instance, method_name):
    '''
    Return a new instance of the appropriate return type for the given method
    on the given instance.
    :param instance: the instance to query.  Must not be None.
    :param method_name: the method name to access.  Must not be None.
    :return: new instance of return type for given method.  Will return None
    if there is no method_name on instance, or it does not have the 
    PB_SERVICE_RETURN_TYPE_ATTR on the function.
    '''
    ret_type = get_instance_return_type(instance, method_name)
    if ret_type is not None:
        return ret_type()
    return None

def get_method_source(method):
    return getattr(method,PB_SERVICE_SOURCE_ATTR, None)

def set_method_source(method, sourcecode):
    setattr(method, PB_SERVICE_ARG_TYPE_ATTR, sourcecode)
    
def update_method(method,oldfunc,new_source):
    if method:
        setattr(method, PB_SERVICE_ARG_TYPE_ATTR, getattr(oldfunc, PB_SERVICE_ARG_TYPE_ATTR, None))
        setattr(method, PB_SERVICE_RETURN_TYPE_ATTR, getattr(oldfunc, PB_SERVICE_RETURN_TYPE_ATTR, None))
        setattr(method, PB_SERVICE_SOURCE_ATTR, new_source)

def get_name_and_type_dict(m):
    result = dict()
    for key in m:
        val = m[key]
        if val is None:
            result[key] = 'None'
        else:
            result[key] = fully_qualified_classname(type(val))
    return result

def fully_qualified_classname(c):
    module = c.__module__
    if module is None or module == str.__class__.__module__:
        return c.__name__
    return module + '.' + c.__name__
                
def _raw_bytes_from_java(self,methodName,serializedArgs):
    return getattr(self,methodName)(serializedArgs)

def bytes_to_pmessage(pmessage_class, message_bytes):
    if not message_bytes:
        return None
    pmessage_instance = None
    try:
        pmessage_instance = pmessage_class()
    except Exception as e:
        _logger.exception('bytes_to_pmessage could not create instance of message_class=%s' % (pmessage_class))
        raise e
    if PY2:
        message_bytes = str(message_bytes)
    #Then pass to parser and return
    t0 = time.time()
    try:
        # deserialze
        pmessage_instance.ParseFromString(message_bytes)
    except Exception as e:
        _logger.exception('bytes_to_pmessage could not parse message from bytes. message_instance=%' % (pmessage_instance))
        raise e
    t1 = time.time()
    _timing.debug("protobuf.bytes_to_pmessage;diff="+str(1000*(t1-t0))+"ms")
    return pmessage_instance

def bytes_to_jmessage(jmessage_class, message_bytes):
    if not message_bytes:
        return None
    parser_instance = None
    try:
        parser_method = jmessage_class.getMethod("parser", None)
        parser_instance = parser_method.invoke(None,None)
    except Exception as e:
        _logger.exception('bytes_to_jmessage could not create instance of message_class=%s' % (jmessage_class))
        raise e
    if PY2:
        message_bytes = bytearray(message_bytes)
    #Then pass to parser and return
    t0 = time.time()
    jmessage_instance = None
    try:
        # deserialze
        jmessage_instance = parser_instance.parseFrom(message_bytes)
    except Exception as e:
        _logger.exception('bytes_to_jmessage could not parse message from bytes.  message_instance=%' % (jmessage_instance))
        raise e
    t1 = time.time()
    _timing.debug("protobuf.bytes_to_jmessage;ddiff="+str(1000*(t1-t0))+"ms")
    return jmessage_instance

def pmessage_to_bytes(pmessage):
    pmessage_bytes = None
    t0 = time.time()
    if pmessage:
        try:
            pmessage_bytes = pmessage.SerializeToString()
        except Exception as e:
            _logger.exception('pmessage_to_bytes failed.  pmessage=%s' % (pmessage))
            raise e
        # If Python 2 we convert the string to bytearray
        if PY2:
            pmessage_bytes = bytearray(pmessage_bytes)
    t1 = time.time()
    _timing.debug("protobuf.pmessage_to_bytes;diff="+str(1000*(t1-t0))+"ms")
    return pmessage_bytes

def jmessage_to_bytes(jmessage):
    jmessage_bytes = None
    t0 = time.time()
    if jmessage:
        try:
            jmessage_bytes = jmessage.toByteArray()
        except Exception as e:
            _logger.exception('jmessage_to_bytes failed.  jmessage=%s' % (jmessage))
            raise e
        # If Python 2 we convert the string to bytearray
        if PY2:
            jmessage_bytes = bytearray(jmessage_bytes)
    t1 = time.time()
    _timing.debug("protobuf.jmessage_to_bytes;diff="+str(1000*(t1-t0))+"ms")
    return jmessage_bytes

def pmessage_to_jmessage(jmessage_class, pmessage):
    if not pmessage:
        return None
    message_bytes = pmessage_to_bytes(pmessage)
    if not message_bytes:
        return None
    return bytes_to_jmessage(jmessage_class, message_bytes)
    
def jmessage_to_pmessage(pmessage_class, jmessage):
    if not jmessage:
        return None
    message_bytes = jmessage_to_bytes(jmessage)
    if not message_bytes:
        return None
    return bytes_to_pmessage(pmessage_class, message_bytes)

    
def protobuf_remote_service(**kwargs):    
    '''
    Class decorator for protobuf-based remote services.  This class decorator is intended to be used as follows:
    
    @protobuf_remote_service(objectClass=['fully.qualified.java.interface.package.Classname',...],export_properties={ 'myprop': 'myvalue' })
    class MyClass:
        pass
        
    Example:
    
    @protobuf_remote_service(objectClass=['org.eclipse.ecf.examples.protobuf.hello.IHello'])
    class HelloServiceImpl:

    :param kwargs: the kwargs dict required to have 'objectClass' item and
    and (optional) 'export_properties' item
    '''
    def decorate(cls):
        # setup the protocol buffers java-side config
        pb_svc_props = { osgiservicebridge.SERVICE_EXPORTED_CONFIGS: PB_SERVICE_EXPORTED_CONFIGS_DEFAULT }
        # get the 'service_properties' value from kwargs, if present
        args_props = kwargs.pop(osgiservicebridge.EXPORT_PROPERTIES_NAME,None)
        # if it is present, then merge/override with the values given
        if args_props:
            pb_svc_props = osgiservicebridge.merge_dicts(pb_svc_props,args_props)
        # then set/reset the kwargs service_properties value
        kwargs[osgiservicebridge.EXPORT_PROPERTIES_NAME] = pb_svc_props
        # call _modify_remoteservice_class to modify the class itself (add Java inner 
        # class and set implements and service_properties values
        cls = osgiservicebridge._modify_remoteservice_class(cls,kwargs)
        # set pbbuffers callback to _raw_bytes_from_java
        cls._raw_bytes_from_java = _raw_bytes_from_java
        return cls
    return decorate

def protobuf_remote_service_method(arg_type,return_type=None):
    '''
    Method decorator for protobuf-based remote services method.  This class decorator is intended to be used as follows:
    
    @protobuf_remote_service_method(arg_type=<arg class>,return_type=<return_class>)
    def <methodName>(self,<argName>):
        
    For example:
    
    @protobuf_remote_service_method(arg_type=HelloMsgContent,return_type=HelloMsgContent)
    def sayHello(self,pbarg):
        
    Where HelloMsgContent is a protoc-generated python class (issubclass(arg_type,google.protobuf.message.Message) 
    is true).  When called, pbarg is guaranteed to be an instance of HelloMsgContent.
    The sayHello method is also required to return an instance of HelloMsgContent or None.
    
    :param arg_type: the class of the pbarg type (will be instance of HelloMsgContent)
    '''
    issubclass(arg_type, Message)
    def pbwrapper(func):
        def logged_wrapped_exception(msg):
            exc_type, exc_value, exc_traceback = sys.exc_info()
            from traceback import format_exception
            lines = format_exception(exc_type, exc_value, exc_traceback)
            lines[1:2] = []
            _logger.error(msg + ('\n%s' % (''.join(lines))))

        func._arg_type = arg_type
        func._return_type = return_type
        func._source = None
        try:
            from inspect import getsource
            setattr(func, PB_SERVICE_SOURCE_ATTR, getsource(func))
        except:
            pass
        @wraps(func)
        def wrapper(*args):
            # set argClass to arg_type
            argClass = arg_type
            # signature should be:  self,arg.  Pass args[1]...the actual argument
            # ...to deserialize byte[] into pb Message into argInst
            argInst = bytes_to_pmessage(argClass, args[1])
            respb = None
            t0 = time.time()
            try:
                # Now actually call function, with self,pbMessage
                respb = func(args[0],argInst)
            except Exception as e:
                logged_wrapped_exception('Remote method invoke failed')
                raise e
            t1 = time.time()
            _timing.debug("protobuf.exec;time="+str(1000*(t1-t0))+"ms")
            if not func._return_type is None:
                isinstance(respb,func._return_type)
            return pmessage_to_bytes(respb)
        return wrapper
    return pbwrapper

def get_python_return_type(java_return_class):
    jdesc_bytes = java_return_class.getMethod("getDescriptor",None).invoke(None,None).toProto().toByteArray()
    if PY2:
        jdesc_bytes = str(jdesc_bytes)
    pdesc_proto = DescriptorProto()
    pdesc_proto.MergeFromString(jdesc_bytes)
    return MakeClass(MakeDescriptor(pdesc_proto))
    
def get_interface_methods(java_interface_class, proxy):
    result_methods = []
    try:
        jmethods = java_interface_class.getMethods()
        for jmethod in jmethods:
            jmethod_name = jmethod.getName()
            if (jmethod_name not in JAVA_OBJECT_METHODS):
                java_parameter_types = jmethod.getParameterTypes()
                java_return_class = jmethod.getReturnType()
                python_return_type = get_python_return_type(java_return_class)
                result_methods.append(ProtobufServiceMethod(proxy,jmethod_name,java_parameter_types,python_return_type))
        return result_methods
    except Exception as e:
        _logger.exception('Could not get interface methods from java_interface_class='+str(java_interface_class))
        raise e
    
def get_interfaces_methods(jvm, interfaces, proxy):
    result = {}
    for interface in interfaces:
        interface_class = jvm.java.lang.Class.forName(interface)
        result[interface] = get_interface_methods(interface_class, proxy)
    return result

class ProtobufServiceMethod(object):
    
    def __init__(self,proxy,name,java_arg_types,python_return_type):
        self._java_proxy = proxy
        self._methodname = name
        self._java_arg_types = java_arg_types
        self._python_return_type = python_return_type
    
    def __call__(self,*args):
        # serialize python message to bytes
        pmessage_bytes = pmessage_to_bytes(args[0])
        jmessage = None
        if pmessage_bytes:
            # convert bytes to java message
            jmessage = bytes_to_jmessage(self._java_arg_types[0], pmessage_bytes)
        # make remote java call with java message
        t0 = time.time()
        jresult = None
        try:
            jresult = getattr(self._java_proxy,self._methodname)(jmessage)
        except Exception as e:
            _logger.exception("Exception making remote java call to methodname="+self._methodname)
            raise e
        t1 = time.time()
        _timing.debug("protobuf.exec;time="+str(1000*(t1-t0))+"ms")
        if not jresult:
            return None
        # convert java result to bytes
        jbytes = jmessage_to_bytes(jresult)
        if PY2:
            jbytes = str(jbytes)
        # convert bytes to python return type
        return bytes_to_pmessage(self._python_return_type, jbytes)

class ProtobufServiceProxy(object):
    
    def __init__(self, jvm, interfaces, proxy):
        self._interfaces = get_interfaces_methods(jvm,interfaces,proxy)
        
    def _find_java_method(self,name):
        for method_list in self._interfaces.values():
            for method in method_list:
                if name  == method._methodname:
                    return method
        return None
    
    def __getattr__(self, name):
        if name == "__call__":
            raise AttributeError
            
        java_method = self._find_java_method(name)
        if not java_method:
            raise AttributeError
        
        return java_method

class ProtobufServiceRegistry(Py4jServiceBridgeEventListener,JavaRemoteServiceRegistry):
    
    def __init__(self):
        super(ProtobufServiceRegistry, self).__init__()
        
    def service_imported(self, servicebridge, endpointid, proxy, endpoint_props):
        imported_configs = endpoint_props[osgiservicebridge.SERVICE_IMPORTED_CONFIGS]
        theproxy = None
        if (osgiservicebridge.protobuf.JAVA_HOST_CONFIG_TYPE in imported_configs):
            theproxy = ProtobufServiceProxy(servicebridge._gateway.jvm,endpoint_props[osgiservicebridge.OBJECT_CLASS],proxy)
        else:
            theproxy = proxy
            
        self._add_remoteservice(endpoint_props, theproxy)
        
    def service_modified(self, servicebridge, endpointid, proxy, endpoint_props):
        self._modify_remoteservice(endpointid, endpoint_props)
    
    def service_unimported(self, servicebridge, endpointid, proxy, endpoint_props):
        self._remove_remoteservice(endpointid)

import importlib
import inspect

JAVA_OBJECT_METHODS = [ 'equals', 'hashCode', 'wait', 'notify', 'notifyAll', 'getClass', 'toString']

PYTHON_SERVICE_EXPORTER_PACKAGE='org.eclipse.ecf.python.protobuf'
PYTHON_SERVICE_EXPORTER_PACKAGE_VERSION='1.0.0'
PYTHON_SERVICE_EXPORTER=PYTHON_SERVICE_EXPORTER_PACKAGE + '.IPythonServiceExporter'

@protobuf_remote_service(
    objectClass=[PYTHON_SERVICE_EXPORTER],export_properties = { ECF_SERVICE_EXPORTED_ASYNC_INTERFACES: '*',
                                                                 ENDPOINT_PACKAGE_VERSION_+PYTHON_SERVICE_EXPORTER_PACKAGE: PYTHON_SERVICE_EXPORTER_PACKAGE_VERSION })
class PythonServiceExporter(object):
    
    def __init__(self,bridge):
        self._bridge = bridge
        
    def _create_error_export_response(self, message):
        result = ExportResponse()
        result.message = message
        return result
    
    def _create_success_export_response(self, endpoint_id):
        result = ExportResponse()
        result.endpoint_id = endpoint_id
        return result
    
    def _load_module(self,module_name):
        module_ = self.__class__.__module__
        if not module_name:
            return sys.modules['__main__']
        try:
            try:
                module_ = sys.modules[module_name]
            except KeyError:
                module_ = importlib.import_module(module_name)
                sys.modules[module_name] = module_
        except (ImportError, IOError) as ex:
            _logger.exception('Could not load module='+module_name,ex)
            return None
        return module_
    
    def _get_class_from_module(self,module,class_name):
        def pred(clazz):
            return inspect.isclass(clazz)
        moduleclasses = inspect.getmembers(module, pred)
        if not moduleclasses:
            return None
        else:
            for t in moduleclasses:
                if t[0] == class_name:
                    return t[1]
        return None
    
    @protobuf_remote_service_method(arg_type=ExportRequest,return_type=ExportResponse)
    def createAndExport(self, export_request): 
        try:
            module = self._load_module(export_request.module_name)
            if not module:
                return self._create_error_export_response('Cannot get module for module_name='+\
                                                          str(export_request.module_name))
            clazz = self._get_class_from_module(module, export_request.class_name)
            if not clazz:
                return self._create_error_export_response('Cannot get class for class_name='+\
                                                          str(export_request.class_name))
            args = export_request.creation_args
            if not args or len(args) <= 0:
                inst = clazz()
            else:
                inst = clazz(args)
            export_id = self._bridge.export(inst,export_request.overriding_export_props)
            return self._create_success_export_response(export_id)
        except Exception as ex:
            _logger.exception('Could not create and export with request='+str(export_request))
            return self._create_error_export_response(str(ex))
        
    @protobuf_remote_service_method(arg_type=UnexportRequest,return_type=UnexportResponse)
    def unexport(self, unexport_request):
        endpoint_id = self._bridge.unexport(unexport_request.endpoint_id)
        result = UnexportResponse()
        result.endpoint_id = endpoint_id
        if endpoint_id:
            result.success = True
        else:
            result.success = False
        return result
    