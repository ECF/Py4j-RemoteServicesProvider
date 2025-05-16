/*******************************************************************************
 * Copyright (c) 2025 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python.mcp.tools.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class ToolAnnotationUtil {

	public static class ToolParamDescription {
		private Parameter param;
		private ToolParam paramAnnotation;

		public ToolParamDescription() {
			this(null, null);
		}

		public ToolParamDescription(Parameter param, ToolParam paramAnnotation) {
			this.param = param;
			this.paramAnnotation = paramAnnotation;
		}

		public Parameter getParameter() {
			return this.param;
		}

		public ToolParam getToolParamAnnotation() {
			return this.paramAnnotation;
		}

		@Override
		public String toString() {
			return "ToolParamDescription[param=" + param.getName() + ",description=" + paramAnnotation.description()
					+ "]";
		}

	}

	public static class ToolDescription {
		private final Method method;
		private final Tool annotation;
		private final ToolParamDescription[] paramDescriptions;

		public ToolDescription(Method method, Tool annotation) {
			this.method = method;
			this.annotation = annotation;
			this.paramDescriptions = Arrays.asList(method.getParameters()).stream().map(p -> {
				ToolParam tp = p.getAnnotation(ToolParam.class);
				return (tp == null) ? new ToolParamDescription() : new ToolParamDescription(p, tp);
			}).collect(Collectors.toList()).toArray(new ToolParamDescription[0]);
		}

		public String name() {
			String res = getAnnotation().name();
			if (res.isEmpty()) {
				res = getMethod().getName();
			}
			return res;
		}

		public Method getMethod() {
			return this.method;
		}

		public Tool getAnnotation() {
			return this.annotation;
		}

		public ToolParamDescription[] getToolParamDescriptions() {
			return this.paramDescriptions;
		}

		@Override
		public String toString() {
			return "ToolDescription[method=" + method.getName() + ",name=" + annotation.name() + ",description="
					+ annotation.description() + ", paramDescriptions=" + Arrays.toString(paramDescriptions) + "]";
		}

	}

	public static ToolDescription[] getToolDescriptions(Class<?> clazz) {
		return Arrays.asList(clazz.getMethods()).stream().map(m -> {
			Tool ma = m.getAnnotation(Tool.class);
			return (ma != null) ? new ToolDescription(m, ma) : null;
		}).filter(Objects::nonNull).collect(Collectors.toList()).toArray(new ToolDescription[0]);
	}

}
