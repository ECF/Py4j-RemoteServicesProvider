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
import logging
from logging import getLogger as getLibLogger
import sys
import os
import importlib
import inspect
import osgiservicebridge
from osgiservicebridge import ENDPOINT_PACKAGE_VERSION_

import time

from google.protobuf import message_factory as _message_factory
from google.protobuf.message import Message
from google.protobuf.descriptor import EnumDescriptor, EnumValueDescriptor, Descriptor, FieldDescriptor, _OptionsOrNone
from osgiservicebridge.bridge import JavaRemoteServiceRegistry, Py4jServiceBridgeEventListener, \
    JavaServiceMethod, JavaServiceProxy, JAVA_OBJECT_METHODS, \
    ServiceMethod, get_interfaces, \
    get_return_methtype, Py4jService, PythonServiceMethod

from osgiservicebridge import ECF_SERVICE_EXPORTED_ASYNC_INTERFACES

from osgiservicebridge.exporter_pb2 import ExportRequest, ExportResponse, UnexportRequest, UnexportResponse
from concurrent.futures._base import Future
from concurrent.futures.thread import ThreadPoolExecutor
from google.protobuf import symbol_database, descriptor_pool
from threading import RLock
from google.protobuf.internal import api_implementation

_USE_C_DESCRIPTORS = False
if api_implementation.Type() == 'cpp': 
    # Used by MakeDescriptor in cpp mode
    import binascii
    from google.protobuf import message as _message
    _USE_C_DESCRIPTORS = getattr(_message, '_USE_C_DESCRIPTORS', False)

# Documentation strings format
__docformat__ = "restructuredtext en"

from osgiservicebridge.version import __version__ as __v__
# Version
__version__ = __v__

# ------------------------------------------------------------------------------
_logger = getLibLogger(__name__)
_timing = getLibLogger("timing." + __name__)
# ------------------------------------------------------------------------------

# PY2/PY3 version differentiation.
PY2 = sys.version_info[0] == 2
PY3 = sys.version_info[0] == 3
PY34 = sys.version_info[0:2] >= (3, 4)

JAVA_HOST_CONFIG_TYPE = 'ecf.py4j.host.pb';
PYTHON_HOST_CONFIG_TYPE = 'ecf.py4j.host.python.pb';
JAVA_CONSUMER_CONFIG_TYPE = 'ecf.py4j.consumer.pb';
PYTHON_CONSUMER_CONFIG_TYPE = 'ecf.py4j.consumer.python.pb';

PB_SERVICE_EXPORTED_CONFIG_DEFAULT = PYTHON_HOST_CONFIG_TYPE
PB_SERVICE_EXPORTED_CONFIGS_DEFAULT = [PB_SERVICE_EXPORTED_CONFIG_DEFAULT]

PB_SERVICE_RETURN_TYPE_ATTR = '_return_type'
PB_SERVICE_ARG_TYPE_ATTR = '_arg_type'
PB_SERVICE_SOURCE_ATTR = '_source'

_default_symbol_db = symbol_database.Default()
_default_descriptor_pool = descriptor_pool.Default()


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
    if ret_type:
        return ret_type()
    return None


def get_method_source(method):
    return getattr(method, PB_SERVICE_SOURCE_ATTR, None)


def set_method_source(method, sourcecode):
    setattr(method, PB_SERVICE_ARG_TYPE_ATTR, sourcecode)

    
def update_method(method, oldfunc, new_source):
    if method:
        setattr(method, PB_SERVICE_ARG_TYPE_ATTR, getattr(oldfunc, PB_SERVICE_ARG_TYPE_ATTR, None))
        setattr(method, PB_SERVICE_RETURN_TYPE_ATTR, getattr(oldfunc, PB_SERVICE_RETURN_TYPE_ATTR, None))
        setattr(method, PB_SERVICE_SOURCE_ATTR, new_source)


def get_name_and_type_dict(m):
    result = dict()
    for key in m:
        val = m[key]
        if val == None:
            result[key] = 'None'
        else:
            result[key] = fully_qualified_classname(type(val))
    return result


def fully_qualified_classname(c):
    module = c.__module__
    if module == None or module == str.__class__.__module__:
        return c.__name__
    return module + '.' + c.__name__

                
