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
package org.eclipse.team.ui.sync;

import org.eclipse.ui.IWorkbenchPage;

/**
 * Manages synchronization pages.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * @since 3.0 
 */
public interface ISynchronizeManager {
	
	/**
	 * Registers the given listener for console notifications. Has
	 * no effect if an identical listener is already registered.
	 * 
	 * @param listener listener to register
	 */
	public void addSynchronizePageListener(ISynchronizePageListener listener);
	
	/**
	 * Deregisters the given listener for console notifications. Has
	 * no effect if an identical listener is not already registered.
	 * 
	 * @param listener listener to deregister
	 */
	public void removeSynchronizePageListener(ISynchronizePageListener listener);

	/**
	 * Adds the given consoles to the console manager. Has no effect for
	 * equivalent consoles already registered. The consoles will be added
	 * to any existing console views.
	 * 
	 * @param consoles consoles to add
	 */
	public void addSynchronizePages(ISynchronizeViewPage[] consoles);
	
	/**
	 * Removes the given consoles from the console manager. If the consoles are
	 * being displayed in any console views, the associated pages will be closed.
	 * 
	 * @param consoles consoles to remove
	 */
	public void removeSynchronizePages(ISynchronizeViewPage[] consoles);
	
	/**
	 * Returns a collection of consoles registered with the console manager.
	 * 
	 * @return a collection of consoles registered with the console manager
	 */
	public ISynchronizeViewPage[] getSynchronizePages();
	
	public INewSynchronizeView showSynchronizeViewInActivePage(IWorkbenchPage page);
}
