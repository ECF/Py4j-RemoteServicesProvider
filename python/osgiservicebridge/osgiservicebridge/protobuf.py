'''
OSGi service bridge protocol buffers (protobuf) support
:author: Scott Lewis
:copyright: Copyright 2016, Composent, Inc.
:license: Apache License 2.0
:version: 0.1.0
    Copyright 2016 Composent, Inc. and others
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

# Version
__version_info__ = (0, 1, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

_logger = getLibLogger(__name__)

# ------------------------------------------------------------------------------

# Useful for very coarse version differentiation.
PY2 = sys.version_info[0] == 2
PY3 = sys.version_info[0] == 3
PY34 = sys.version_info[0:2] >= (3, 4)

PB_SERVICE_EXPORTED_CONFIG_DEFAULT='ecf.py4j.host.python.pb'
PB_SERVICE_EXPORTED_CONFIGS_DEFAULT=[PB_SERVICE_EXPORTED_CONFIG_DEFAULT]

def _raw_bytes_from_java(self,methodName,serializedArgs):
    try:
        return getattr(self,methodName)(serializedArgs)
    except Exception as e:
        _logger.error('Exception calling methodName='+str(methodName)+" on object="+str(self))
        raise e

def argument_deserialize(argClass, serialized):
    #If nothing to serialze, return None
    if not serialized:
        return None
    #First create a new instance of the given argClass
    argInst = None
    try:
        # create instance of argClass
        argInst = argClass()
    except Exception as e:
        _logger.error('Could not create instance of class='+str(argClass), e)
        raise e
    # XXX because of problem discovered we check the Python version, 
    # If Python 2 we convert the serialized to a string (from bytearray)
    if PY2:
        serialized = str(serialized)
    #Then pass to parser and return
    try:
        # deserialze
        argInst.ParseFromString(serialized)
    except Exception as e:
        _logger.error('Could not call ParseFromString on instance='+str(argInst),e)
        raise e
    return argInst

def return_serialize(respb):
    resBytes = None
    if respb:
        try:
            resBytes = respb.SerializeToString()
        except Exception as e:
            _logger.error('Could not call SerializeToString on resp object='+str(respb),e)
            raise e
        # If Python 2 we convert the string to bytearray
        if PY2:
            resBytes = bytearray(resBytes)
    return resBytes
    
def ProtoBufRemoteService(**kwargs):
    def decorate(cls):
        pb_svc_props = { osgiservicebridge.SERVICE_EXPORTED_CONFIGS: PB_SERVICE_EXPORTED_CONFIGS_DEFAULT }
        args_props = kwargs.pop(osgiservicebridge.EXPORT_PROPERTIES_NAME,None)
        if args_props:
            pb_svc_props = osgiservicebridge.merge_dicts(pb_svc_props,args_props)
        kwargs[osgiservicebridge.EXPORT_PROPERTIES_NAME] = pb_svc_props
        cls = osgiservicebridge._modify_remoteservice_class(cls,kwargs)
        cls._raw_bytes_from_java = _raw_bytes_from_java
        return cls
    return decorate

def ProtoBufRemoteServiceMethod(arg_type):
    def pbwrapper(func):
        @wraps(func)
        def wrapper(*args):
            argClass = arg_type
            if len(args) > 1 and argClass:
                argInst = argument_deserialize(argClass, args[1])
            respb = None
            try:
                respb = func(args[0],argInst)
            except Exception as e:
                _logger.error('Could not call function='+str(func)+' on object='+str(args[0]))
                raise e
            resBytes = return_serialize(respb)
            return resBytes
        return wrapper
    return pbwrapper

