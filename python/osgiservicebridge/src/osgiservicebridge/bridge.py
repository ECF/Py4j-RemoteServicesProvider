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
from osgiservicebridge.version import __version__ as __v__
# Version
__version__ = __v__

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------
from logging import getLogger as getLibLogger

_logger = getLibLogger(__name__)
_timing = getLibLogger("timing."+__name__)
# ------------------------------------------------------------------------------

from threading import RLock, Condition
import sys
import importlib
import time


_done = False
__condition = Condition()

# Utility functions for threading

def _done_waiting():
    global _done
    global __condition
    __condition.acquire()
    _done = True
    __condition.notify_all()
    __condition.release()
      
def _wait_for_sec(sec=1):
    global _done
    global __condition
    __condition.acquire()
    __condition.wait(sec)
    __condition.release()
     
def _wait_until_done(sec=None):
    global _done
    global __condition
    __condition.acquire()
    while not _done:
        __condition.wait(sec)
    __condition.release()
    
class flushfile(object):
    def __init__(self, f):
        self.file = f
        
    def write(self, x):
        self.file.write(x)
        self.file.flush()
        
# end threading utilities      
  
from py4j.java_collections import ListConverter, MapConverter, JavaArray, JavaList, JavaSet
from osgiservicebridge import merge_dicts, ENDPOINT_ID, get_edef_props, PY4J_PROTOCOL,\
     PY4J_PYTHON_PATH, PY4J_JAVA_ATTRIBUTE, PY4J_JAVA_IMPLEMENTS_ATTRIBUTE, EXPORT_PROPERTIES_NAME,\
    PY4J_EXPORTED_CONFIG
 
import osgiservicebridge
from argparse import ArgumentError

from py4j.java_gateway import (
    server_connection_started, server_connection_stopped,
    server_started, server_stopped, pre_server_shutdown, post_server_shutdown,
    JavaGateway, CallbackServerParameters, DEFAULT_ADDRESS, DEFAULT_PORT, DEFAULT_PYTHON_PROXY_PORT,
    GatewayParameters)

'''
Py4J constants
'''
PY4J_DEFAULT_GATEWAY_PORT = DEFAULT_PORT
PY4J_DEFAULT_CB_PORT = DEFAULT_PYTHON_PROXY_PORT
PY4J_DEFAULT_HOSTNAME = DEFAULT_ADDRESS

JAVA_PATH_PROVIDER = 'org.eclipse.ecf.provider.direct.ExternalPathProvider'
JAVA_DIRECT_ENDPOINT_CLASS = 'org.eclipse.ecf.provider.direct.InternalDirectDiscovery'
PY4J_CALL_BY_VALUE_CLASS = 'org.eclipse.ecf.provider.direct.ExternalCallableEndpoint'

def make_edef_props(*args,**kwargs):
    '''
    Make EDEF-compliant dictionary of properties.
    
    :param args: List of dictionaries with properties to be added to dictionary result
    :param kwargs: Keyword based arguments to be added to dictionary result
    :return: Dictionary containing items from all given dictionaries and kwargs
    '''
    dicts = [x for x in args if isinstance(x,type({}))]
    itemdict = {k: v for k, v in kwargs.items() if k not in dicts}
    dicts.append(itemdict)
    tpl = tuple(dicts)
    return merge_dicts(*tpl)

def createLocalPy4jId(hostname=PY4J_DEFAULT_HOSTNAME,port=PY4J_DEFAULT_CB_PORT):
    '''
    Create a Local Py4j Id.   A local Py4j id has syntax:  py4j://<hostname>:<port>/python
    :param hostname: The hostname to use.  Defaults to PY4J_DEFAULT_HOSTNAME.
    :param port:  The port to use.  Defaults to PY4J_DEFAULT_CB_PORT
    :return: The Py4j Id as a string.
    '''
    return PY4J_PROTOCOL + "://" + hostname + ":" + str(port) + PY4J_PYTHON_PATH

def prepare_java_prim(val):
    '''
    Prepare java primitive for use within Python.  This method converts a given Java primitive (JavaObject, 
    JavaSet, JavaList) to the Python representation.   If a collection (set, array, list) the items within 
    are recursively converted as well.
    :param val: the java object to convert
    :return: the converted object(s)
    '''
    if isinstance(val,str):
        return str(val)
    elif isinstance(val,type('')) or isinstance(val,type(int(0))):
        return val
    elif isinstance(val, JavaArray) or isinstance(val, JavaSet) or isinstance(val, JavaList):
        newarray = []
        for item in val:
            if item:
                newval = prepare_java_prim(item)
                newarray.append(newval)
        return newarray
    else:
        return str(val)

