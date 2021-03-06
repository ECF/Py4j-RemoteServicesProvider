# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: exporter.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='exporter.proto',
  package='org.eclipse.ecf.python.protobuf',
  syntax='proto3',
  serialized_pb=_b('\n\x0e\x65xporter.proto\x12\x1forg.eclipse.ecf.python.protobuf\"\xf0\x02\n\rExportRequest\x12\x13\n\x0bmodule_name\x18\x01 \x01(\t\x12\x12\n\nclass_name\x18\x02 \x01(\t\x12W\n\rcreation_args\x18\x03 \x03(\x0b\x32@.org.eclipse.ecf.python.protobuf.ExportRequest.CreationArgsEntry\x12j\n\x17overriding_export_props\x18\x04 \x03(\x0b\x32I.org.eclipse.ecf.python.protobuf.ExportRequest.OverridingExportPropsEntry\x1a\x33\n\x11\x43reationArgsEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\r\n\x05value\x18\x02 \x01(\t:\x02\x38\x01\x1a<\n\x1aOverridingExportPropsEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\r\n\x05value\x18\x02 \x01(\t:\x02\x38\x01\"<\n\x0e\x45xportResponse\x12\x13\n\x0b\x65ndpoint_id\x18\x01 \x01(\t\x12\x15\n\rerror_message\x18\x02 \x01(\t\"&\n\x0fUnexportRequest\x12\x13\n\x0b\x65ndpoint_id\x18\x01 \x01(\t\"I\n\x10UnexportResponse\x12\x13\n\x0b\x65ndpoint_id\x18\x01 \x01(\t\x12\x0f\n\x07success\x18\x02 \x01(\x08\x12\x0f\n\x07message\x18\x03 \x01(\tb\x06proto3')
)




_EXPORTREQUEST_CREATIONARGSENTRY = _descriptor.Descriptor(
  name='CreationArgsEntry',
  full_name='org.eclipse.ecf.python.protobuf.ExportRequest.CreationArgsEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.CreationArgsEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='value', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.CreationArgsEntry.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=_descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001')),
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=307,
  serialized_end=358,
)

_EXPORTREQUEST_OVERRIDINGEXPORTPROPSENTRY = _descriptor.Descriptor(
  name='OverridingExportPropsEntry',
  full_name='org.eclipse.ecf.python.protobuf.ExportRequest.OverridingExportPropsEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.OverridingExportPropsEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='value', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.OverridingExportPropsEntry.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=_descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001')),
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=360,
  serialized_end=420,
)

_EXPORTREQUEST = _descriptor.Descriptor(
  name='ExportRequest',
  full_name='org.eclipse.ecf.python.protobuf.ExportRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='module_name', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.module_name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='class_name', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.class_name', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='creation_args', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.creation_args', index=2,
      number=3, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='overriding_export_props', full_name='org.eclipse.ecf.python.protobuf.ExportRequest.overriding_export_props', index=3,
      number=4, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[_EXPORTREQUEST_CREATIONARGSENTRY, _EXPORTREQUEST_OVERRIDINGEXPORTPROPSENTRY, ],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=52,
  serialized_end=420,
)


_EXPORTRESPONSE = _descriptor.Descriptor(
  name='ExportResponse',
  full_name='org.eclipse.ecf.python.protobuf.ExportResponse',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='endpoint_id', full_name='org.eclipse.ecf.python.protobuf.ExportResponse.endpoint_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='error_message', full_name='org.eclipse.ecf.python.protobuf.ExportResponse.error_message', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=422,
  serialized_end=482,
)


_UNEXPORTREQUEST = _descriptor.Descriptor(
  name='UnexportRequest',
  full_name='org.eclipse.ecf.python.protobuf.UnexportRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='endpoint_id', full_name='org.eclipse.ecf.python.protobuf.UnexportRequest.endpoint_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=484,
  serialized_end=522,
)


_UNEXPORTRESPONSE = _descriptor.Descriptor(
  name='UnexportResponse',
  full_name='org.eclipse.ecf.python.protobuf.UnexportResponse',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='endpoint_id', full_name='org.eclipse.ecf.python.protobuf.UnexportResponse.endpoint_id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='success', full_name='org.eclipse.ecf.python.protobuf.UnexportResponse.success', index=1,
      number=2, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='message', full_name='org.eclipse.ecf.python.protobuf.UnexportResponse.message', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=524,
  serialized_end=597,
)