def _raw_bytes_from_java(self, methodName, serializedArgs):
    return getattr(self, methodName)(serializedArgs)


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
    # Then pass to parser and return
    t0 = time.time()
    try:
        # deserialze
        pmessage_instance.ParseFromString(message_bytes)
    except Exception as e:
        _logger.exception('bytes_to_pmessage could not parse message from bytes. message_instance=%' % (pmessage_instance))
        raise e
    t1 = time.time()
    _timing.debug("protobuf.bytes_to_pmessage;diff={0}".format(1000 * (t1 - t0)))
    return pmessage_instance


def bytes_to_jmessage(jmessage_class, message_bytes):
    if not message_bytes:
        return None
    parser_instance = None
    try:
        parser_method = jmessage_class.getMethod("parser", None)
        parser_instance = parser_method.invoke(None, None)
    except Exception as e:
        _logger.exception('bytes_to_jmessage could not create instance of message_class=%s' % (jmessage_class))
        raise e
    if PY2:
        message_bytes = bytearray(message_bytes)
    # Then pass to parser and return
    t0 = time.time()
    jmessage_instance = None
    try:
        # deserialze
        jmessage_instance = parser_instance.parseFrom(message_bytes)
    except Exception as e:
        _logger.exception('bytes_to_jmessage could not parse message from bytes.  message_instance=%' % (jmessage_instance))
        raise e
    t1 = time.time()
    _timing.debug("protobuf.bytes_to_jmessage;diff={0}".format(1000 * (t1 - t0)))
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
    _timing.debug("protobuf.pmessage_to_bytes;diff={0}".format(1000 * (t1 - t0)))
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
    _timing.debug("protobuf.jmessage_to_bytes;diff={0}".format(1000 * (t1 - t0)))
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
        args_props = kwargs.pop(osgiservicebridge.EXPORT_PROPERTIES_NAME, None)
        # if it is present, then merge/override with the values given
        if args_props:
            pb_svc_props = osgiservicebridge.merge_dicts(pb_svc_props, args_props)
        # then set/reset the kwargs service_properties value
        kwargs[osgiservicebridge.EXPORT_PROPERTIES_NAME] = pb_svc_props
        # call _modify_remoteservice_class to modify the class itself (add Java inner 
        # class and set implements and service_properties values
        cls = osgiservicebridge._modify_remoteservice_class(cls, kwargs)
        # set pbbuffers callback to _raw_bytes_from_java
        cls._raw_bytes_from_java = _raw_bytes_from_java
        return cls

    return decorate


def protobuf_remote_service_method(arg_type, return_type=None):
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
                respb = func(args[0], argInst)
            except Exception as e:
                logged_wrapped_exception('Remote method invoke failed')
                raise e
            t1 = time.time()
            _timing.debug("protobuf.exec;time={0}".format(1000 * (t1 - t0)))
            if func._return_type:
                isinstance(respb, func._return_type)
            return pmessage_to_bytes(respb)

        return wrapper

    return pbwrapper


class ProtobufServiceRegistry(Py4jServiceBridgeEventListener, JavaRemoteServiceRegistry):
    
    def __init__(self):
        super(ProtobufServiceRegistry, self).__init__()
        self._executor = ThreadPoolExecutor()
        self._timeout = 30
        
    def service_imported(self, servicebridge, endpointid, proxy, endpoint_props):
        imported_configs = endpoint_props[osgiservicebridge.SERVICE_IMPORTED_CONFIGS]
        theproxy = None
        if (osgiservicebridge.protobuf.JAVA_HOST_CONFIG_TYPE in imported_configs):
            theproxy = ProtobufJavaServiceProxy(servicebridge._gateway.jvm, endpoint_props[osgiservicebridge.OBJECT_CLASS], proxy, self._executor, self._timeout)
        else:
            theproxy = proxy
        self._add_remoteservice(endpoint_props, theproxy)
        
    def service_modified(self, servicebridge, endpointid, proxy, endpoint_props):
        self._modify_remoteservice(endpointid, endpoint_props)
    
    def service_unimported(self, servicebridge, endpointid, proxy, endpoint_props):
        self._remove_remoteservice(endpointid)


