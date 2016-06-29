/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.local;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.eclipse.ecf.remoteservice.AbstractRSAContainer;
import org.eclipse.ecf.remoteservice.RSARemoteServiceContainerAdapter.RSARemoteServiceRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class DirectHostContainer extends AbstractRSAContainer
		implements ServiceTrackerCustomizer<DirectRemoteServiceProvider, DirectRemoteServiceProvider> {

	private BundleContext context;
	private ServiceTracker<DirectRemoteServiceProvider, DirectRemoteServiceProvider> tracker;
	private Object lock = new Object();
	private List<RSARemoteServiceRegistration> unexportedRegs = new ArrayList<RSARemoteServiceRegistration>();
	private List<RSARemoteServiceRegistration> exportedRegs = new ArrayList<RSARemoteServiceRegistration>();

	public DirectHostContainer(ID id, BundleContext context) {
		super(id);
		this.context = context;
		tracker = new ServiceTracker<DirectRemoteServiceProvider, DirectRemoteServiceProvider>(context, DirectRemoteServiceProvider.class,
				this);
		tracker.open();
	}

	private DirectRemoteServiceProvider rsc;

	@Override
	protected Map<String, Object> exportRemoteService(RSARemoteServiceRegistration registration) {
		exportRegs(registration);
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Map convertProps(RSARemoteServiceRegistration reg) {
		Map result = new TreeMap();
		for (String key : reg.getPropertyKeys())
			result.put(key, reg.getProperty(key));
		return result;
	}

	@Override
	protected void unexportRemoteService(RSARemoteServiceRegistration registration) {
		unexportRegs(registration);
	}

	void exportReg(RSARemoteServiceRegistration reg) {
		try {
			DirectRemoteServiceProvider r = this.rsc;
			if (r != null) {
				r.registerService(reg.getService(), convertProps(reg));
				exportedRegs.add(reg);
			} else
				unexportedRegs.add(reg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void unexportReg(RSARemoteServiceRegistration reg) {
		try {
			DirectRemoteServiceProvider r = this.rsc;
			if (r != null) {
				r.unregisterService(convertProps(reg));
				exportedRegs.remove(reg);
			} else
				unexportedRegs.remove(reg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void exportRegs(RSARemoteServiceRegistration reg) {
		synchronized (lock) {
			if (reg != null)
				exportReg(reg);
			else
				for (Iterator<RSARemoteServiceRegistration> i = unexportedRegs.iterator(); i.hasNext();) {
					exportReg(i.next());
					i.remove();
				}
		}
	}

	void unexportRegs(RSARemoteServiceRegistration reg) {
		synchronized (lock) {
			if (reg != null)
				unexportReg(reg);
			else
				for (Iterator<RSARemoteServiceRegistration> i = unexportedRegs.iterator(); i.hasNext();) {
					unexportReg(i.next());
					i.remove();
				}
		}
	}

	@Override
	public DirectRemoteServiceProvider addingService(ServiceReference<DirectRemoteServiceProvider> reference) {
		this.rsc = context.getService(reference);
		exportRegs(null);
		return this.rsc;
	}

	@Override
	public void modifiedService(ServiceReference<DirectRemoteServiceProvider> reference, DirectRemoteServiceProvider service) {
	}

	@Override
	public void removedService(ServiceReference<DirectRemoteServiceProvider> reference, DirectRemoteServiceProvider service) {
		unexportRegs(null);
		this.rsc = null;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (tracker != null) {
			tracker.close();
			tracker = null;
			context = null;
			exportedRegs.clear();
			unexportedRegs.clear();
		}
	}
}
