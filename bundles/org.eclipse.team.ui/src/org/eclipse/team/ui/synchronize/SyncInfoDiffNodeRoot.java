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
package org.eclipse.team.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.team.core.subscribers.SyncInfoSet;
import org.eclipse.team.ui.synchronize.views.SyncInfoDiffNodeBuilder;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SyncInfoDiffNodeRoot extends SyncInfoDiffNode {

	SyncInfoDiffNodeBuilder builder;
	
	/**
	 * @param set
	 * @param resource
	 */
	public SyncInfoDiffNodeRoot(SyncInfoSet set, IResource resource) {
		super(null, set, resource);
		builder = createBuilder();
		builder.buildTree();
	}

	/**
	 * @return
	 */
	protected SyncInfoDiffNodeBuilder createBuilder() {
		return new SyncInfoDiffNodeBuilder(this);
	}

	/**
	 * 
	 */
	public void dispose() {
		builder.dispose();
	}

	/**
	 * @param viewer
	 */
	public void setViewer(AbstractTreeViewer viewer) {
		builder.setViewer(viewer);
	}

}
