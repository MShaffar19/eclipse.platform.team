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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;


/**
 * This class is the abstract superclass for CVS operations. It provides
 * error handling, prompting and other UI.
 */
public abstract class CVSOperation implements IRunnableWithProgress {

	private int statusCount;

	private boolean involvesMultipleResources = false;

	private List errors = new ArrayList(); // of IStatus

	protected static final IStatus OK = new CVSStatus(IStatus.OK, Policy.bind("ok")); //$NON-NLS-1$
	
	private IRunnableContext runnableContext;
	private Shell shell;
	private boolean interruptable = true;
	private boolean modifiesWorkspace = true;
	
	// instance variable used to indicate behavior while prompting for overwrite
	private boolean confirmOverwrite = true;
	
	// instance variable used to indicate that the operation is running in the background
	private boolean runningAsJob = false;
	
	public static void run(Shell shell, CVSOperation operation) throws CVSException, InterruptedException {
		operation.setShell(shell);
		operation.setRunnableContext(new ProgressMonitorDialog(shell));
		operation.run();
	}
	
	/**
	 * @param shell
	 */
	public CVSOperation(Shell shell) {
		this.shell = shell;
	}
	
	/**
	 * Execute the operation in the given runnable context. If null is passed, 
	 * the runnable context assigned to the operation is used.
	 * 
	 * @throws InterruptedException
	 * @throws CVSException
	 */
	public void runInContext(IRunnableContext aRunnableContext) throws InterruptedException, CVSException {
		if (aRunnableContext == null) {
			aRunnableContext = getRunnableContext();
		}
		try {
			aRunnableContext.run(isInterruptable(), isInterruptable(), this);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (OperationCanceledException e) {
			throw new InterruptedException();
		}
	}
	
	protected void runAsJob() {
		Job job;
		if (isModifiesWorkspace()) {
			job = getWorkspaceJob();
		} else {
			job = getBasicJob();
		}
		runningAsJob = true;
		job.schedule();
	}
	
	protected IStatus runInJob(IProgressMonitor monitor) {
		try {
			// Don't wrap inside the run since the WorkspaceJob will do the batching
			CVSOperation.this.run(monitor, false /* wrap in ModifyOperation*/);
			return Status.OK_STATUS;
		} catch (InvocationTargetException e) {
			return CVSException.wrapException(e).getStatus();
		} catch (InterruptedException e) {
			return Status.CANCEL_STATUS;
		}
	}
	
	protected Job getBasicJob() {
		return new Job(Policy.bind("CVSOperation.operationJobName", getTaskName())) { //$NON-NLS-1$
			public IStatus run(IProgressMonitor monitor) {
				return CVSOperation.this.runInJob(monitor);
			}
		};
	}
	
	protected Job getWorkspaceJob() {
		return new WorkspaceJob(Policy.bind("CVSOperation.operationJobName", getTaskName())) { //$NON-NLS-1$
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				return CVSOperation.this.runInJob(monitor);
			}
		};
	}
	
	protected void fireDelta() {
		ResourcesPlugin.getWorkspace().checkpoint(false);
	}
	
	/**
	 * Run the operation. Progress feedback will be provided by one of the following mechanisms
	 * (in priotiry order):
	 * <ol>
	 * <li>the runnable context assigned to the operation 
	 * <li>a background job (if supported by the operation and enabled through the preferences)
	 * <li>the workbench active page
	 * </ol>
	 * @throws CVSException
	 * @throws InterruptedException
	 */
	public synchronized void run() throws CVSException, InterruptedException {
		if(canRunAsJob() && !hasRunnableContext() && areJobsEnabled()) {
			runAsJob();
		} else {
			runInContext(getRunnableContext());
		}
	}
	