PYTHON_SERVICE_EXPORTER_PACKAGE = 'org.eclipse.ecf.python.protobuf'
PYTHON_SERVICE_EXPORTER_PACKAGE_VERSION = '1.0.0'
PYTHON_SERVICE_EXPORTER = PYTHON_SERVICE_EXPORTER_PACKAGE + '.PythonServiceExporter'


@protobuf_remote_service(
    objectClass=[PYTHON_SERVICE_EXPORTER], export_properties={ ECF_SERVICE_EXPORTED_ASYNC_INTERFACES: '*',
                                                                 ENDPOINT_PACKAGE_VERSION_ + PYTHON_SERVICE_EXPORTER_PACKAGE: PYTHON_SERVICE_EXPORTER_PACKAGE_VERSION })
class PythonServiceExporter(object):
    
    def __init__(self, bridge):
        self._bridge = bridge
        
    def _create_error_export_response(self, message):
        result = ExportResponse()
        result.error_message = message
        return result
    
    def _create_success_export_response(self, endpoint_id):
        result = ExportResponse()
        result.endpoint_id = endpoint_id
        return result
    
    def _load_module(self, module_name):
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
            _logger.exception('Could not load module={0}'.format(module_name, ex))
            return None
        return module_
    
    def _get_class_from_module(self, module, class_name):

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
    
    @protobuf_remote_service_method(arg_type=ExportRequest, return_type=ExportResponse)
    def createAndExport(self, export_request): 
        try:
            module = self._load_module(export_request.module_name)
            if not module:
                return self._create_error_export_response('Cannot get module for module_name={0}'.format(export_request.module_name))
            clazz = self._get_class_from_module(module, export_request.class_name)
            if not clazz:
                return self._create_error_export_response('Cannot get class for class_name={0}'.format(export_request.class_name))
            args = export_request.creation_args
            if not args or len(args) <= 0:
                inst = clazz()
            else:
                inst = clazz(args)
            export_id = self._bridge.export(inst, export_request.overriding_export_props)
            return self._create_success_export_response(export_id)
        except Exception as ex:
            _logger.exception('Could not create and export with request={0}'.format(export_request))
            return self._create_error_export_response(str(ex))
        
    @protobuf_remote_service_method(arg_type=UnexportRequest, return_type=UnexportResponse)
    def unexport(self, unexport_request):
        endpoint_id = self._bridge.unexport(unexport_request.endpoint_id)
        result = UnexportResponse()
        result.endpoint_id = endpoint_id
        if endpoint_id:
            result.success = True
        else:
            result.success = False
        return result


