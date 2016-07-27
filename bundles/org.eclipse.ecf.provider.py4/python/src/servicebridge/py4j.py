"""
Pelix service bridge package

:author: Scott Lewis
:copyright: Copyright 2015, Composent, Inc.
:license: Apache License 2.0
:version: 0.1.0

..

    Copyright 2015 Composent, Inc. and others

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
"""
from logging import getLogger as getLibLogger
from threading import RLock

from py4j.java_collections import ListConverter, MapConverter, JavaArray, JavaList, JavaSet
from servicebridge import merge_dicts, ENDPOINT_ID
import servicebridge
from argparse import ArgumentError
from abc import ABCMeta, abstractmethod

from py4j.java_gateway import (
    server_connection_started, server_connection_stopped,
    server_started, server_stopped, pre_server_shutdown, post_server_shutdown,
    JavaGateway, CallbackServerParameters, DEFAULT_ADDRESS, DEFAULT_PORT, DEFAULT_PYTHON_PROXY_PORT)

    # do something
PY4J_EXPORTED_CONFIG = 'ecf.py4j.host.python'
PY4J_EXPORTED_CONFIGS = [PY4J_EXPORTED_CONFIG]
PY4J_PROTOCOL = 'py4j'
PY4J_DEFAULT_GATEWAY_PORT = DEFAULT_PORT
PY4J_DEFAULT_CB_PORT = DEFAULT_PYTHON_PROXY_PORT
PY4J_DEFAULT_HOSTNAME = DEFAULT_ADDRESS
PY4J_PYTHON_PATH = "/python"
PY4J_JAVA_PATH = "/java"

PY4J_NAMESPACE = 'ecf.namespace.py4j'
JAVA_DIRECT_ENDPOINT_CLASS = 'org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider'
PY4J_SERVICE_INTENTS = ['passByReference', 'exactlyOnce', 'ordered']