def prepare_java_props(java_props):
    '''
    Prepare java properties (as Map) for use as Python dictionary of properties.  The result will
    be a dictionary of Python items, all of which have been converted via prepare_java_prim above.
    :param java_props:  Java Map of properties to be converted
    :return: dictionary of properties with same (string) keys and values converted via prepare_java_prim
    '''
    result = {}
    java_set = java_props.keySet()
    java_array = java_set.toArray()
    for i in range(int(java_set.size())):
        key = str(java_array[i])
        result[key] = prepare_java_prim(java_props.get(key))
    return result

class Py4jServiceBridgeEventListener(object):
    '''
    Listener adapter class for service bridge events.   Subclasses may set themselves up
    as a listener for Py4jServiceBridge events (service_imported, service_modified,
    service_unimported see below) by extending this class and overriding
    these methods, and then providing their listener instance to the Py4jServiceBridge instance
    creation'''
    
    def __init__(self):
        super(Py4jServiceBridgeEventListener,self).__init__()

    def service_imported(self, servicebridge, endpointid, proxy, endpoint_props):
        '''
        Service imported notification.
        :param servicebridge: The Py4jServiceBridge instance that received the 
        notification. Will not be None.
        :param endpointid:  The endpointid from given set of properties.  
        Will not be None.
        :param proxy:  The endpoint instance proxy.  Will not be None.
        :param endpoint_props:  The endpoint properties.  Will not be None.
        '''
        _logger.info('_service_imported endpointid='+endpointid+";proxy="+str(proxy)+";endpoint_props="+str(endpoint_props))
    
    def service_modified(self, servicebridge, endpointid, proxy, endpoint_props):
        '''
        Service modified notification.
        :param servicebridge: The Py4jServiceBridge instance that received the notification
        :param endpointid:  The endpointid from given set of properties.  
        Will not be None.
        :param proxy:  The endpoint instance proxy.  Will not be None.
        :param endpoint_props:  The endpoint properties.  Will not be None.
        '''
        _logger.info('_service_modified endpointid='+endpointid+";proxy="+str(proxy)+";endpoint_props="+str(endpoint_props))
    
    def service_unimported(self, servicebridge, endpointid, proxy, endpoint_props):
        '''
        Service modified notification.
        :param servicebridge: The Py4jServiceBridge instance that received the notification
        :param endpointid:  The endpointid from given set of properties.  
        Will not be None.
        :param proxy:  The endpoint instance proxy.  Will not be None.
        :param endpoint_props:  The endpoint properties.  Will not be None.
        '''
        _logger.info('_service_unimported endpointid='+endpointid+";proxy="+str(proxy)+";endpoint_props="+str(endpoint_props))
 
class Py4jServiceBridgeConnectionListener(object):
    '''
    Service Bridge Connection Listener.  Subclass instances will be notified via signals when
    the callback server is started, the connection_started, the connection_stopped, the cb server
    stopped, pre_shutdown and post_shutdown.
    '''    
    def started(self, server):
        '''
        Notification that the callback server has been started
        :param server: The callback server that has been started
        '''
        _logger.info("Py4j started server="+repr(server))

    def connection_started(self, connection):
        '''
        Notification that the a connection has been received and started
        :param connection: The connection that has been started
        '''
        _logger.info("Py4j connection started="+repr(connection))
    
    def connection_stopped(self, connection, exception):
        '''
        Notification that the a connection has been stopped
        :param connection: The connection that has been stopped
        '''
        _logger.info("Py4j connection stopped="+repr(connection)+',exception='+repr(exception))
    
    def stopped(self, server):
        '''
        Notification that the callback server has been stopped
        :param server: The callback server that has been stopped
        '''
        _logger.info("Py4j gateway stopped="+repr(server))
    
    def pre_shutdown(self, server):
        '''
        Notification that the callback server is going to be shutdown
        :param server: The callback server that is being shutdown
        '''
        _logger.info("Py4j gateway pre_shutdown="+repr(server))
    
    def post_shutdown(self, server):
        '''
        Notification that the callback server has been shutdown
        :param server: The callback server that was shutdown
        '''
        _logger.info("Py4j gateway post_shutdown="+repr(server))

class JavaPathHook(object):
    def __init__(self, bridge):
        self._bridge = bridge
        
    def initialize_path(self,path_list):
        '''
        Initialize this path provider with a path_list (list of Strings)
        that is to be added to the sys.path
        '''
        pass
        
    def _add_code_path(self,path):
        '''
        Add a single path (string) to the sys.path 
        '''
        pass
        
    def _remove_code_path(self,path):
        '''
        Remove a single path (string) from the sys.path
        '''
        pass
        
    class Java:
        implements = [JAVA_PATH_PROVIDER]
                
