/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.syncinfo;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.core.subscribers.caches.ResourceVariantByteStore;

/**
 * A CVS resource variant tree that associates a different tag with each root project.
 */
public class MultiTagResourceVariantTree extends CVSResourceVariantTree {

	Map projects = new HashMap();
	
	public MultiTagResourceVariantTree(ResourceVariantByteStore cache, boolean cacheFileContentsHint) {
		super(cache, null, cacheFileContentsHint);
	}
	
	public void addProject(IProject project, CVSTag tag) {
		Assert.isNotNull(project);
		Assert.isNotNull(tag);
		projects.put(project, tag);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.syncinfo.CVSResourceVariantTree#getTag(org.eclipse.core.resources.IResource)
	 */
	protected CVSTag getTag(IResource resource) {
		return (CVSTag)projects.get(resource.getProject());
	}
}
