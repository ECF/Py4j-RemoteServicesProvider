'''
OSGi service bridge Py4j classes
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
from logging import getLogger as getLibLogger
from threading import RLock

from py4j.java_collections import ListConverter, MapConverter, JavaArray, JavaList, JavaSet
from osgiservicebridge import merge_dicts, ENDPOINT_ID, get_edef_props, PY4J_EXPORTED_CONFIGS, PY4J_NAMESPACE,\
 PY4J_SERVICE_INTENTS,PY4J_PROTOCOL, PY4J_PYTHON_PATH
import osgiservicebridge
from argparse import ArgumentError

from py4j.java_gateway import (
    server_connection_started, server_connection_stopped,
    server_started, server_stopped, pre_server_shutdown, post_server_shutdown,
    JavaGateway, CallbackServerParameters, DEFAULT_ADDRESS, DEFAULT_PORT, DEFAULT_PYTHON_PROXY_PORT)

'''
Py4J constants
'''
PY4J_DEFAULT_GATEWAY_PORT = DEFAULT_PORT
PY4J_DEFAULT_CB_PORT = DEFAULT_PYTHON_PROXY_PORT
PY4J_DEFAULT_HOSTNAME = DEFAULT_ADDRESS

JAVA_DIRECT_ENDPOINT_CLASS = 'org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider'

# Version
__version_info__ = (0, 1, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

_logger = getLibLogger(__name__)

# ------------------------------------------------------------------------------
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
    '''
    Prepare java properties (as Map) for use as Python dictionary of properties.  The result will
    be a dictionary of Python items, all of which have been converted via prepare_java_prim above.
    :param java_props:  Java Map of properties to be converted
    :return: dictionary of properties with same (string) keys and values converted via prepare_java_prim
    '''
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

class Py4jServiceBridgeEventListener(object):
    '''
    Listener adapter class for service bridge events.   Subclasses may set themselves up
    as a listener for Py4jServiceBridge events (service_imported, service_modified,
    service_unimported see below) by extending this class and overriding
    these methods, and then providing their listener instance to the Py4jServiceBridge instance
    creation'''
    
    def service_imported(self, servicebridge, endpointid, endpoint):
        '''
        Service imported notification.
        :param servicebridge: The Py4jServiceBridge instance that received the notification
        :param endpointid:  The endpointid from given set of properties.  Will not be None.
        :param endpoint:  The endpoint instance as a tuple where first item is the proxy, and
        the second item is the entire dictionary of properties.
        '''
        _logger.info('_service_imported endpointid='+endpointid)
    
    def service_modified(self, servicebridge, endpointid, endpoint):
        '''
        Service modified notification.
        :param servicebridge: The Py4jServiceBridge instance that received the notification
        :param endpointid:  The endpointid from given set of properties.  Will not be None.
        :param endpoint:  The endpoint instance as a tuple where first item is the proxy, and
        the second item is the dictionary of properties.
        '''
        _logger.info('_service_modified endpointid='+endpointid)
    
    def service_unimported(self, servicebridge, endpointid, endpoint):
        '''
        Service modified notification.
        :param servicebridge: The Py4jServiceBridge instance that received the notification
        :param endpointid:  The endpointid from given set of properties.  Will not be None.
        :param endpoint:  The endpoint instance as a tuple where first item is the proxy, and
        the second item is the dictionary of properties.
        '''
        _logger.info('_service_unimported endpointid='+endpointid)
 
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
    def __init__(self,service_listener=None,connection_listener=None,callback_server_parameters=None):
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
        if export_props is None:
            '''The Java class attribute must be present'''
            java = getattr(svc,'Java')
            '''The Java.package_version may be optionally present'''
            pkgvers = getattr(java,'package_version',None)
            '''The Java.implements must be present'''
            export_props = get_edef_props(svc.Java.implements, self.get_id(),pkg_ver = pkgvers)
        try:
            endpointid = export_props[ENDPOINT_ID]
        except KeyError:
            raise ArgumentError('Cannot export service since no ENDPOINT_ID present in export_props')
        with self._exported_endpoints_lock:
            self._exported_endpoints[endpointid] = (svc,export_props)
        self.__export(svc,export_props)
        return endpointid

    def update(self,update_props):
        '''
        Update the previously exported svc with the given update_props.  Note that the update_props must have and ENDPOINT_ID
        and contain the other standard Endpoint Description service properties as described by the OSGI
        R5+ Chapter 122 (Remote Service Admin) in the enterprise specification.
        :param update_props: A dictionary of Python properties.  Note that these properties must contain all
        the properties describing the service as required by the OSGI Endpoint Description and must contain
        an ENDPOINT_ID that matches the endpoint ID previously returned from export
        :return: True if updated, False if not
        :raise: ArgumentError if there is not an ENDPOINT_ID value in the update_props
        '''
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
        endpointid = None
        try:
            endpointid = export_props[ENDPOINT_ID]
        except KeyError:
            raise ArgumentError('Cannot export service since no ENDPOINT_ID present in export_props')
        if endpointid:
            endpoint = self._remove_export_endpoint(endpointid)
            if endpoint:
                self.__unexport(endpoint[1])
                return endpoint
        return None
        
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
            
    def isconnected(self):
        '''
        Returns True if the gateway callback server is connected,
        False if not
        '''
        with self._lock:
            return True if self._gateway is not None else False

    def connect(self,callback_server_parameters=None):
        '''
        Connect gateway to Java side
        :param callback_server_parameters the CallbackServerParameters instance.
        If None then a new CallbackServerParameters() instance is used.
        '''
        if not self._callback_server_parameters:
            self._callback_server_parameters = callback_server_parameters if callback_server_parameters else CallbackServerParameters()
        else:
            self._callback_server_parameters = callback_server_parameters if callback_server_parameters else self._callback_server_parameters
        
        with self._lock:
            if not self._gateway is None:
                raise OSError('already connected to java gateway')
            server_started.connect(self.__started)
            self._gateway = JavaGateway(callback_server_parameters=self._callback_server_parameters)
            cbserver = self._gateway.get_callback_server()
            server_stopped.connect(
                self.__stopped, sender=cbserver)
            server_connection_started.connect(
                self.__connection_started,
                sender=cbserver)
            server_connection_stopped.connect(
                self.__connection_stopped,
                sender=cbserver)
            pre_server_shutdown.connect(
                self.__pre_shutdown, sender=cbserver)
            post_server_shutdown.connect(
                self.__post_shutdown, sender=cbserver)
            self._consumer = self._gateway.entry_point.getJavaConsumer()
            class JavaRemoteServiceExporter(object):
                def __init__(self, bridge):
                    self._bridge = bridge
                    
                def exportService(self,proxy,props):
                    self._bridge.__import_service_from_java(proxy,props)
                    
                def modifyService(self,props):
                    self._bridge.__modify_service_from_java(props)
                    
                def unexportService(self,props):
                    self._bridge.__unimport_service_from_java(props)
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


    def __started(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.started(kwargs["server"])
    
    def __stopped(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.stopped(kwargs["server"])
    
    def __connection_started(self, sender, **kwargs):
        with self._lock:
            self._connection = kwargs["connection"]
        if self._connection_listener:
            self._connection_listener.connection_started(self._connection)
    
    def __connection_stopped(self, sender, **kwargs):
        with self._lock:
            self._connection = None
        if self._connection_listener:
            self._connection_listener.connection_stopped(kwargs['connection'],kwargs.pop('exception',None))
    
    def __pre_shutdown(self, sender, **kwargs):
        if self._connection_listener:
            self._connection_listener.pre_shutdown(kwargs["server"])

    
    def __post_shutdown(self, sender, **kwargs):
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
                        _logger.error('__unimport_service_from_java listener threw exception endpointid='+endpointid, e)
        self.disconnect();
        if self._connection_listener:
            self._connection_listener.post_shutdown(kwargs["server"])
    
    # Methods called by java  
    def __import_service_from_java(self,proxy,props):
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
                    _logger.error('__import_service_from_java listener threw exception endpointid='+endpointid, e)
        except Exception as e:
            _logger.error(e)
            raise e

    def __modify_service_from_java(self,newprops):
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
                self._service_listener.service_modified(self, endpointid, newendpoint)
            except Exception as e:
                _logger.error('__modify_service_from_java listener threw exception endpointid='+endpointid, e)
   
    def __unimport_service_from_java(self,props):
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