_EXPORTREQUEST_CREATIONARGSENTRY.containing_type = _EXPORTREQUEST
_EXPORTREQUEST_OVERRIDINGEXPORTPROPSENTRY.containing_type = _EXPORTREQUEST
_EXPORTREQUEST.fields_by_name['creation_args'].message_type = _EXPORTREQUEST_CREATIONARGSENTRY
_EXPORTREQUEST.fields_by_name['overriding_export_props'].message_type = _EXPORTREQUEST_OVERRIDINGEXPORTPROPSENTRY
DESCRIPTOR.message_types_by_name['ExportRequest'] = _EXPORTREQUEST
DESCRIPTOR.message_types_by_name['ExportResponse'] = _EXPORTRESPONSE
DESCRIPTOR.message_types_by_name['UnexportRequest'] = _UNEXPORTREQUEST
DESCRIPTOR.message_types_by_name['UnexportResponse'] = _UNEXPORTRESPONSE
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

ExportRequest = _reflection.GeneratedProtocolMessageType('ExportRequest', (_message.Message,), dict(

  CreationArgsEntry = _reflection.GeneratedProtocolMessageType('CreationArgsEntry', (_message.Message,), dict(
    DESCRIPTOR = _EXPORTREQUEST_CREATIONARGSENTRY,
    __module__ = 'exporter_pb2'
    # @@protoc_insertion_point(class_scope:org.eclipse.ecf.python.protobuf.ExportRequest.CreationArgsEntry)
    ))
  ,

  OverridingExportPropsEntry = _reflection.GeneratedProtocolMessageType('OverridingExportPropsEntry', (_message.Message,), dict(
    DESCRIPTOR = _EXPORTREQUEST_OVERRIDINGEXPORTPROPSENTRY,
    __module__ = 'exporter_pb2'
    # @@protoc_insertion_point(class_scope:org.eclipse.ecf.python.protobuf.ExportRequest.OverridingExportPropsEntry)
    ))
  ,
  DESCRIPTOR = _EXPORTREQUEST,
  __module__ = 'exporter_pb2'
  # @@protoc_insertion_point(class_scope:org.eclipse.ecf.python.protobuf.ExportRequest)
  ))
_sym_db.RegisterMessage(ExportRequest)
_sym_db.RegisterMessage(ExportRequest.CreationArgsEntry)
_sym_db.RegisterMessage(ExportRequest.OverridingExportPropsEntry)

ExportResponse = _reflection.GeneratedProtocolMessageType('ExportResponse', (_message.Message,), dict(
  DESCRIPTOR = _EXPORTRESPONSE,
  __module__ = 'exporter_pb2'
  # @@protoc_insertion_point(class_scope:org.eclipse.ecf.python.protobuf.ExportResponse)
  ))
_sym_db.RegisterMessage(ExportResponse)

UnexportRequest = _reflection.GeneratedProtocolMessageType('UnexportRequest', (_message.Message,), dict(
  DESCRIPTOR = _UNEXPORTREQUEST,
  __module__ = 'exporter_pb2'
  # @@protoc_insertion_point(class_scope:org.eclipse.ecf.python.protobuf.UnexportRequest)
  ))
_sym_db.RegisterMessage(UnexportRequest)

UnexportResponse = _reflection.GeneratedProtocolMessageType('UnexportResponse', (_message.Message,), dict(
  DESCRIPTOR = _UNEXPORTRESPONSE,
  __module__ = 'exporter_pb2'
  # @@protoc_insertion_point(class_scope:org.eclipse.ecf.python.protobuf.UnexportResponse)
  ))
_sym_db.RegisterMessage(UnexportResponse)


_EXPORTREQUEST_CREATIONARGSENTRY.has_options = True
_EXPORTREQUEST_CREATIONARGSENTRY._options = _descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001'))
_EXPORTREQUEST_OVERRIDINGEXPORTPROPSENTRY.has_options = True
_EXPORTREQUEST_OVERRIDINGEXPORTPROPSENTRY._options = _descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('8\001'))
# @@protoc_insertion_point(module_scope)