def MakeClassDescriptor(descriptor_pool, desc_proto, package='', build_file_if_cpp=True, syntax=None):
    if api_implementation.Type() == 'cpp' and build_file_if_cpp:
        # The C++ implementation requires all descriptors to be backed by the same
        # definition in the C++ descriptor pool. To do this, we build a
        # FileDescriptorProto with the same definition as this descriptor and build
        # it into the pool.
        from google.protobuf import descriptor_pb2
        file_descriptor_proto = descriptor_pb2._FIELDDESCRIPTORPROTO
        file_descriptor_proto.message_type.add().MergeFrom(desc_proto)

        # Generate a random name for this proto file to prevent conflicts with any
        # imported ones. We need to specify a file name so the descriptor pool
        # accepts our FileDescriptorProto, but it is not important what that file
        # name is actually set to.
        proto_name = binascii.hexlify(os.urandom(16)).decode('ascii')

        if package:
            file_descriptor_proto.name = os.path.join(package.replace('.', '/'),
                                                proto_name + '.proto')
            file_descriptor_proto.package = package
        else:
            file_descriptor_proto.name = proto_name + '.proto'

        descriptor_pool.Add(file_descriptor_proto)
        result = descriptor_pool.FindFileByName(file_descriptor_proto.name)

        if _USE_C_DESCRIPTORS:
            return result.message_types_by_name[desc_proto.name]

    full_message_name = [desc_proto.name]
    if package: full_message_name.insert(0, package)

    # Create Descriptors for enum types
    enum_types = {}
    for enum_proto in desc_proto.enum_type:
        full_name = '.'.join(full_message_name + [enum_proto.name])
        enum_desc = EnumDescriptor(
            enum_proto.name, full_name, None, [
                EnumValueDescriptor(enum_val.name, ii, enum_val.number)
                for ii, enum_val in enumerate(enum_proto.value)])
        enum_types[full_name] = enum_desc

    # Create Descriptors for nested types
    nested_types = {}
    for nested_proto in desc_proto.nested_type:
        full_name = '.'.join(full_message_name + [nested_proto.name])
        # Nested types are just those defined inside of the message, not all types
        # used by fields in the message, so no loops are possible here.
        nested_desc = MakeClassDescriptor(descriptor_pool, nested_proto,
                                 package='.'.join(full_message_name),
                                 build_file_if_cpp=False,
                                 syntax=syntax)
        nested_types[full_name] = nested_desc

    fields = []
    for field_proto in desc_proto.field:
        full_name = '.'.join(full_message_name + [field_proto.name])
        enum_desc = None
        nested_desc = None
        if field_proto.json_name:
            json_name = field_proto.json_name
        else:
            json_name = None
        if field_proto.HasField('type_name'):
            type_name = field_proto.type_name
            full_type_name = '.'.join(full_message_name + 
                                [type_name[type_name.rfind('.') + 1:]])
            if full_type_name in nested_types:
                nested_desc = nested_types[full_type_name]
            elif full_type_name in enum_types:
                enum_desc = enum_types[full_type_name]
            # Else type_name references a non-local type, which isn't implemented
        field = FieldDescriptor(
            field_proto.name, full_name, field_proto.number - 1,
            field_proto.number, field_proto.type,
            FieldDescriptor.ProtoTypeToCppProtoType(field_proto.type),
            field_proto.label, None, nested_desc, enum_desc, None, False, None,
            options=_OptionsOrNone(field_proto), has_default_value=False,
            json_name=json_name)
        fields.append(field)

    desc_name = '.'.join(full_message_name)
    return Descriptor(desc_proto.name, desc_name, None, None, fields,
                    list(nested_types.values()), list(enum_types.values()), [],
                    options=_OptionsOrNone(desc_proto))
    
    
_lock = RLock()
_jclass_pclass_map = {}


# ---------------------------------------------------------------------------------------------------------
#  API for async method invocation
# ---------------------------------------------------------------------------------------------------------
def get_pb_type_from_java_class(java_class):
    with _lock:
        pbclass_entry = _jclass_pclass_map.get(java_class, None)
        if not pbclass_entry:
            # get full name for java class descriptor
            pdesc_desc_fn = java_class.getMethod("getDescriptor", None).invoke(None, None).getFullName()
            if pdesc_desc_fn:
                # find message type descriptor from name
                msg_descriptor = _default_descriptor_pool.FindMessageTypeByName(pdesc_desc_fn)
                if msg_descriptor:
                    return msg_descriptor._concrete_class


def get_jparser_from_pdescriptor(jvm, pdescriptor):
    try:
        dfilename = os.path.splitext(os.path.basename(pdescriptor.file.name))[0]
        fqcname = "{0}.{1}${2}".format(pdescriptor.file.package, dfilename[0].upper() + dfilename[1:], pdescriptor.name)
        return getattr(jvm, fqcname)
    except Exception as e:
        logging.exception('get_jparser_from_descriptor')
        raise e


def get_pb_service_methods(jvm, jmethods, service, executor, timeout, timeunit):
    '''
    get list of service methods by invoking cls constructor with service, jmethod.getName(), and MethodType for method return type
    '''
    return [ProtobufJavaServiceMethod(service,
                jmethod.getName(),
                get_return_methtype(jvm,
                                    jmethod.getReturnType().getName(),
                                    executor,
                                    timeout,
                                    timeunit),
                                    jmethod.getParameterTypes()) for jmethod in jmethods if not jmethod.getName() in JAVA_OBJECT_METHODS ]


