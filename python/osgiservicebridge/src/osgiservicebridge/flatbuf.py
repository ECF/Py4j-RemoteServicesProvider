'''
OSGi service bridge Google protocol buffers (protobuf) support
:author: Scott Lewis
:copyright: Copyright 2016, Composent, Inc.
:license: Apache License 2.0
:version: 1.0.4
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
import time

from flatbuffers import Builder

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

FB_SERVICE_EXPORTED_CONFIG_DEFAULT='ecf.py4j.host.python.fb'
FB_SERVICE_EXPORTED_CONFIGS_DEFAULT=[FB_SERVICE_EXPORTED_CONFIG_DEFAULT]

FB_SERVICE_RETURN_TYPE_ATTR = '_return_type'
FB_SERVICE_ARG_TYPE_ATTR = '_arg_type'
FB_SERVICE_SOURCE_ATTR = '_source'

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
    return get_instance_type(instance, method_name, FB_SERVICE_RETURN_TYPE_ATTR)

def get_instance_arg_type(instance, method_name):
    '''
    Return the class for the arg_type  for method_name on instance.
    :param instance:  the instance to query.  Must not be None.
    :param method_name: the method_name to access.  Must not be None.
    :return: the class of the argument type.  Will be None if no function with method_name
    or no return type attribute on function.
    '''
    return get_instance_type(instance, method_name, FB_SERVICE_ARG_TYPE_ATTR)

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
    return getattr(method,FB_SERVICE_SOURCE_ATTR, None)

def set_method_source(method, sourcecode):
    setattr(method, FB_SERVICE_ARG_TYPE_ATTR, sourcecode)
    
def update_method(method,oldfunc,new_source):
    if method:
        setattr(method, FB_SERVICE_ARG_TYPE_ATTR, getattr(oldfunc, FB_SERVICE_ARG_TYPE_ATTR, None))
        setattr(method, FB_SERVICE_RETURN_TYPE_ATTR, getattr(oldfunc, FB_SERVICE_RETURN_TYPE_ATTR, None))
        setattr(method, FB_SERVICE_SOURCE_ATTR, new_source)

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

def argument_deserialize(argClass, serialized):
    #If nothing to serialize, return None
    if not serialized:
        return None
    # XXX because of problem discovered we check the Python version, 
    # If Python 2 we convert the serialized to a string (from bytearray)
    if PY2:
        serialized = str(serialized)
    argInst = None
    #Then pass to parser and return
    t0 = time.time()
    try:
        # Create fb staticmethod name
        methodName = 'GetRootAs' + argClass.__name__
        argInst = getattr(argClass,methodName)(bytearray(serialized),0)
    except Exception as e:
        _logger.exception('Class.'+methodName+' failed. argClass=%' % (argClass))
        raise e
    t1 = time.time()
    _timing.debug("flatbuf.request.deserialize;d="+str(1000*(t1-t0))+"ms")
    return argInst

def return_serialize(builder):
    resBytes = None
    t0 = time.time()
    if builder:
        try:
            resBytes = builder.Output()
        except Exception as e:
            _logger.exception('builder.Output failed. builder=%s' % (builder))
            raise e
        # If Python 2 we convert the string to bytearray
        if PY2:
            resBytes = bytearray(resBytes)
    t1 = time.time()
    _timing.debug("flatbuf.response.serialize;d="+str(1000*(t1-t0))+"ms")
    return resBytes
    
def flatbuf_remote_service(**kwargs):    
    '''
    Class decorator for flatbuf-based remote services.  This class decorator is intended to be used as follows:
    
    @flatbuf_remote_service(objectClass=['fully.qualified.java.interface.package.Classname',...],export_properties={ 'myprop': 'myvalue' })
    class MyClass:
        pass
        
    Example:
    
    @flatbuf_remote_service(objectClass=['org.eclipse.ecf.examples.protobuf.hello.IHello'])
    class HelloServiceImpl:

    :param kwargs: the kwargs dict required to have 'objectClass' item and
    and (optional) 'export_properties' item
    '''
    def decorate(cls):
        # setup the protocol buffers java-side config
        pb_svc_props = { osgiservicebridge.SERVICE_EXPORTED_CONFIGS: FB_SERVICE_EXPORTED_CONFIGS_DEFAULT }
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

def flatbuf_remote_service_method(arg_type,return_type=None):
    '''
    Method decorator for flatbuf-based remote services method.  This class decorator is intended to be used as follows:
    
    @flatbuf_remote_service_method(arg_type=<arg class>,return_type=<return_class>)
    def <methodName>(self,<argName>):
        
    For example:
    
    @flatbuf_remote_service_method(arg_type=HelloMsgContent,return_type=HelloMsgContent)
    def sayHello(self,pbarg):
        
    Where HelloMsgContent is a flatc-generated python class.  When called, pbarg is guaranteed to be an 
    instance of a flatc-generated class.
    The sayHello method is also required to return an instance of HelloMsgContent or None.
    
    :param arg_type: the class of the pbarg type (will be instance of HelloMsgContent)
    '''
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
            setattr(func, FB_SERVICE_SOURCE_ATTR, getsource(func))
        except:
            pass
        @wraps(func)
        def wrapper(*args):
            # set argClass to arg_type
            argClass = arg_type
            # signature should be:  self,arg.  Pass args[1]...the actual argument
            # ...to deserialize byte[] into pb Message into argInst
            argInst = argument_deserialize(argClass, args[1])
            builder = None
            t0 = time.time()
            try:
                # Now actually call function, with self,pbMessage
                builder = func(args[0],argInst)
            except Exception as e:
                logged_wrapped_exception('Remote method invoke failed')
                raise e
            t1 = time.time()
            _timing.debug("flatbuf.exec;time="+str(1000*(t1-t0))+"ms")
            if not func._return_type is None:
                isinstance(builder,Builder)
            return return_serialize(builder)
        return wrapper
    return pbwrapper

