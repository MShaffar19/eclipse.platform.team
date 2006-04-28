/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.ArrayList;
import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;

/**
 * This class can be used to fetch and cache file contents for remote files.
 */
public class UpdateContentCachingService {

	private CVSRepositoryLocation repository;
	private ICVSFolder remoteRoot;
	private final CVSTag tag;
	private final int depth;

	public class SandboxUpdate extends Update {
		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ccvs.core.client.Command#commandFinished(org.eclipse.team.internal.ccvs.core.client.Session, org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption[], org.eclipse.team.internal.ccvs.core.client.Command.LocalOption[], org.eclipse.team.internal.ccvs.core.ICVSResource[], org.eclipse.core.runtime.IProgressMonitor, org.eclipse.core.runtime.IStatus)
		 */
		protected IStatus commandFinished(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor, IStatus status) throws CVSException {
			// Don't do anything (i.e. don't prune)
			return status;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ccvs.core.client.Command#doExecute(org.eclipse.team.internal.ccvs.core.client.Session, org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption[], org.eclipse.team.internal.ccvs.core.client.Command.LocalOption[], java.lang.String[], org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener, org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus doExecute(Session session, GlobalOption[] globalOptions, LocalOption[] localOptions, String[] arguments, ICommandOutputListener listener, IProgressMonitor monitor) throws CVSException {
			session.registerResponseHandler(new SandboxUpdatedHandler(UpdatedHandler.HANDLE_CREATED));
			session.registerResponseHandler(new SandboxUpdatedHandler(UpdatedHandler.HANDLE_MERGED));
			session.registerResponseHandler(new SandboxUpdatedHandler(UpdatedHandler.HANDLE_UPDATE_EXISTING));
			session.registerResponseHandler(new SandboxUpdatedHandler(UpdatedHandler.HANDLE_UPDATED));
			return super.doExecute(session, globalOptions, localOptions, arguments, listener, monitor);
		}
	}
	
	/**
	 * This class overrides the "Created" handler in order to configure the remote file
	 * to recieve and cache the contents
	 */
	public class SandboxUpdatedHandler extends UpdatedHandler {
		public SandboxUpdatedHandler(int type) {
			super(type);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.team.internal.ccvs.core.client.UpdatedHandler#receiveTargetFile(org.eclipse.team.internal.ccvs.core.client.Session, org.eclipse.team.internal.ccvs.core.ICVSFile, java.lang.String, java.util.Date, boolean, boolean, org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected void receiveTargetFile(
			Session session,
			ICVSFile mFile,
			String entryLine,
			Date modTime,
			boolean binary,
			boolean readOnly,
			boolean executable,
			IProgressMonitor monitor)
			throws CVSException {
			
			// Set the sync info first so that the contents are cached properly
			ResourceSyncInfo info = new ResourceSyncInfo(entryLine, modTime);
			// We're always excepting new revisions so the file is clean
			mFile.setSyncInfo(info, ICVSFile.CLEAN);
			
			// receive the file contents from the server
			session.receiveFile(mFile, binary, getHandlerType(), monitor);
			
			// Handle execute
			try {
				if (executable) mFile.setExecutable(true);
	        } catch (CVSException e) {
	            // Just log and keep going
	            CVSProviderPlugin.log(e);
	        }
		}
	}
	