class Py4jServiceBridge(object):
    '''Py4jServiceBridge class
    This class provides and API for consumers to use the Py4jServiceBridge.  This 
    allows a bridge between Python and the OSGi service registry.
    '''
    def __init__(self,service_listener=None,connection_listener=None,gateway_parameters=None,callback_server_parameters=None):
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
        self._connection_listener = connection_listener
        self._connection = None
        self._gateway_parameters = gateway_parameters
        self._callback_server_parameters = callback_server_parameters
    
    def get_id(self):
        '''
        Get the py4j id for this service bridge.
        :return: The py4j id...e.g. py4j://localhost:23334/python for this service bridge
        '''
        cb = self._gateway.get_callback_server()
        return createLocalPy4jId(hostname=cb.get_listening_address(),port=cb.get_listening_port())
    
    def export(self,svc,export_props=None):
        '''
        Export the given svc via the given export_props.  Note that the export_props must have and ENDPOINT_ID
        and contain the other standard Endpoint Description service properties as described by the OSGI
        R5+ Chapter 122 (Remote Service Admin) in the enterprise specification.
        :param svc: The Python service to export to java service registry
        :param export_props: An optional dictionary of Python properties.  Note these properties 
        must contain all the properties describing the service as required by the OSGI Endpoint Description.
        :return: The ENDPOINT_ID value from the export_props.  This value may be used to get subsequent
        access to the svc and/or the export_properties via get_export_endpoint
        '''
        with self._lock:
            self._raise_not_connected()
            '''The Java class attribute must be present'''
            java = getattr(svc,PY4J_JAVA_ATTRIBUTE)
            '''The Java.implements must be present'''
            objClass = getattr(java,PY4J_JAVA_IMPLEMENTS_ATTRIBUTE)
            if isinstance(objClass,str):
                objClass = [objClass]
            props = {}
            '''The export_properties attribute does not need to be present'''
            j_props = getattr(java,EXPORT_PROPERTIES_NAME,None)
            if j_props:
                props = osgiservicebridge.merge_dicts(props,j_props)
            if export_props:
                props = osgiservicebridge.merge_dicts(props,export_props)
            sec = props.get(osgiservicebridge.REMOTE_CONFIGS_SUPPORTED,None)
            if not sec:
                sec = [PY4J_EXPORTED_CONFIG]
            # check that OBJECTCLASS is set, if not
            props1 = get_edef_props(object_class=objClass,exported_cfgs=sec,ep_namespace=None,ecf_ep_id=self.get_id(),ep_rsvc_id=None,ep_ts=None)
            export_props = merge_dicts(props1, props)
        try:
            endpointid = export_props[ENDPOINT_ID]
        except KeyError:
            raise ArgumentError('Cannot export service since no ENDPOINT_ID present in export_props')
        with self._exported_endpoints_lock:
            self._exported_endpoints[endpointid] = (svc,export_props)
        # without holding lock, call __export
        try:
            self.__export(svc,export_props)
        except Exception as e:
            # if it fails, remove from exported endpoints
            self.remove_export_endpoint(endpointid)
            raise e
        return endpointid

    def update(self,update_props):
        '''
        Update the previously exported svc with the given update_props.  Note that the update_props must have and ENDPOINT_ID
        and contain the other standard Endpoint Description service properties as described by the OSGI
        R5+ Chapter 122 (Remote Service Admin) in the enterprise specification.
        :param update_props: A dictionary of Python properties.  Note that these properties must contain all
        the properties describing the service as required by the OSGI Endpoint Description and must contain
        an ENDPOINT_ID that matches the endpoint ID previously returned from export
        :return: old endpoint props (dict) or None
        :raise: ArgumentError if there is not an ENDPOINT_ID value in the update_props
        '''
        with self._lock:
            self._raise_not_connected()
        try:
            endpointid = update_props[ENDPOINT_ID]
        except KeyError:
            raise ArgumentError('Cannot update service since no ENDPOINT_ID present in update_props')
        # get lock and make sure oldendpoint is in exported_endpoints
        with self._exported_endpoints_lock:
            oldendpoint = self.get_export_endpoint(endpointid)
            if oldendpoint:
                self._exported_endpoints[endpointid] = (oldendpoint[0],update_props)
            else:
                return None
        try:
            self.__update(update_props)
        except Exception as e:
            # if exception, restore old endpoint
            with self._exported_endpoints_lock:
                self._exported_endpoints[endpointid] = oldendpoint
            raise e
        return oldendpoint[1]

    def unexport(self,endpointid):
        '''
        Unexport the svc via the given export_props.  Note that the export_props must have an ENDPOINT_ID
        and contain the other standard Endpoint Description service properties as described by the OSGI
        R5+ Chapter 122 (Remote Service Admin) in the enterprise specification.
        :param export_props: A dictionary of Python properties.  Note that these properties must contain all
        the properties describing the service as required by the OSGI Endpoint Description.
        :return: The endpoint service if successfully unexported, None if unsuccessfully
        exported
        '''
        with self._lock:
            self._raise_not_connected()
        if endpointid:
            endpoint = self.remove_export_endpoint(endpointid)
        if endpoint:
            try:
                self.__unexport(endpoint[1])
            except Exception as e:
                with self._exported_endpoints_lock:
                    self._exported_endpoints = endpoint
                raise e
        return endpoint
        
    def get_jvm(self):
        '''
        Get the Py4JService Gateway's JVM
        :return: The Gateway's jvm
        '''
        return self._gateway.jvm
    
    def get_gateway(self):
        '''
        Get the Py4JService JavaGateway 
        :return: The Gateway
        '''
        return self._gateway
    
    def get_callback_server_parameters(self):
        '''
        Get the callback server parameters used for the gateway
        :return: callback server parameters used for the gateway
        '''
        return self._gateway.callback_server_parameters
       
    def get_import_endpoint(self,endpointid):
        '''
        Get import endpoint for given endpointid
        :param endpointid: the endpoint id to find
        :return: the tuple of the endpoint or None if no
        endpoint exists for given endpointid.  Element 0 is the
        service, element 1 is the dictionary of properties.
        '''
        with self._imported_endpoints_lock:
            return self._imported_endpoints.get(endpointid,None)
    
    def get_export_endpoint(self,endpointid):
        '''
        Get export endpoint for given endpointid
        :param endpointid: the endpoint id to find
        :return: the tuple of the endpoint or None if no
        endpoint exists for given endpointid.  Element 0 is the
        service, element 1 is the dictionary of properties.
        '''
        with self._exported_endpoints_lock:
            return self._exported_endpoints.get(endpointid,None)
            
    def get_export_endpoint_for_rsid(self,rsId):  
        with self._exported_endpoints_lock:     
            for eptuple in self._exported_endpoints.values():
                val = eptuple[1][osgiservicebridge.ECF_RSVC_ID]
                if not val is None and val == rsId:
                    return eptuple[0]
        return None
                
    def isconnected(self):
        '''
        Returns True if the gateway callback server is connected,
        False if not
        '''
        with self._lock:
            return True if self._gateway is not None else False

    def connect(self,gateway_parameters=None,callback_server_parameters=None,path_hook=None):
        '''
        Connect gateway to Java side
        :param gateway_parameters an overriding GatewayParameters instance.
        If None then self._gateway_parameters is used.  If that is None, then new GatewayParameters (defaults) is used
        :param callback_server_parameters an overriding CallbackServerParameters instance.
        If None then self._callback_server_parameters is used.  If that is None, then new CallbackServerParameters (defaults) is used
        '''
        if callback_server_parameters:
            self._callback_server_parameters = callback_server_parameters
        else:
            self._callback_server_parameters = self._callback_server_parameters if self._callback_server_parameters else CallbackServerParameters()
        
        if gateway_parameters:
            self._gateway_parameters = gateway_parameters
        else:
            self._gateway_parameters = self._gateway_parameters if self._gateway_parameters else GatewayParameters()
            
        with self._lock:
            if not self._gateway is None:
                raise OSError('already connected to java gateway')
            server_started.connect(self._started)
            self._gateway = JavaGateway(gateway_parameters=self._gateway_parameters,callback_server_parameters=self._callback_server_parameters)
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
            
            class JavaRemoteServiceDiscoverer(object):
                def __init__(self, bridge):
                    self._bridge = bridge
                    
                def _external_discoverService(self,proxy,props):
                    self._bridge._import_service_from_java(proxy,props)
                    
                def _external_updateDiscoveredService(self,props):
                    self._bridge._modify_service_from_java(props)
                    
                def _external_undiscoverService(self,props):
                    self._bridge._unimport_service_from_java(props)
                    
                def _call_endpoint(self,rsId,methodName,serializedArgs):
                    endpoint = self._bridge.get_export_endpoint_for_rsid(rsId)
                    if endpoint:
                        return endpoint._raw_bytes_from_java(methodName,serializedArgs)
                    else:
                        msg = 'No endpoint for rsId=%s methodName=%s serializedArgs=%s' % (rsId,methodName,serializedArgs)
                        _logger.error(msg)
                        raise Exception(msg)
                    
                class Java:
                    implements = [JAVA_DIRECT_ENDPOINT_CLASS, PY4J_CALL_BY_VALUE_CLASS]

            if not path_hook:
                path_hook = JavaPathHook(self)
            self._bridge = JavaRemoteServiceDiscoverer(self)
            '''Call _getExternalDirectDiscovery first, so that we are ready to export'''
            self._consumer = self._gateway.entry_point._getExternalDirectDiscovery()
            '''Then call _setDirectBridge so that java side can now call us
            to notify about exported services'''
            path_list = self._gateway.entry_point._setDirectBridge(path_hook,self._bridge, self._bridge, self.get_id())
            path_hook.initialize_path(path_list)
    
    def get_module_type(self,modname):
        with self._lock:
            if not self.isconnected():
                raise ImportError()
        return self._gateway.entry_point.getModuleType(modname)
    
    def get_module_code(self,modname,ispackage):
        with self._lock:
            if not self.isconnected():
                raise ImportError()
        return self._gateway.entry_point.getModuleCode(modname,ispackage)
    
    def disconnect(self):
        with self._lock:
            if self.isconnected():
                self._gateway.close()
                self._gateway = None
                self._consumer = None   
                self._bridge = None 
        with self._imported_endpoints_lock:
            self._imported_endpoints.clear()
        with self._exported_endpoints_lock:
            self._exported_endpoints.clear()
     
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
        if self._connection_listener:
            self._connection_listener.connection_stopped(kwargs['connection'],kwargs.pop('exception',None))
    
    def _pre_shutdown(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.pre_shutdown(kwargs["server"])

    def _post_shutdown(self, sender, **kwargs):
        with self._imported_endpoints_lock:
            for endpointid in self._imported_endpoints.keys():
                endpoint = self._imported_endpoints.get(endpointid,None)
                if self._service_listener and endpoint:
                    try:
                        self._service_listener.service_unimported(self, endpointid, endpoint[0], endpoint[1])
                    except:
                        _logger.exception('_unimport_service_from_java listener threw exception endpointid={0}'.format(endpointid))
        self.disconnect();
        if self._connection_listener:
            self._connection_listener.post_shutdown(kwargs["server"])
    
    # Methods called by java  
    def _import_service_from_java(self,proxy,props):
            local_props = prepare_java_props(props)
            endpointid = local_props.get(ENDPOINT_ID,None)
            if endpointid:
                endpoint = (proxy,local_props)
                with self._imported_endpoints_lock:
                    self._imported_endpoints[endpointid] = endpoint
                if self._service_listener:
                    try:
                        self._service_listener.service_imported(self, endpointid, endpoint[0], endpoint[1])
                    except:
                        _logger.exception('__import_service_from_java listener threw exception endpointid={0}'.format(endpointid))

    def _modify_service_from_java(self,newprops):
        newendpoint = None
        python_newprops = self._prepare_props(newprops)
        endpointid = python_newprops.get(ENDPOINT_ID,None)
        if endpointid:
            with self._imported_endpoints_lock:
                oldendpoint = self._imported_endpoints[endpointid]
                newendpoint = (oldendpoint[0],python_newprops)
                self._imported_endpoints[endpointid] = newendpoint
            if self._service_listener:
                try:
                    self._service_listener.service_modified(self, endpointid, newendpoint[0], newendpoint[1])
                except:
                    _logger.exception('__modify_service_from_java listener threw exception endpointid={0}'.format(endpointid))
   
    def _unimport_service_from_java(self,props):
        endpoint = None
        local_props = prepare_java_props(props)
        endpointid = local_props.get(ENDPOINT_ID,None)
        if endpointid:
            endpoint = self.remove_import_endpoint(endpointid)
        if self._service_listener and endpoint:
            try:
                self._service_listener.service_unimported(self, endpointid, endpoint[0], endpoint[1])
            except:
                _logger.exception('__unimport_service_from_java listener threw exception endpointid={0}'.format(endpointid))
                
    def remove_export_endpoint(self,endpointid):
        with self._exported_endpoints_lock:
            return self._exported_endpoints.pop(endpointid, None)

    def remove_import_endpoint(self,endpointid):
        with self._imported_endpoints_lock:
            return self._imported_endpoints.pop(endpointid, None)
        
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
            raise OSError('Not connected to java gateway')

    def __export(self,svc,props):
        try:
            self._consumer._java_discoverService(svc,self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e
    
    def __update(self,props):
        try:
            self._consumer._java_updateDiscoveredService(self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e
           
    def __unexport(self,props):
        try:
            self._consumer._java_undiscoverService(self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e

class JavaRemoteService(object):
    
    def __init__(self,endpoint_props,proxy):
        self._endpoint_props = endpoint_props
        self._proxy = proxy
        
    def get_endpoint_props(self):
        return self._endpoint_props
    
    def get_proxy(self):
        return self._proxy
    
    def get_objectclass(self):
        return self._endpoint_props[osgiservicebridge.OBJECT_CLASS]
    
    def has_service(self,interface):
        return (interface in self.get_objectclass())
    
    def get_endpointid(self):
        return self._endpoint_props(osgiservicebridge.ENDPOINT_ID)   

class RemoteServiceRegistryListener(object):
    
    def add_remote_service(self,remote_service):
        pass
    
    def remove_remote_service(self,remote_service):
        pass
    
class JavaRemoteServiceRegistry(object):
    
    def __init__(self):
        super(JavaRemoteServiceRegistry,self).__init__()
        self._lock = RLock()
        self._remote_services = {}
        self._listeners = []
    
    def register_listener(self, listener):
        with self._lock:
            self._listeners.append(listener)
            
    def unregister_listener(self, listener):
        with self._lock:
            self._listeners.remove(listener)
    
    def _fire_remote_service_event(self,listeners,eventtype,remote_service):     
        for listener in listeners:
            func = None
            if eventtype == 0:
                func = getattr(listener,'add_remote_service')
            else:
                func = getattr(listener,'remove_remote_service')
            try:
                func(remote_service)
            except Exception as e:
                _logger.error(e)
                
    def _add_remoteservice(self,endpoint_props,proxy):
        remote_service = JavaRemoteService(endpoint_props,proxy)
        with self._lock:
            listeners = list(self._listeners)
            self._remote_services[endpoint_props[osgiservicebridge.ENDPOINT_ID]] = remote_service
        self._fire_remote_service_event(listeners, 0, remote_service)
            
    def _remove_remoteservice(self,endpointid):
        with self._lock:
            listeners = list(self._listeners)
            removed_service = self._remote_services.pop(endpointid)
        if removed_service:
            self._fire_remote_service_event(listeners, 1, removed_service)
        
    def _modify_remoteservice(self,endpointid,endpoint_props):
        with self._lock:
            rs = self._remote_services[endpointid]
            if rs:
                self._remote_services[endpointid] = JavaRemoteService(endpoint_props,rs.get_proxy())
                return rs
            return None
    
    def _dispose(self):
        with self._lock:
            self._remote_services.clear()
            
    def lookup_services(self,interface):
        with self._lock:
            return [rs for rs in self._remote_services.values() if rs.has_service(interface)]

        
class OSGIPythonModulePathHook(object):
    def __init__(self,bridge):
        self._bridge = bridge
        
    def __call__(self,*args):
        uri = args[0]
        if not uri.startswith('py4j:'):
            raise ImportError()
        return OSGIPythonModuleFinder(self._bridge,uri)

    def initialize_path(self,path_list):
        sys.path_hooks.append(self)
        sys.path.extend(path_list)
        sys.path_importer_cache.clear()
        
    def _add_code_path(self,path):
        sys.path.append(path)
        sys.path_importer_cache.clear()
        
    def _remove_code_path(self,path):
        sys.path.remove(path)
        sys.path_importer_cache.clear()
        
    class Java:
        implements = [JAVA_PATH_PROVIDER]

class OSGIPythonModuleFinder(object):
    
    def __init__(self, bridge, path_entry):
        self._bridge = bridge
        self._path_entry = path_entry
        
    def find_spec(self, modname, target=None):
        python_type = 0
        pckg = False
        try:
            python_type = self._bridge.get_module_type(self._path_entry + modname)
        except:
            _logger.exception('error in Py4jFinder.find_spec._bridge.get_module_type uri={0}'.format(self._path_entry + modname))
            return None
        if python_type == 0:
            return None
        modnames = modname.split('.')
        origin = self._path_entry + '/'.join(modnames)
        if python_type == 1:
            pckg = True
            origin += '/'
        elif python_type == 2:
            origin += '.py'
        else:
            return None
        spec = importlib.util.spec_from_loader(modname,OSGIPythonModuleLoader(self._bridge, self._path_entry, pckg),origin=origin,is_package=pckg)
        spec.submodule_search_locations = [self._path_entry]
        return spec

class OSGIPythonModuleLoader(object):
    
    def __init__(self, bridge, path_entry, ispackage):
        self._bridge = bridge
        self._path_entry = path_entry
        self._ispackage = ispackage
        
    def create_module(self, target):
        return None

    def exec_module(self, module):
        modname = module.__name__
        code = None
        try:
            code = self._bridge.get_module_code(self._path_entry + modname, self._ispackage)
        except Exception as e:
            _logger.exception('error in Py4jOSGILoader.exec_module._bridge.get_module_code uri={0}'.format(self._path_entry + modname))
            raise e
        if not code:
            code = ''
        try:
            exec(compile(code+'\n', module.__spec__.origin, 'exec'), module.__dict__, module.__dict__)
        except:
            _logger.exception('error in Py4jOSGILoader.exec_module.exec code={0}'.format(code))
            raise e

JAVA_OBJECT_METHODS = [ 'equals', 'hashCode', 'wait', 'notify', 'notifyAll', 'toString']
JAVA_NONASYNC_RETURN_TYPES = { 'java.lang.Object', 'java.lang.String', 'java.lang.Integer', 'java.lang.Long', 'java.lang.Boolean', 'java.lang.Short', 'java.lang.Byte',
                              'java.lang.Double', 'java.lang.Throwable','java.lang.Enum','java.lang.Float'}
SYNC_TYPE = 0
FUTURE_ASYNC_TYPE = 1
COMPLETION_STAGE_ASYNC_TYPE = 2
PROMISE_ASYNC_TYPE = 3
IFUTURE_ASYNC_TYPE = 4
COMPLETABLE_FUTURE_ASYNC_TYPE = 5

COMPLETABLE_FUTURE_CLASS_NAME = 'java.util.concurrent.CompletableFuture'
COMPLETION_STAGE_INTF_NAME = 'java.util.concurrent.CompletionStage'
FUTURE_INTF_NAME = 'java.util.concurrent.Future'
IFUTURE_INTF_NAME = 'org.eclipse.equinox.concurrent.future.IFuture'
PROMISE_CLASS_NAME = 'org.osgi.util.promise.Promise'

ASYNC_CLASS_NAMES = {COMPLETABLE_FUTURE_CLASS_NAME:COMPLETABLE_FUTURE_ASYNC_TYPE,PROMISE_CLASS_NAME:PROMISE_ASYNC_TYPE}
ASYNC_INTF_NAMES = {COMPLETION_STAGE_INTF_NAME:COMPLETION_STAGE_ASYNC_TYPE,FUTURE_INTF_NAME:FUTURE_ASYNC_TYPE,IFUTURE_INTF_NAME:IFUTURE_ASYNC_TYPE}

DEFAULT_METHOD_TIMEOUT = 30  # seconds
DEFAULT_TIMEOUT_FACTOR = 1.0 # multiplier to get seconds

def invoke_sync_timeout(executor,timeout,result_fn,call_fn,*args):  
    '''
    invoke synchronously with timeout.  This method uses the given executor
    to submit call_fn/*args and then after result has been returned (with given timeout)
    the result_fn is optionally called with the returned result, executor, timeout, call_fn
    must not be None.  
    '''
    if not executor:
        raise Exception('cannot submit call_fn={0} without executor'.format(call_fn))  
    call_result = executor.submit(call_fn,*args).result(timeout)
    if result_fn:
        return result_fn(call_result)
    return call_result
    
def invoke_future(executor,call_fn,*args):  
    '''
    submit via given executor and return future that represents the result of the call_fn/*args call
    '''  
    return executor.submit(call_fn,*args)

def get_jasyncresult(result_func,async_type,timeout,timeunit,jasync):
    '''
    Get the result of calling the appropriate 'get' method on the jasync
    type.  Type must match the given async_type.  E.g. if async_type is java's 
    CompletableFuture, the jasync.get(timeout,timeunit) method will be called.
    If Promise, then jasync.getValue() will be called, etc for all the given
    types.  Then result_func (if not None) will be called on the result of the java.get()
    method call.
    '''
    jresult = None
    if not async_type:
        jresult = jasync
    elif async_type == FUTURE_ASYNC_TYPE or async_type == COMPLETABLE_FUTURE_ASYNC_TYPE:
        if timeunit:
            jresult = jasync.get(timeout,timeunit)
        else:
            jresult = jasync.get()
    elif async_type == COMPLETION_STAGE_ASYNC_TYPE:
        # call recursively after calling jresult.toComple
        return get_jasyncresult(result_func, COMPLETABLE_FUTURE_ASYNC_TYPE, timeout, timeunit, jasync.toCompletableFuture())
    elif async_type == PROMISE_ASYNC_TYPE:
        jresult = jasync.getValue()
    elif async_type == IFUTURE_ASYNC_TYPE:
        jresult = jasync.get()
    if result_func:
        return result_func(jresult)
    return jresult

def get_interfaces(jvm, interfaces, fn, proxy, executor, timeout, timeunit=None):
    '''
    Given list of interfaces, return dict of interface: [ServiceMethods] for each interface in interfaces
    '''
    if not timeunit:
        timeunit = jvm.java.util.concurrent.TimeUnit.SECONDS
    return { interface: fn(jvm, jvm.java.lang.Class.forName(interface).getMethods(), proxy, executor, timeout, timeunit) for interface in interfaces }

def get_return_methtype(jvm,class_name,executor,timeout,timeunit):
    '''
    Get java ReturnMethodType for given class_name,executor,timeout and timeunit
    '''
    async_type = SYNC_TYPE
    if executor:
        if class_name in ASYNC_CLASS_NAMES.keys():
            async_type = ASYNC_CLASS_NAMES[class_name]
        elif class_name in ASYNC_INTF_NAMES:
            async_type = ASYNC_INTF_NAMES[class_name]
    return ReturnMethodType(jvm, executor,async_type,timeout,timeunit)

def get_service_methods(jvm, cls, jmethods, service, executor, timeout, timeunit):
    '''
    get list of service methods by invoking cls constructor with service, jmethod.getName(), and ReturnMethodType for method return type
    '''
    return [cls(service,
                jmethod.getName(),
                get_return_methtype(jvm,
                                    jmethod.getReturnType().getName(),
                                    executor,
                                    timeout,
                                    timeunit)) for jmethod in jmethods if not jmethod.getName() in JAVA_OBJECT_METHODS ]

def get_java_service_methods(jvm,jmethods, proxy, executor, timeout, timeunit):
    '''
    get JavaServiceMethod (remote) methods given list of java Methods instances (jmethods)
    '''
    return get_service_methods(jvm,
                               JavaServiceMethod,
                               jmethods, 
                               proxy, 
                               executor, 
                               timeout, 
                               timeunit)

def get_python_service_methods(jvm, jmethods, service, executor, timeout, timeunit):
    '''
    get PythonServiceMethod (local) methods given list of java Methods instances (jmethods)
    '''
    return get_service_methods(jvm, 
                               PythonServiceMethod,
                               jmethods, 
                               service, 
                               executor, 
                               timeout, 
                               timeunit)

class ReturnMethodType():
    '''
    Method type to represent the type of Java method.  e.g. one of SYNC_TYPE, FUTURE_ASYNC_TYPE,
    COMPLETION_STAGE_ASYNC_TYPE, PROMISE_ASYNC_TYPE, IFUTURE_ASYNC_TYPE. 
    '''
    def __init__(self,jvm,executor,async_type,timeout,jtimeunit=None):
        self._jvm = jvm
        self._executor = executor
        self._async_type = async_type
        self._timeout = timeout
        self._jtimeunit = jtimeunit
    
    def _jproxy_jasyncresult(self, result_fn, call_fn, *args):
        return get_jasyncresult(result_fn,
                                self._async_type,
                                self._timeout,
                                self._jtimeunit,
                                call_fn(*args))
      
    def _invoke_jproxy(self, result_fn, call_fn, *args):
        if self._async_type:
            return invoke_future(self._executor,
                                 invoke_sync_timeout,
                                 self._executor, 
                                 self._timeout,
                                 None,
                                 self._jproxy_jasyncresult, 
                                 result_fn, 
                                 call_fn, 
                                 *args)
        else:
            return invoke_sync_timeout(self._executor,
                                       self._timeout,
                                       result_fn, 
                                       call_fn, 
                                       *args)
        
    def _invoke_pservice(self, result_fn, call_fn, *args):
        return invoke_sync_timeout(self._executor, self._timeout, result_fn, call_fn, *args)
    
    def _convert_async_return(self,presult):
        if self._async_type == SYNC_TYPE:
            return presult
        else:
            cf = self._jvm.java.util.concurrent.CompletableFuture()
            cf.complete(presult)
            return cf
            
class ServiceMethod(): 
    
    def __init__(self,service,method_name,method_type):
        assert service
        self._service = service
        assert method_name and isinstance(method_name,str)
        self._methodname = method_name
        assert method_type and isinstance(method_type,ReturnMethodType)
        self._method_type = method_type

    def _call_sync(self,*args):
        t0 = time.time()
        result = getattr(self._service,self._methodname)(*args)
        _timing.debug("method.syncexec;time={0}ms".format(1000*(time.time()-t0)))
        return result

    def _process_result(self,result):
        return result
    
    def __call__(self,*args):
        raise Exception("__call__ cannot be called on ServiceMethod superclass.  Subclasses must override")

class JavaServiceMethod(ServiceMethod):
    
    def __call__(self,*args):
        # If it's a java service proxy, we call self._method_type invoke_jproxy
        return self._method_type._invoke_jproxy(self._process_result,self._call_sync,*args)

class PythonServiceMethod(ServiceMethod):

    def _process_result(self,presult):
        return self._method_type._convert_async_return(presult)
    
    def __call__(self,*args):
        # If it's a PythonHostServiceMethod, then we invoke _invoke_pservice
        return self._method_type._invoke_pservice(self._process_result,self._call_sync,*args)
    
class Py4jService():
    
    def __init__(self, service):
        self._interfaces = []
        self._service = service
        
    def _find_method(self,name):
        for method_list in self._interfaces.values():
            for method in method_list:
                if name  == method._methodname:
                    return method
        return None
    
    def __getattr__(self, name):
        if name == "__call__":
            raise AttributeError("Cannot call method '__call__' on service={0}".format(self._proxy))
        # find java_method (ServiceMethod) with same name
        java_method = self._find_method(name)
        # if not found, we throw
        if not java_method:
            raise AttributeError("'{0}' not found on {1};specs={2}".format(name,self.__str__(),[intf for intf in self._interfaces.keys()]))
        # else return it
        return java_method
    
    def __str__(self):
        return 'Py4jService;{0}'.format(str(self._service))

class JavaServiceProxy(Py4jService):
    
    def __init__(self, jvm, interfaces, proxy, executor, timeout):
        Py4jService.__init__(self, proxy)
        self._interfaces = get_interfaces(jvm, interfaces, get_java_service_methods, proxy, executor, timeout)
        
class PythonService(Py4jService):

    def __init__(self, bridge, interfaces, svc_object, executor, timeout):
        Py4jService.__init__(self, svc_object)
        osgiservicebridge._modify_remoteservice_class(PythonService,{ 'objectClass': interfaces })
        self._interfaces = get_interfaces(bridge.get_jvm(), interfaces, get_python_service_methods, svc_object, executor, timeout)
