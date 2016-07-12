'''
Created on Jul 11, 2016

@author: slewis
'''
from logging import getLogger as getLibLogger
from threading import RLock


from py4j.java_gateway import JavaGateway, CallbackServerParameters
from py4j.java_collections import ListConverter, MapConverter
from servicebridge import merge_dicts, ENDPOINT_ID

PY4J_EXPORTED_CONFIGS = ['ecf.py4j.host.python']
PY4J_PROTOCOL = 'py4j'
PY4J_DEFAULT_GATEWAY_PORT = 25333
PY4J_DEFAULT_CB_PORT = 25334
PY4J_DEFAULT_HOSTNAME = 'localhost'
PY4J_PYTHON_PATH = "/python"
PY4J_JAVA_PATH = "/java"

PY4J_NAMESPACE = 'ecf.namespace.py4j'
JAVA_DIRECT_ENDPOINT_CLASS = 'org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider'
PY4J_DEFAULT_REMOTE_CONFIGS = ['passByReference', 'exactlyOnce', 'ordered']

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
        self._map_converter = MapConverter()
        self._list_converter = ListConverter()
        self._listener = listener
    
    def _import_service_from_java(self,proxy,props):
        with self._endpoints_lock:
            try:
                endpointid = props[ENDPOINT_ID]
                endpoint = (proxy,props)
                self._endpoints[endpointid] = endpoint
                if not self._listener is None:
                    try:
                        self._listener.service_imported(endpointid,endpoint)
                    except:
                        _logger.error('_import_service_from_java listener threw exception endpointid='+endpointid)
            except KeyError:
                pass
    def _modify_service_from_java(self,props):
        with self._endpoints_lock:
            try:
                endpointid = props[ENDPOINT_ID]
                endpoint = self._endpoints[endpointid]
                endpoint[1].update(props)
                if not self._listener is None:
                    try:
                        self._listener.service_modified(endpointid,endpoint)
                    except:
                        _logger.error('_modify_service_from_java listener threw exception endpointid='+endpointid)
            except KeyError:
                pass
    
    def _unimport_service_from_java(self,props):
        with self._endpoints_lock:
            try:
                endpointid = props[ENDPOINT_ID]
                endpoint = self._endpoints.pop(endpointid)
                if not self._listener is None:
                    try:
                        self._listener.service_removed(endpointid,endpoint)
                    except:
                        _logger.error('_unimport_service_from_java listener threw exception endpointid='+endpointid)
                        pass
            except KeyError:
                pass

    def get_jvm(self):
        with self._lock:
            self._raise_not_connected();
            return self._gateway.jvm
        
    def get_endpoint(self,endpointid):
        with self._endpoints_lock:
            try:
                return self._endpoints[endpointid]
            except KeyError:
                return None
        
    def get_endpoints(self):
        with self._endpoints_lock:
            return self._endpoints.copy()
    
    def convert_props_for_java(self,props):
        with self._lock:
            self._raise_not_connected()
            result = {}
            for item in props.items():
                val = item[1]
                if isinstance(val, type([])):
                    val = self._list_converter.convert(val,self._gateway._gateway_client)
                result[item[0]] = val
            return self._map_converter.convert(result,self._gateway._gateway_client)

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
    
    def _raise_not_connected(self):
        if not self.isconnected():
            raise ConnectionError('Not connected to java gateway')

    def get_access(self):
        return createLocalPy4jId(port=self._gateway.get_callback_server().get_listening_port())
    
    def export_to_java(self,svc,export_props):
        with self._lock:
            self._raise_not_connected()
            props = self.convert_props_for_java(export_props)
            self._consumer.exportService(svc,props)

    def unexport_to_java(self,props):
        with self._lock:
            self._raise_not_connected()
        self._consumer.unexportService(self.convert_props_for_java(props))

    def disconnect(self):
        with self._lock:
            if self.isconnected():
                self._gateway.shutdown()
                self._gateway = None
                self._consumer = None   
                self._bridge = None 
        with self._endpoints_lock:
            self._endpoints.clear()
     