	protected boolean areJobsEnabled() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.BACKGROUND_OPERATIONS);
	}

	/**
	 * Returns true if the operation can be run as a background job
	 * @return whether operation can be run as a job
	 */
	public boolean canRunAsJob() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		run(monitor, isModifiesWorkspace());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	private void run(IProgressMonitor monitor, boolean wrapInModifiyOperation) throws InvocationTargetException, InterruptedException {
		startOperation();
		try {
			if (wrapInModifiyOperation) {
				new CVSWorkspaceModifyOperation(this).run(monitor);
			} else {
				execute(monitor);
			}
			endOperation();
		} catch (CVSException e) {
			// TODO: errors may not be empty
			throw new InvocationTargetException(e);
		}
	}
	
	protected void startOperation() {
		statusCount = 0;
		resetErrors();
		confirmOverwrite = true;
	}
	
	protected void endOperation() throws CVSException {
		handleErrors((IStatus[]) errors.toArray(new IStatus[errors.size()]));
	}

	/**
	 * Subclasses must override to perform the operation
	 * @param monitor
	 * @throws CVSException
	 * @throws InterruptedException
	 */
	public abstract void execute(IProgressMonitor monitor) throws CVSException, InterruptedException;

	/**
	 * @return
	 */
	private IRunnableContext getRunnableContext() {
		if (runnableContext == null) {
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		}
		return runnableContext;
	}

	/**
	 * @param context
	 */
	public void setRunnableContext(IRunnableContext context) {
		this.runnableContext = context;
	}

	public boolean hasRunnableContext() {
		return runnableContext != null;
	}
	
	public Shell getShell() {
		if (isRunningAsJob()) {
			// We can't use the assigned shell as it may have been disposed
			// run in syncExec because callback is from an operation,
			// which is probably not running in the UI thread.
			final Shell[] newShell = new Shell[] { null };
			Display.getDefault().syncExec(
				new Runnable() {
					public void run() {
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if (window == null) {
							Display display = Display.getDefault();
							newShell[0] = new Shell(display);
						} else {
							newShell[0] = window.getShell();
						}
					}
				});
			return newShell[0];
		}
		return shell;
	}

	public void setShell(Shell shell) {
		this.shell = shell;
	}
	
	public boolean isInterruptable() {
		return interruptable;
	}
	
	public void setInterruptable(boolean b) {
		interruptable = b;
	}

	public boolean isModifiesWorkspace() {
		return modifiesWorkspace;
	}

	public void setModifiesWorkspace(boolean b) {
		modifiesWorkspace = b;
	}

	protected void addError(IStatus status) {
		if (status.isOK()) return;
		errors.add(status);
	}
	
	protected void collectStatus(IStatus status)  {
		statusCount++;
		if (!status.isOK()) addError(status);
	}
	
	protected void resetErrors() {
		errors.clear();
	}
	
	protected void handleErrors(IStatus[] errors) throws CVSException {
		if (errors.length == 0) return;
		if (errors.length == 1 && statusCount == 1)  {
			throw new CVSException(errors[0]);
		}
		MultiStatus result = new MultiStatus(CVSUIPlugin.ID, 0, getErrorMessage(errors, statusCount), null);
		for (int i = 0; i < errors.length; i++) {
			IStatus s = errors[i];
			if (s.isMultiStatus()) {
				result.add(new CVSStatus(s.getSeverity(), s.getMessage(), s.getException()));
				result.addAll(s);
			} else {
				result.add(s);
			}
		}
		throw new CVSException(result);
	}

	protected String getErrorMessage(IStatus[] failures, int totalOperations) {
		return "Errors occured in " + failures.length + " of " + totalOperations + " operations.";
	}

	/**
	 * This method prompts the user to overwrite an existing resource. It uses the
	 * <code>involvesMultipleResources</code> to determine what buttons to show.
	 * @param project
	 * @return
	 */
	protected boolean promptToOverwrite(String title, String msg) {
		if (!confirmOverwrite) {
			return true;
		}
		String buttons[];
		if (involvesMultipleResources()) {
			buttons = new String[] {
				IDialogConstants.YES_LABEL, 
				IDialogConstants.YES_TO_ALL_LABEL, 
				IDialogConstants.NO_LABEL, 
				IDialogConstants.CANCEL_LABEL};
		} else {
			buttons = new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL};
		}	
		Shell displayShell = getShell();
		final MessageDialog dialog = 
			new MessageDialog(displayShell, title, null, msg, MessageDialog.QUESTION, buttons, 0);

		// run in syncExec because callback is from an operation,
		// which is probably not running in the UI thread.
		displayShell.getDisplay().syncExec(
			new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		if (involvesMultipleResources()) {
			switch (dialog.getReturnCode()) {
				case 0://Yes
					return true;
				case 1://Yes to all
					confirmOverwrite = false; 
					return true;
				case 2://No
					return false;
				case 3://Cancel
				default:
					throw new OperationCanceledException();
			}
		} else {
			return dialog.getReturnCode() == 0;
		}
	}

	/**
	 * This method is used by <code>promptToOverwrite</code> to determine which 
	 * buttons to show in the prompter.
	 * 
	 * @return
	 */
	protected boolean involvesMultipleResources() {
		return involvesMultipleResources;
	}

	public void setInvolvesMultipleResources(boolean b) {
		involvesMultipleResources = b;
	}

	/**
	 * Return the string that is to be used as the task name for the operation
	 * 
	 * @param remoteFolders
	 * @return
	 */
	protected abstract String getTaskName();
	
	protected boolean isRunningAsJob() {
		return runningAsJob;
	}

}