# Version
__version_info__ = (0, 1, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

_logger = getLibLogger(__name__)

# ------------------------------------------------------------------------------

def make_edef_props(*args,**kwargs):
    dicts = [x for x in args if isinstance(x,type({}))]
    itemdict = {k: v for k, v in kwargs.items() if k not in dicts}
    dicts.append(itemdict)
    tpl = tuple(dicts)
    return merge_dicts(*tpl)

def createLocalPy4jId(hostname=PY4J_DEFAULT_HOSTNAME,port=PY4J_DEFAULT_CB_PORT):
    return PY4J_PROTOCOL + "://" + hostname + ":" + str(port) + PY4J_PYTHON_PATH

def createJavaPy4jId(hostname=PY4J_DEFAULT_HOSTNAME,port=PY4J_DEFAULT_GATEWAY_PORT):
    return PY4J_PROTOCOL + "://" + hostname + ":" + str(port) + PY4J_JAVA_PATH

def prepare_java_prim(val):
    if isinstance(val,type('')) or isinstance(val,type(int(0))):
        return val
    elif isinstance(val, JavaArray) or isinstance(val, JavaSet) or isinstance(val, JavaList):
        newarray = []
        for item in val:
            if item:
                newval = prepare_java_prim(item)
                newarray.append(newval)
        return newarray
    else:
        val = str(val)

def prepare_java_props(java_props):
    result = {}
    size = len(java_props)
    i = 0
    keySet = java_props.keySet()
    for key in keySet:
        newkey = prepare_java_prim(key)
        newvalue = java_props.get(newkey)
        result[newkey] = prepare_java_prim(newvalue)
        i += 1
        if i == size:
            break
    return result

class Py4jServiceBridgeEventListener(metaclass=ABCMeta):
    '''
    Listener adapter class.   Subclasses can set themselves up
    as a listener for Py4jBridge events (service_imported, service_modified,
    service_unimported see below), buy extending this class and overriding
    these methods, and then providing the listener to the Py4jBridge instance
    creation'''
    @abstractmethod
    def service_imported(self, servicebridge, endpointid, endpoint):
        _logger.info('_service_imported endpointid='+endpointid)
    
    @abstractmethod
    def service_modified(self, servicebridge, endpointid, endpoint):
        _logger.info('_service_modified endpointid='+endpointid)
    
    @abstractmethod
    def service_unimported(self, servicebridge, endpointid, endpoint):
        _logger.info('_service_unimported endpointid='+endpointid)
 
class Py4jServiceBridgeConnectionListener(metaclass=ABCMeta):
    
    def started(self, server):
        _logger.info("Py4j started server="+repr(server))

    def connection_started(self, connection):
        _logger.info("Py4j connection started="+repr(connection))
    
    def connection_stopped(self, connection):
        _logger.info("Py4j connection stopped="+repr(connection))
    
    def stopped(self, server):
        _logger.info("Py4j gateway stopped="+repr(server))
    
    def pre_shutdown(self, server):
        _logger.info("Py4j gateway pre_shutdown="+repr(server))
    
    def post_shutdown(self, server):
        _logger.info("Py4j gateway post_shutdown="+repr(server))

   
class Py4jServiceBridge(object):
    '''Py4jServiceBridge class
    This class provides and API for consumers to use the Py4jServiceBridge.  This 
    allows a bridge between Python and the OSGi service registry.
    '''
    def __init__(self,service_listener=None,connection_listener=None):
        self._gateway = None
        self._lock = RLock()
        self._consumer = None
        self._imported_endpoints = {}
        self._imported_endpoints_lock = RLock()
        self._exported_endpoints_lock = RLock()
        self._exported_endpoints = {}
        self._map_converter = MapConverter()
        self._list_converter = ListConverter()
        self._service_listener = service_listener
        self._connection_listener = None
        self._connection = None
    
    def export(self,svc,export_props):
        with self._lock:
            self._raise_not_connected()
        try:
            endpointid = export_props[ENDPOINT_ID]
        except KeyError:
            raise ArgumentError('Cannot export service since no ENDPOINT_ID present in export_props')
        with self._exported_endpoints_lock:
            self._exported_endpoints[endpointid] = (svc,export_props)
        self.__export(svc,export_props)
        return endpointid

    def update(self,update_props):
        with self._lock:
            self._raise_not_connected()
        endpointid = None
        try:
            endpointid = update_props[ENDPOINT_ID]
        except KeyError:
            raise ArgumentError('Cannot export service since no ENDPOINT_ID present in export_props')
        endpoint = self.get_export_endpoint(endpointid)
        if endpoint:
            with self._exported_endpoints_lock:
                self._exported_endpoints[endpointid] = (endpoint[0],update_props)
            self.__update(update_props)
            return True
        else:
            return False

    def unexport(self,export_props):
        with self._lock:
            self._raise_not_connected()
        endpointid = None
        try:
            endpointid = export_props[ENDPOINT_ID]
        except KeyError:
            raise ArgumentError('Cannot export service since no ENDPOINT_ID present in export_props')
        if endpointid:
            return self.unexport_raw(endpointid)
        
    def export_raw(self,svc,object_class, rsvc_id, fw_id=None, pkg_ver=None):
        return self.export(svc, self.make_rsa_props(object_class, rsvc_id, fw_id, pkg_ver))
    
    def unexport_raw(self,endpointid):
        with self._lock:
            self._raise_not_connected()
        endpoint = self._remove_export_endpoint(endpointid)
        if endpoint:
            self.__unexport(endpoint[1])
            return endpoint
        else:
            return None

    def get_jvm(self):
        with self._lock:
            self._raise_not_connected();
        return self._gateway.jvm
     
    def get_callback_server_parameters(self):
        return self._gateway.callback_server_parameters
       
    def get_import_endpoint(self,endpointid):
        with self._imported_endpoints_lock:
            try:
                return self._imported_endpoints[endpointid]
            except KeyError:
                return None
    
    def get_export_endpoint(self,endpointid):
        with self._exported_endpoints_lock:
            try:
                return self._exported_endpoints[endpointid]
            except KeyError:
                return None
            
    def isconnected(self):
        with self._lock:
            return True if self._gateway is not None else False

    def _started(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.started(kwargs["server"])
    
    def _stopped(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.stopped(kwargs["server"])
    
    def _connection_started(self, sender, **kwargs):
        with self._lock:
            self._connection = kwargs["connection"]
            
        if self._connection_listener:
            self._connection_listener.connection_started(self._connection)
    
    def _connection_stopped(self, sender, **kwargs):
        with self._lock:
            self._connection = None
        with self._imported_endpoints_lock:
            for endpointid in self._imported_endpoints.keys():
                endpoint = None
                try:
                    endpoint = self._imported_endpoints[endpointid]
                except KeyError:
                    pass
                if self._service_listener and endpoint:
                    try:
                        self._service_listener.service_unimported(self, endpointid, endpoint)
                    except Exception as e:
                        _logger.error('_unimport_service_from_java listener threw exception endpointid='+endpointid, e)
        self.disconnect();
        if self._connection_listener:
            self._connection_listener.connection_stopped(kwargs['connection'])
    
    def _pre_shutdown(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.pre_shutdown(kwargs["server"])
    
    def _post_shutdown(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.post_shutdown(kwargs["server"])
    
    def connect(self,callback_server_parameters=None):
        if not callback_server_parameters:
            callback_server_parameters = CallbackServerParameters()
        with self._lock:
            if not self._gateway is None:
                raise ConnectionError('already connected to java gateway')
            server_started.connect(self._started)
            self._gateway = JavaGateway(callback_server_parameters=callback_server_parameters)
            cbserver = self._gateway.get_callback_server()
            server_stopped.connect(
                self._stopped, sender=cbserver)
            server_connection_started.connect(
                self._connection_started,
                sender=cbserver)
            server_connection_stopped.connect(
                self._connection_stopped,
                sender=cbserver)
            pre_server_shutdown.connect(
                self._pre_shutdown, sender=cbserver)
            post_server_shutdown.connect(
                self._post_shutdown, sender=cbserver)
            self._consumer = self._gateway.entry_point.getJavaConsumer()
            class JavaRemoteServiceExporter(object):
                def __init__(self, bridge):
                    self._bridge = bridge
                    
                def exportService(self,proxy,props):
                    self._bridge._import_service_from_java(proxy,props)
                    
                def modifyService(self,props):
                    self._bridge._modify_service_from_java(props)
                    
                def unexportService(self,props):
                    self._bridge._unimport_service_from_java(props)
                class Java:
                    implements = [JAVA_DIRECT_ENDPOINT_CLASS]

            self._bridge = JavaRemoteServiceExporter(self)
            self._gateway.entry_point.setPythonConsumer(self._bridge)
            
    def disconnect(self):
        with self._lock:
            if self.isconnected():
                self._gateway.shutdown()
                self._gateway = None
                self._consumer = None   
                self._bridge = None 
        with self._imported_endpoints_lock:
            self._imported_endpoints.clear()
        with self._exported_endpoints_lock:
            self._exported_endpoints.clear()
     
    def make_rsa_props(self,object_class, rsvc_id, fw_id, pkg_ver):
        with self._lock:
            self._raise_not_connected()
            osgiprops = servicebridge.get_rsa_props(object_class, PY4J_EXPORTED_CONFIGS, PY4J_SERVICE_INTENTS, rsvc_id, fw_id, pkg_ver)
            cbserver = self._gateway.get_callback_server()
            if cbserver:
                hostname = str(cbserver.get_listening_address())
                port = cbserver.get_listening_port()
                myid = createLocalPy4jId(hostname, port)
            else:
                myid = createLocalPy4jId()
            ecfprops = servicebridge.get_ecf_props(myid, PY4J_NAMESPACE, rsvc_id)
            return servicebridge.merge_dicts(osgiprops,ecfprops)

    # Methods called by java  
    def _import_service_from_java(self,proxy,props):
        try:
            endpointid = None
            local_props = prepare_java_props(props)
            try:
                endpointid = local_props[ENDPOINT_ID]
            except KeyError:
                pass
            if endpointid:
                endpoint = (proxy,local_props)
                with self._imported_endpoints_lock:
                    self._imported_endpoints[endpointid] = endpoint
            if self._service_listener and endpointid:
                try:
                    self._service_listener.service_imported(self, endpointid, endpoint)
                except Exception as e:
                    _logger.error('_import_service_from_java listener threw exception endpointid='+endpointid, e)
        except Exception as e:
            _logger.error(e)
            raise e

    def _modify_service_from_java(self,newprops):
        newendpoint = None
        local_props = self._prepare_props(newprops)
        try:
            endpointid = local_props[ENDPOINT_ID]
            with self._imported_endpoints_lock:
                endpoint = self._imported_endpoints[endpointid]
                props = endpoint[1]
                props.update(props)
                newendpoint = (endpoint[0],local_props)
                self._imported_endpoints[endpointid] = newendpoint
        except KeyError:
            pass
        if self._service_listener and newendpoint:
            try:
                self._service_listener.service_modified(self, endpointid, newendpoint)
            except Exception as e:
                _logger.error('_modify_service_from_java listener threw exception endpointid='+endpointid, e)
   
    def _unimport_service_from_java(self,props):
        endpoint = None
        endpointid = None
        local_props = prepare_java_props(props)
        try:
            endpointid = local_props[ENDPOINT_ID]
            with self._imported_endpoints_lock:
                endpoint = self._imported_endpoints.pop(endpointid, None)
        except KeyError:
            pass
        if self._service_listener and endpoint:
            try:
                self._service_listener.service_unimported(self, endpointid, endpoint)
            except Exception as e:
                _logger.error('_unimport_service_from_java listener threw exception endpointid='+endpointid, e)

    def _remove_export_endpoint(self,endpointid):
        with self._exported_endpoints_lock:
            return self._exported_endpoints.pop(endpointid, None)
        
    def _convert_string_list(self,l):
        llength = len(l)
        r = self._gateway.new_array(self._gateway.jvm.java.lang.String,llength)
        for i in range(0,llength):
            r[i] = l[i]
        return r
    
    def _convert_props_for_java(self,props):
        with self._lock:
            self._raise_not_connected()
        result = {}
        for item in props.items():
            val = item[1]
            if isinstance(val, type([])):
                val = self._convert_string_list(val)
            result[item[0]] = val
        return self._map_converter.convert(result,self._gateway._gateway_client)

    def _raise_not_connected(self):
        if not self.isconnected():
            raise ConnectionError('Not connected to java gateway')

    def get_access(self):
        cb = self._gateway.get_callback_server()
        return createLocalPy4jId(hostname=cb.get_listening_address(),port=cb.get_listening_port())
    
    def __export(self,svc,props):
        try:
            self._consumer.exportService(svc,self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e
    
    def __update(self,props):
        try:
            self._consumer.modifyService(self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e
           
    def __unexport(self,props):
        try:
            self._consumer.unexportService(self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e
            
