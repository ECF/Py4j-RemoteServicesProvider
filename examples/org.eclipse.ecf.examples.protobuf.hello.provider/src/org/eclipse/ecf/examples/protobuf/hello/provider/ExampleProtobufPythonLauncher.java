package org.eclipse.ecf.examples.protobuf.hello.provider;

import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.python.PythonLaunchCommandProvider;
import org.eclipse.ecf.python.PythonLauncher;
import org.eclipse.ecf.python.protobuf.ProtobufPythonLauncher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(immediate=true)
public class ExampleProtobufPythonLauncher extends ProtobufPythonLauncher implements PythonLauncher {

	@Override
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
	protected void bindLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		super.bindLaunchCommandProvider(provider);
	}
	
	protected void unbindPythonLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		super.unbindPythonLaunchCommandProvider(provider);
	}
	
	@Reference
	protected void bindPy4jProvider(Py4jProvider provider) {
		super.bindPy4jProvider(provider);
	}
	
	protected void unbindPy4jProvider(Py4jProvider provider) {
		super.unbindPy4jProvider(provider);
	}
	
	@Activate
	protected void activate(BundleContext context) throws Exception {
		super.activate(context);
		launch(null,null);
	}
	
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
}
