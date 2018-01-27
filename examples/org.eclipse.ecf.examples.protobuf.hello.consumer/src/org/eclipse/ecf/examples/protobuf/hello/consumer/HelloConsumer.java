package org.eclipse.ecf.examples.protobuf.hello.consumer;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.eclipse.ecf.python.protobuf.Exporter.ExportRequest;
import org.eclipse.ecf.python.protobuf.PythonServiceExporterAsync;
import org.eclipse.ecf.examples.protobuf.hello.IHello;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(immediate = true)
public class HelloConsumer {

	private PythonServiceExporterAsync exporter;
	
	@Reference(policy=ReferencePolicy.DYNAMIC,target="(service.imported=*)")
	void bindPythonServiceExporter(PythonServiceExporterAsync exporter) {
		this.exporter = exporter;
	}
	
	void unbindPythonServiceExporter(PythonServiceExporterAsync exporter) {
		this.exporter = null;
	}
	
	private IHello helloService;
	
	@Reference(policy=ReferencePolicy.DYNAMIC,cardinality=ReferenceCardinality.OPTIONAL,target="(service.imported=*)")
	void bindHello(IHello hello) {
		this.helloService = hello;
		try {
			HelloMsgContent result = this.helloService.sayHello(createRequest());
			System.out.println("Java received sayHello result="+result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void unbindHello(IHello hello) {
		this.helloService = null;
	}
	
	static HelloMsgContent createRequest() {
		HelloMsgContent.Builder b1 = HelloMsgContent.newBuilder();
		b1.addX(1.1);
		b1.addX(1.2);
		b1.setF("java");
		b1.setTo("python");
		b1.setHellomsg("Hello from java");
		b1.setH("some other message");
		return b1.build();
	}
	
	@Activate
	void activate() {
		// Using the exporter, ask the python side to create an export an instance of python HelloServiceImpl
		ExportRequest.Builder b = ExportRequest.newBuilder();
		b.setClassName("HelloServiceImpl");
		Map<String,String> creationArgs = new HashMap<String,String>();
		creationArgs.put("one", "1");
		creationArgs.put("two", "2");
		Map<String,String> o = new HashMap<String,String>();
		o.put("three", "3");
		o.put("four", "4");
		b.putAllCreationArgs(creationArgs);
		b.putAllOverridingExportProps(o);
		this.exporter.createAndExportAsync(b.build()).whenComplete((r,e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("response="+r);
		});
	}

}