def get_pb_python_service_methods(jvm, jmethods, service, executor, timeout, timeunit):
    '''
    get list of service methods by invoking cls constructor with service, jmethod.getName(), and MethodType for method return type
    '''
    return [ProtobufPythonServiceMethod(service,
                jmethod.getName(),
                get_return_methtype(jvm,
                                    jmethod.getReturnType().getName(),
                                    executor,
                                    timeout,
                                    timeunit),
                                    jmethod.getParameterTypes(),
                                    jvm) for jmethod in jmethods if not jmethod.getName() in JAVA_OBJECT_METHODS ]

# -----------------------------------------------------------------------------------------------


class ProtobufPythonService(Py4jService):

    def __init__(self, bridge, interfaces, svc_object, executor, timeout):
        Py4jService.__init__(self, svc_object)
        osgiservicebridge._modify_remoteservice_class(ProtobufPythonService, { 'objectClass': interfaces })
        self._interfaces = get_interfaces(bridge.get_jvm(), interfaces, get_pb_python_service_methods, svc_object, executor, timeout)


class ProtobufPythonServiceMethod(PythonServiceMethod):
    
    def __init__(self, service, method_name, method_type, java_param_types, jvm):
        PythonServiceMethod.__init__(self, service, method_name, method_type)
        self._jvm = jvm
        self._jreturn_parser = None
        if java_param_types:
            self._is_pbmessage = jvm.java.lang.Class.forName('com.google.protobuf.Message').isAssignableFrom(java_param_types[0])
            if self._is_pbmessage:
                # if it is a pbmessage type
                # then get the python message type given the first java param type
                self._pmessage_arg_type = get_pb_type_from_java_class(java_param_types[0])
        
    def _process_result(self, presult):
        presult = presult.result(self._method_type._timeout) if isinstance(presult, Future) else presult
        if presult:
            if not self._jreturn_parser:
                # get and cache self._jreturn_parser
                self._jreturn_parser = get_jparser_from_pdescriptor(self._jvm, presult.DESCRIPTOR)
            # time the conversion to jmessage including serialization
            t0 = time.time()
            # we convert
            presult = self._jreturn_parser.parseFrom(pmessage_to_bytes(presult))
            _timing.debug("protobuf.returnserialize;time={0}".format(1000 * (time.time() - t0)))
        return PythonServiceMethod._process_result(self, presult)
    
    def _call_sync(self, *args):
        call_args = args
        if self._is_pbmessage:
            first = args[0] if args else None
            if first:
                # for pb services the first argument should be a java Message subclass
                t0 = time.time()
                pmessage = jmessage_to_pmessage(self._pmessage_arg_type, first)
                _timing.debug("protobuf.argdeserialize;time={0}".format(1000 * (time.time() - t0)))
                call_args = [pmessage] + list(args)[1:]
        return PythonServiceMethod._call_sync(self, *call_args)


# -----------------------------------------------------------------------------------------------
class ProtobufJavaServiceProxy(JavaServiceProxy):
    
    def __init__(self, jvm, interfaces, proxy, executor, timeout=30):
        self._interfaces = get_interfaces(jvm, interfaces, get_pb_service_methods, proxy, executor, timeout)
        self._proxy = proxy

        
class ProtobufJavaServiceMethod(JavaServiceMethod):
    
    def __init__(self, proxy, method_name, method_type, java_param_types):
        ServiceMethod.__init__(self, proxy, method_name, method_type)
        self._java_param_types = java_param_types
        self._python_return_type = None
    
    # called by superclass to actually make the synchronous remote call (may be
    # on separate thread)
    def _call_sync(self, *args):
        # check arguments
        call_args = [None]
        if args and len(args) > 0:
            # normal case is that there is one argument that
            # should be a protobuf message
            t0 = time.time()
            # serialize python message to bytes
            call_args = [pmessage_to_jmessage(self._java_param_types[0], args[0])]
            _timing.debug("protobuf.argserialize;time={0}".format(1000 * (time.time() - t0)))
        # now call superclass with first call_args [jmessage]
        return JavaServiceMethod._call_sync(self, *call_args)
    
    def _process_result(self, jresult):
        if not self._python_return_type:
            self._python_return_type = get_pb_type_from_java_class(jresult.getClass())
        # convert bytes to python return type
        return jmessage_to_pmessage(self._python_return_type, jresult)
    
