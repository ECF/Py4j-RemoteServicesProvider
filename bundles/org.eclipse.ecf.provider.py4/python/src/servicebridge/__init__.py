
from uuid import UUID, uuid1
from time import time
from datetime import datetime
from threading import Lock
from argparse import ArgumentError

#----------------------------------------------------------------------------
# RSA constants (declared in org.osgi.service.remoteserviceadmin.RemoteConstants
ENDPOINT_ID = "endpoint.id"
ENDPOINT_SERVICE_ID = "endpoint.service.id"
ENDPOINT_FRAMEWORK_UUID = "endpoint.framework.uuid"
ENDPOINT_PACKAGE_VERSION_ = "endpoint.package.version."
SERVICE_EXPORTED_INTERFACES = "service.exported.interfaces"
REMOTE_CONFIGS_SUPPORTED = "remote.configs.supported"
REMOTE_INTENTS_SUPPORTED = "remote.intents.supported"
SERVICE_EXPORTED_CONFIGS = "service.exported.configs"
SERVICE_EXPORTED_INTENTS = "service.exported.intents"
SERVICE_EXPORTED_INTENTS_EXTRA = "service.exported.intents.extra"
SERVICE_IMPORTED = "service.imported"
SERVICE_IMPORTED_CONFIGS = "service.imported.configs"
SERVICE_INTENTS = "service.intents"
SERVICE_ID = "service.id"

ECF_ENDPOINT_CONTAINERID_NAMESPACE = "ecf.endpoint.id.ns"
ECF_ENDPOINT_ID = "ecf.endpoint.id"
ECF_RSVC_ID = "ecf.rsvc.id"
ECF_ENDPOINT_TIMESTAMP = "ecf.endpoint.ts"
ECF_ENDPOINT_CONNECTTARGET_ID = "ecf.endpoint.connecttarget.id"
ECF_ENDPOINT_IDFILTER_IDS = "ecf.endpoint.idfilter.ids"

ECF_ENDPOINT_REMOTESERVICE_FILTER = "ecf.endpoint.rsfilter"
ECF_SERVICE_EXPORTED_CONTAINER_FACTORY_ARGS = "ecf.exported.containerfactoryargs"
ECF_SERVICE_EXPORTED_CONTAINER_CONNECT_CONTEXT = "ecf.exported.containerconnectcontext"
ECF_SERVICE_EXPORTED_CONTAINER_ID = "ecf.exported.containerid"
ECF_SERVICE_EXPORTED_ASYNC_INTERFACES = "ecf.exported.async.interfaces"
ECF_SERVICE_EXPORTED_ASYNC_NOPROXY = "ecf.rsvc.async.noproxy"
ECF_SERVICE_ASYNC_RSPROXY_CLASS_ = "ecf.rsvc.async.proxy_"
ECF_ASYNC_INTERFACE_SUFFIX = "Async"
ECF_SERVICE_IMPORTED_VALUETYPE = "ecf.service.imported.valuetype"
ECF_SERVICE_IMPORTED_ENDPOINT_ID = ENDPOINT_ID
ECF_SERVICE_IMPORTED_ENDPOINT_SERVICE_ID = ENDPOINT_SERVICE_ID
ECF_OSGI_ENDPOINT_MODIFIED = "ecf.osgi.endpoint.modified"
ECF_OSGI_CONTAINER_ID_NS = "ecf.osgi.ns"
#----------------------------------------------------------------------------------

def create_uuid():
    return str(uuid1())

def time_since_epoch():
    return int(time() * 1000)

_next_rsid = 1
_next_rsid_lock = Lock()

def get_next_rsid():
    with _next_rsid_lock:
        global _next_rsid
        n = _next_rsid
        _next_rsid += 1
        return n

def merge_dicts(*dict_args):
    '''
    Given any number of dicts, shallow copy and merge into a new dict,
    precedence goes to key value pairs in latter dicts.
    '''
    result = {}
    for dictionary in dict_args:
        result.update(dictionary)
    return result

def get_rsa_props(object_class, exported_cfgs, intents=None, ep_svc_id=None, fw_id=None, pkg_ver=None):
    results = {}
    if not object_class:
        raise ArgumentError('object_class must be an [] of Strings')
    results['objectClass'] = object_class
    if not exported_cfgs:
        raise ArgumentError('rmt_configs must be an array of Strings')
    results[SERVICE_EXPORTED_CONFIGS] = exported_cfgs
    results[SERVICE_IMPORTED_CONFIGS] = exported_cfgs
    if intents:
        results[SERVICE_INTENTS] = intents
    if not ep_svc_id:
        ep_svc_id = get_next_rsid()
    results[ENDPOINT_SERVICE_ID] = ep_svc_id
    results[SERVICE_ID] = ep_svc_id
    if not fw_id:
        fw_id = create_uuid()
    results[ENDPOINT_FRAMEWORK_UUID] = fw_id
    if pkg_ver:
        results[ENDPOINT_PACKAGE_VERSION_+pkg_ver[0]] = pkg_ver[1]
    results[ENDPOINT_ID] = create_uuid()
    results[SERVICE_IMPORTED] = 'true'
    results[SERVICE_EXPORTED_INTERFACES] = '*'
    return results

def get_ecf_props(ep_id, ep_id_ns, rsvc_id=None, ep_ts=None):
    results = {}
    if not ep_id:
        raise ArgumentError('ep_id must be a valid endpoint id')
    results[ECF_ENDPOINT_ID] = ep_id
    if not ep_id_ns:
        raise ArgumentError('ep_id_ns must be a valid namespace')
    results[ECF_ENDPOINT_CONTAINERID_NAMESPACE] = ep_id_ns
    if not rsvc_id:
        rsvc_id = get_next_rsid()
    results[ECF_RSVC_ID] = rsvc_id
    if not ep_ts:
        ep_ts = time_since_epoch()
    results[ECF_ENDPOINT_TIMESTAMP] = ep_ts
    results[ECF_SERVICE_EXPORTED_ASYNC_INTERFACES] = '*'
    return results


