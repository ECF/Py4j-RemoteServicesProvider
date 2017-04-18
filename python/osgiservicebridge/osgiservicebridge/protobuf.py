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

# Version
__version_info__ = (0, 1, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

_logger = getLibLogger(__name__)

# ------------------------------------------------------------------------------

def protobufmethod(arg_type):
    def pbwrapper(func):
        @wraps(func)
        def wrapper(*args,**kwargs):
            argClass = arg_type
            if len(args) > 1 and argClass:
                argInst = argClass()
                argInst.ParseFromString(args[1])
            respb = func(args[0],argInst)
            resBytes = None
            if respb:
                resBytes = respb.SerializeToString()
            return resBytes
        return wrapper
    return pbwrapper

PB_SERVICE_EXPORTED_CONFIG_DEFAULT='ecf.py4j.host.python.pb'
PB_SERVICE_EXPORTED_CONFIGS_DEFAULT=[PB_SERVICE_EXPORTED_CONFIG_DEFAULT]

class ProtobufServiceImpl(object):
    
    def _raw_bytes_from_java(self,methodName,serializedArgs):
        try:
            return getattr(self,methodName)(serializedArgs)
        except Exception as e:
            _logger.error('Exception calling methodName='+str(methodName)+" on object="+str(self))
            raise e
        
    
        