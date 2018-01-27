/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.BundleContext;

public class BundleModuleResolver implements ModuleResolver {

	public static final String PATH_PREFIX_PROP = "pathPrefix";
	public static final String PACKAGE_SUFFIX = "/";
	public static final String PACKAGE_INIT_NAME = "__init__";
	public static final String MODULE_SUFFIX = ".py";
	public static final String PACKAGE_INIT_FILE = PACKAGE_SUFFIX + PACKAGE_INIT_NAME + MODULE_SUFFIX;

	protected BundleContext context;
	protected String pathPrefix = PACKAGE_SUFFIX;

	protected void activate(BundleContext context, @SuppressWarnings("rawtypes") Map properties) {
		this.context = context;
		String prefix = (String) properties.get(PATH_PREFIX_PROP);
		if (prefix != null) {
			while (prefix.startsWith("/"))
				prefix = prefix.substring(1);
			this.pathPrefix = prefix;
		}
		if (!this.pathPrefix.endsWith(PACKAGE_SUFFIX))
			this.pathPrefix = this.pathPrefix + PACKAGE_SUFFIX;
	}

	protected String getPathPrefix() {
		return this.pathPrefix;
	}
	
	protected void deactivate() {
		this.pathPrefix = null;
		this.context = null;
	}

	protected Enumeration<String> getEntryPaths(String path) {
		return this.context.getBundle().getEntryPaths(path);
	}
	
	protected int getModuleType(String prefix, String pathSegment) {
		Enumeration<String> paths = getEntryPaths(prefix);
		if (paths != null)
			for (; paths.hasMoreElements();) {
				String path = paths.nextElement().substring(prefix.length());
				// check to see that the path starts with
				if (path.startsWith(pathSegment)) {
					if (path.endsWith(PACKAGE_SUFFIX) || path.endsWith(PACKAGE_INIT_FILE))
						// package endswith '/' or '/__init__.py'
						return ModuleResolver.PACKAGE;
					else if (path.endsWith(MODULE_SUFFIX))
						// module
						return ModuleResolver.MODULE;
				}
			}
		return ModuleResolver.NONE;
	}

	@Override
	public int getModuleType(String moduleName) {
		String[] pathSegments = moduleName.split("\\.");
		String prefix = getPathPrefix();
		int pathSegmentIndex = 0;
		while (pathSegmentIndex < (pathSegments.length - 1)) {
			String pathSegment = pathSegments[pathSegmentIndex];
			// all the preceeding segments must be packages
			int moduleType = getModuleType(prefix, pathSegment);
			if (moduleType == ModuleResolver.PACKAGE) {
				// found it, keep going by adding to prefix and moving to next segment
				prefix = prefix + pathSegment + PACKAGE_SUFFIX;
				pathSegmentIndex++;
			} else
				return ModuleResolver.NONE;
		}
		return getModuleType(prefix, pathSegments[pathSegments.length - 1]);
	}

	protected String readFileAsString(URL url) throws Exception {
		InputStream ins = url.openStream();
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = ins.read(buffer)) != -1)
			result.write(buffer, 0, length);
		return result.toString("UTF-8");
	}

	protected URL getEntry(String entryPath) {
		return this.context.getBundle().getEntry(entryPath);
	}
	
	@Override
	public String getModuleCode(String moduleName, boolean ispackage) throws Exception {
		String modulePath = getPathPrefix() + moduleName.replace('.', '/')
				+ (ispackage ? PACKAGE_SUFFIX + PACKAGE_INIT_NAME + MODULE_SUFFIX : MODULE_SUFFIX);
		URL url = getEntry(modulePath);
		if (url != null)
			return readFileAsString(url);
		else if (ispackage)
			return ""; // python 3 packages can have no __init__.py
		else
			throw new FileNotFoundException("Could not find module=" + moduleName);
	}

}