	public static RemoteFolder buildRemoteTree(final CVSRepositoryLocation repository, ICVSFolder root, CVSTag tag, int depth, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, 100);
		try {
			RemoteFolder tree = buildBaseTree(repository, root, tag, Policy.subMonitorFor(monitor, 50));
			UpdateContentCachingService service = new UpdateContentCachingService(repository, tree, tag, depth);
			if (!service.cacheFileContents(Policy.subMonitorFor(monitor, 50)))
				return null;
			return tree;
		} finally {
			monitor.done();
		}
	}
	
	private static RemoteFolder buildBaseTree(final CVSRepositoryLocation repository, ICVSFolder root, CVSTag tag, IProgressMonitor progress) throws CVSException {
		try {
			RemoteFolderTreeBuilder builder = new RemoteFolderTreeBuilder(repository, root, tag) {
				protected RemoteFolder createRemoteFolder(ICVSFolder local, RemoteFolder parent, FolderSyncInfo folderSyncInfo) {
					return new RemoteFolderSandbox(parent, local.getName(), repository, folderSyncInfo.getRepository(), folderSyncInfo.getTag(), folderSyncInfo.getIsStatic());
				}
				protected RemoteFile createRemoteFile(RemoteFolder remote, byte[] syncBytes) throws CVSException {
					return new RemoteFile(remote, syncBytes){
						public boolean isModified(IProgressMonitor monitor) throws CVSException {
							return false;
						}	
						public void delete() {
							RemoteFolderSandbox parent = (RemoteFolderSandbox)getParent();
							parent.remove(this);
						}
					};
				}
			};
			progress.beginTask(null, 100);
			IProgressMonitor subProgress = Policy.infiniteSubMonitorFor(progress, 100);
			subProgress.beginTask(null, 512);  
			subProgress.subTask(NLS.bind(CVSMessages.RemoteFolderTreeBuilder_buildingBase, new String[] { root.getName() })); 
	 		return builder.buildBaseTree(null, root, subProgress);
		} finally {
			progress.done();
		}
	}
	
	public static RemoteFile buildRemoteTree(CVSRepositoryLocation repository, ICVSFile file, CVSTag tag, IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, 100);
		try {
			RemoteFolderTreeBuilder builder = new RemoteFolderTreeBuilder(repository, file.getParent(), tag);
			RemoteFile remote =  builder.buildTree(file, Policy.subMonitorFor(monitor, 10));
			byte[] syncBytes = remote.getSyncBytes();
			if (builder.getFileDiffs().length > 0) {
				// Getting the storage of the file will cache the contents
				remote.getStorage(Policy.subMonitorFor(monitor, 90));
			}
			// We need to set the sync bytes back because the content fetch
			// makes the handle sticky
			remote.setSyncBytes(syncBytes, ICVSFile.CLEAN);
			return remote;
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}
	
	public UpdateContentCachingService(CVSRepositoryLocation repository, RemoteFolder tree, CVSTag tag, int depth) {
		this.repository = repository;
		this.remoteRoot = tree;
		this.tag = tag;
		this.depth = depth;
	}
	
	private boolean cacheFileContents(IProgressMonitor monitor) throws CVSException {
		// Fetch the file contents for all out-of-sync files by running an update
		// on the remote tree passing the known changed files as arguments
		monitor.beginTask(null, 100);
		Policy.checkCanceled(monitor);
		Session session = new Session(repository, remoteRoot, false);
		session.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
		try {
			Policy.checkCanceled(monitor);
			IStatus status = new SandboxUpdate().execute(session,
				Command.NO_GLOBAL_OPTIONS,
				getLocalOptions(),
				new String[] { Session.CURRENT_LOCAL_FOLDER },
				null,
				Policy.subMonitorFor(monitor, 90));
			if (!status.isOK()) {
				if (status.getCode() == CVSStatus.SERVER_ERROR) {
					CVSServerException e = new CVSServerException(status);
					if ( ! e.isNoTagException() && e.containsErrors())
						throw e;
					return false;
				} else if (status.getSeverity() == IStatus.ERROR)
					throw new CVSException(status);
				else 
					CVSProviderPlugin.log (new CVSException(status));
			}
		} finally {
			session.close();
			monitor.done();
		}
		return true;
	}

	private LocalOption[] getLocalOptions() {
		ArrayList options = new ArrayList();
		if (tag != null)
			options.add(Update.makeTagOption(tag));
		
		if (depth != IResource.DEPTH_INFINITE )
			options.add(Command.DO_NOT_RECURSE);
		
		if (!options.isEmpty())
			return (LocalOption[]) options.toArray(new LocalOption[options.size()]);
		
		return Command.NO_LOCAL_OPTIONS;
	}
}