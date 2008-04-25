/*******************************************************************************
 * Copyright (c) 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net.proxy.win32.winhttp;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StaticProxyConfig wraps certain information of WinHttpCurrentIEProxyConfig,
 * i.e. the Windows specific list of proxies and the proxy bypass list.
 */
public class StaticProxyConfig {

	private List universalProxies = new ArrayList();
	private Map protocolSpecificProxies = new HashMap();
	private ProxyBypass proxyBypass;

	/**
	 * @param proxiesString
	 * @param proxyBypassString
	 */
	public StaticProxyConfig(String proxiesString, String proxyBypassString) {
		ProxyProviderUtil.fillProxyLists(proxiesString, universalProxies,
				protocolSpecificProxies);
		proxyBypass = new ProxyBypass(proxyBypassString);
	}

	/**
	 * Select the static proxies for the given uri and add it to the given list
	 * of proxies.<br>
	 * This respects also the proxy bypass definition.
	 * 
	 * @param uri
	 * @param proxies
	 */
	public void select(URI uri, List proxies) {
		if (proxyBypass.bypassProxyFor(uri))
			return;

		if (!protocolSpecificProxies.isEmpty()) {
			List protocolProxies = (List) protocolSpecificProxies.get(uri
					.getScheme().toUpperCase());
			if (protocolProxies == null)
				return;
			proxies.addAll(protocolProxies);
		} else
			proxies.addAll(universalProxies);
	}

}