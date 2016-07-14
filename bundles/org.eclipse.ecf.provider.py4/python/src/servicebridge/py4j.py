'''
Created on Jul 11, 2016

@author: slewis
'''
from logging import getLogger as getLibLogger
from threading import RLock

from py4j.java_gateway import JavaGateway, CallbackServerParameters
from py4j.java_collections import ListConverter, MapConverter
from servicebridge import merge_dicts, ENDPOINT_ID
import servicebridge
from argparse import ArgumentError

PY4J_EXPORTED_CONFIG = 'ecf.py4j.host.python'
PY4J_EXPORTED_CONFIGS = [PY4J_EXPORTED_CONFIG]
PY4J_PROTOCOL = 'py4j'
PY4J_DEFAULT_GATEWAY_PORT = 25333
PY4J_DEFAULT_CB_PORT = 25334
PY4J_DEFAULT_HOSTNAME = 'localhost'
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

class Py4jServiceBridgeEventListener(object):
    '''
    Listener adapter class.   Subclasses can set themselves up
    as a listener for Py4jBridge events (service_imported, service_modified,
    service_unimported see below), buy extending this class and overriding
    these methods, and then providing the listener to the Py4jBridge instance
    creation'''
    def service_imported(self,endpointid,endpoint):
        _logger.info('_service_imported endpointid='+endpointid)
    
    def service_modified(self,endpointid,endpoint):
        _logger.info('_service_modified endpointid='+endpointid)
    
    def service_unimported(self,endpointid,endpoint):
        _logger.info('_service_unimported endpointid='+endpointid)
    
    
class Py4jServiceBridge(object):
    '''Py4jServiceBridge class
    This class provides and API for consumers to use the Py4jServiceBridge.  This 
    allows a bridge between Python and the OSGi service registry.
    '''
    def __init__(self,listener=None):
        self._gateway = None
        self._lock = RLock()
        self._consumer = None
        self._endpoints_lock = RLock()
        self._endpoints = {}
        self._exported_endpoints_lock = RLock()
        self._exported_endpoints = {}
        self._map_converter = MapConverter()
        self._list_converter = ListConverter()
        self._listener = listener
    
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
        
    def get_import_endpoint(self,endpointid):
        with self._endpoints_lock:
            try:
                return self._endpoints[endpointid]
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
        
    def connect(self,callback_server_parameters=None):
        if not callback_server_parameters:
            callback_server_parameters = CallbackServerParameters()
        with self._lock:
            if not self._gateway is None:
                raise ConnectionError('already connected to java gateway')
            self._gateway = JavaGateway(callback_server_parameters=callback_server_parameters)
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

    def make_rsa_props(self,object_class, rsvc_id, fw_id, pkg_ver):
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

    

# Methods called by listener    
    def _import_service_from_java(self,proxy,props):
        endpointid = None
        try:
            endpointid = props[ENDPOINT_ID]
        except KeyError:
            pass
        if endpointid:
            endpoint = (proxy,props)
            with self._endpoints_lock:
                self._endpoints[endpointid] = endpoint
        if self._listener and endpointid:
            try:
                self._listener.service_imported(endpointid,endpoint)
            except Exception as e:
                _logger.error('_import_service_from_java listener threw exception endpointid='+endpointid, e)

    def _modify_service_from_java(self,props):
        newendpoint = None
        try:
            endpointid = props[ENDPOINT_ID]
            with self._endpoints_lock:
                endpoint = self._endpoints[endpointid]
                props = endpoint[1]
                props.update(props)
                newendpoint = (endpoint[0],props)
                self._endpoints[endpointid] = newendpoint
        except KeyError:
            pass
        if self._listener and newendpoint:
            try:
                self._listener.service_modified(endpointid,newendpoint)
            except Exception as e:
                _logger.error('_modify_service_from_java listener threw exception endpointid='+endpointid, e)
   
    def _unimport_service_from_java(self,props):
        endpoint = None
        endpointid = None
        try:
            endpointid = props[ENDPOINT_ID]
            with self._endpoints_lock:
                endpoint = self._endpoints.pop(endpointid, None)
        except KeyError:
            pass
        if self._listener and endpoint:
            try:
                self._listener.service_unimported(endpointid,endpoint)
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
        return createLocalPy4jId(port=self._gateway.get_callback_server().get_listening_port())
    
    def __export(self,svc,props):
        try:
            self._consumer.exportService(svc,self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e
        
    def __unexport(self,props):
        try:
            self._consumer.unexportService(self._convert_props_for_java(props))
        except Exception as e:
            _logger.error(e)
            raise e
            
    def disconnect(self):
        with self._lock:
            if self.isconnected():
                self._gateway.shutdown()
                self._gateway = None
                self._consumer = None   
                self._bridge = None 
        with self._endpoints_lock:
            self._endpoints.clear()
        with self._exported_endpoints_lock:
            self._exported_endpoints.clear()
     
