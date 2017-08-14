'''
OSGi service bridge Py4j classes
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

from osgiservicebridge.version import __version__ as __v__
# Version
__version__ = __v__

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------
from logging import getLogger as getLibLogger

_logger = getLibLogger(__name__)

# ------------------------------------------------------------------------------

from threading import RLock, Condition

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
from osgiservicebridge import merge_dicts, ENDPOINT_ID, get_edef_props, PY4J_EXPORTED_CONFIGS, PY4J_NAMESPACE,\
 PY4J_SERVICE_INTENTS,PY4J_PROTOCOL, PY4J_PYTHON_PATH, PY4J_JAVA_ATTRIBUTE, PY4J_JAVA_IMPLEMENTS_ATTRIBUTE,\
    EXPORT_PROPERTIES_NAME
 
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
    for entry in java_props.entrySet():
        result[entry.getKey()] = prepare_java_prim(entry.getValue())
    return result

class Py4jServiceBridgeEventListener(object):
    '''
    Listener adapter class for service bridge events.   Subclasses may set themselves up
    as a listener for Py4jServiceBridge events (service_imported, service_modified,
    service_unimported see below) by extending this class and overriding
    these methods, and then providing their listener instance to the Py4jServiceBridge instance
    creation'''
    
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
        self._connection_listener = None
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
            sec = props.pop(osgiservicebridge.SERVICE_EXPORTED_CONFIGS,None)
            props1 = get_edef_props(object_class=objClass, ecf_ep_id=self.get_id(),exported_cfgs=sec)
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
            self._remove_export_endpoint(endpointid)
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
            with self._exported_endpoints_lock:
                endpoint = self._remove_export_endpoint(endpointid)
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
        with self._lock:
            self._raise_not_connected();
        return self._gateway.jvm
    
    def get_gateway(self):
        '''
        Get the Py4JService JavaGateway 
        :return: The Gateway
        '''
        with self._lock:
            self._raise_not_connected()
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
            try:
                return self._imported_endpoints[endpointid]
            except KeyError:
                return None
    
    def get_export_endpoint(self,endpointid):
        '''
        Get export endpoint for given endpointid
        :param endpointid: the endpoint id to find
        :return: the tuple of the endpoint or None if no
        endpoint exists for given endpointid.  Element 0 is the
        service, element 1 is the dictionary of properties.
        '''
        with self._exported_endpoints_lock:
            try:
                return self._exported_endpoints[endpointid]
            except KeyError:
                return None
            
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

    def connect(self,gateway_parameters=None,callback_server_parameters=None):
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

            self._bridge = JavaRemoteServiceDiscoverer(self)
            '''Call _getExternalDirectDiscovery first, so that we are ready to export'''
            self._consumer = self._gateway.entry_point._getExternalDirectDiscovery()
            '''Then call _setDirectBridge so that java side can now call us
            to notify about exported services'''
            self._gateway.entry_point._setDirectBridge(self._bridge, self._bridge, self.get_id())
            
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
     
    def make_rsa_props(self,object_class, rsvc_id, fw_id, pkg_ver):
        '''
        Make dictionary with all RSA-required properties, using this service bridge's hostname and port to create the ENDPOIN
        :param object_class: the value for the objectClass required property.  Must be a list of strings.
        :param rsvc_id: the remote service id to use.  Must be of type integer.
        :param fw_id: the framework id to use.  Must be of type string and globally unique (e.g. string of uuid4)
        :param pkg_vers: a list of tuples with a package name as first item in tuple (String, and the version string as the second item.  Example:  [('org.eclipse.ecf','1.0.0')].  If None, nothing is added to the returned dictionary.
        :return: Dictionary containing all required RSA properties.
        '''
        with self._lock:
            self._raise_not_connected()
            osgiprops = osgiservicebridge.get_rsa_props(object_class, PY4J_EXPORTED_CONFIGS, PY4J_SERVICE_INTENTS, rsvc_id, fw_id, pkg_ver)
            cbserver = self._gateway.get_callback_server()
            if cbserver:
                hostname = str(cbserver.get_listening_address())
                port = cbserver.get_listening_port()
                myid = createLocalPy4jId(hostname, port)
            else:
                myid = createLocalPy4jId()
            ecfprops = osgiservicebridge.get_ecf_props(myid, PY4J_NAMESPACE, rsvc_id)
            return osgiservicebridge.merge_dicts(osgiprops,ecfprops)


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
                endpoint = None
                try:
                    endpoint = self._imported_endpoints[endpointid]
                except KeyError:
                    pass
                if self._service_listener and endpoint:
                    try:
                        self._service_listener.service_unimported(self, endpointid, endpoint[0], endpoint[1])
                    except Exception as e:
                        _logger.error('_unimport_service_from_java listener threw exception endpointid='+endpointid, e)
        self.disconnect();
        if self._connection_listener:
            self._connection_listener.post_shutdown(kwargs["server"])
    
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
                    self._service_listener.service_imported(self, endpointid, endpoint[0], endpoint[1])
                except Exception as e:
                    _logger.error('__import_service_from_java listener threw exception endpointid='+endpointid, e)
        except Exception as e:
            _logger.error(e)
            raise e

    def _modify_service_from_java(self,newprops):
        newendpoint = None
        python_newprops = self._prepare_props(newprops)
        try:
            endpointid = python_newprops[ENDPOINT_ID]
            with self._imported_endpoints_lock:
                endpoint = self._imported_endpoints[endpointid]
                self._imported_endpoints[endpointid] = (endpoint[0],python_newprops)
        except KeyError:
            pass
        if self._service_listener and newendpoint:
            try:
                self._service_listener.service_modified(self, endpointid, newendpoint[0], newendpoint[1])
            except Exception as e:
                _logger.error('__modify_service_from_java listener threw exception endpointid='+endpointid, e)
   
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
                self._service_listener.service_unimported(self, endpointid, endpoint[0], endpoint[1])
            except Exception as e:
                _logger.error('__unimport_service_from_java listener threw exception endpointid='+endpointid, e)
                
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
