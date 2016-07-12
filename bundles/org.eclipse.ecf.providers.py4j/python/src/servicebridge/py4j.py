'''
Created on Jul 11, 2016

@author: slewis
'''
from logging import getLogger as getLibLogger
from threading import RLock

from py4j.java_gateway import JavaGateway
from py4j.java_gateway import CallbackServerParameters
from py4j.java_collections import ListConverter, MapConverter
from servicebridge import _RSA_ENDPOINT_ID, _JAVA_DIRECT_ENDPOINT_CLASS

# Version
__version_info__ = (0, 1, 0)
__version__ = ".".join(str(x) for x in __version_info__)

# Documentation strings format
__docformat__ = "restructuredtext en"

# ------------------------------------------------------------------------------

_logger = getLibLogger(__name__)

# ------------------------------------------------------------------------------

class Py4jBridgeEventListener(object):
    def service_exported(self,endpointid,endpoint):
        pass
    
    def service_modified(self,endpointid,endpoint):
        pass
    
    def service_unexported(self,endpointid,endpoint):
        pass
    
    
class Py4jBridge(object):
    
    def __init__(self,listener=None):
        self._gateway = None
        self._lock = RLock()
        self._consumer = None
        self._endpoints_lock = RLock()
        self._endpoints = {}
        self._map_converter = MapConverter()
        self._list_converter = ListConverter()
        self._listener = listener
        
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
            if not self.isconnected():
                raise ConnectionError('Not connected to java gateway')
            result = {}
            for item in props.items():
                val = item[1]
                if isinstance(val, type[[]]):
                    val = self._list_converter.convert(val,self._gateway._gateway_client)
            result[item[0]] = val
            return self._map_converter.convert(props,self._gateway._gateway_client)

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
            self._gateway.entry_point.setPythonConsumer(self)
    
    def disconnect(self):
        with self._lock:
            if self.isconnected():
                self._gateway.shutdown()
                self._gateway = None
                self._consumer = None    
        with self._endpoints_lock:
            self._endpoints.clear()
     
    class Java:
        implements = [_JAVA_DIRECT_ENDPOINT_CLASS]

    def exportService(self,proxy,props):
        print('exportService')
        with self._endpoints_lock:
            try:
                endpointid = props[_RSA_ENDPOINT_ID]
                print('adding endpointid='+endpointid+",proxy="+proxy.toString()+",props="+props.toString())
                endpoint = (proxy,props)
                self._endpoints[endpointid] = endpoint
                if not self._listener is None:
                    try:
                        self._listener.service_exported(endpointid,endpoint)
                    except:
                        # log?
                        pass
            except KeyError:
                pass
            

    def modifyService(self,props):
        print('modifyService='+props.toString())
        with self._endpoints_lock:
            try:
                endpointid = props[_RSA_ENDPOINT_ID]
                endpoint = self._endpoints[endpointid]
                endpoint[1].update(props)
                if not self._listener is None:
                    try:
                        self._listener.service_modified(endpointid,endpoint)
                    except:
                        # log?
                        pass
            except KeyError:
                pass
        
    def unexportService(self,props):
        print('unexportService props='+props.toString())
        with self._endpoints_lock:
            try:
                endpointid = props[_RSA_ENDPOINT_ID]
                print('removing endpointid='+endpointid)
                endpoint = self._endpoints.pop(endpointid)
                if not self._listener is None:
                    try:
                        self._listener.service_removed(endpointid,endpoint)
                    except:
                        # log?
                        pass
            except KeyError:
                pass
        

