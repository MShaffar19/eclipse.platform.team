package org.eclipse.team.internal.ccvs.ui.model;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class VersionCategory extends CVSModelElement implements IAdaptable {
	private ICVSRepositoryLocation repository;
	
	/**
	 * ProjectVersionsCategory constructor.
	 */
	public VersionCategory(ICVSRepositoryLocation repo) {
		super();
		this.repository = repo;
	}
	
	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	
	/**
	 * Returns the children of this object.  When this object
	 * is displayed in a tree, the returned objects will be this
	 * element's children.  Returns an empty enumeration if this
	 * object has no children.
	 */
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) throws TeamException {
		if (CVSUIPlugin.getPlugin().getRepositoryManager().isDisplayingProjectVersions(repository)) {
			return getProjectVersionChildren(o, monitor);
		} else {
			return getVersionTagChildren(o);
		}
	}

	/*
	 * Return the children as a list of versions whose children are projects
	 */
	private Object[] getVersionTagChildren(Object o) {
		CVSTag[] tags = CVSUIPlugin.getPlugin().getRepositoryManager().getKnownTags(repository, CVSTag.VERSION);
		CVSTagElement[] versionElements = new CVSTagElement[tags.length];
		for (int i = 0; i < tags.length; i++) {
			versionElements[i] = new CVSTagElement(tags[i], repository);
		}
		return versionElements;
	}
	
	/*
	 * Return the children as a list of projects whose children ar project versions
	 */
	private Object[] getProjectVersionChildren(Object o, IProgressMonitor monitor) throws TeamException {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		ICVSRemoteResource[] resources = repository.members(CVSTag.DEFAULT,
			store.getBoolean(ICVSUIConstants.PREF_SHOW_MODULES), monitor);
		Object[] modules = new Object[resources.length];
		for (int i = 0; i < resources.length; i++) {
			modules[i] = new RemoteModule((ICVSRemoteFolder)resources[i], VersionCategory.this);
		}
		return modules;
	}
	
	/**
	 * Returns an image descriptor to be used for displaying an object in the workbench.
	 * Returns null if there is no appropriate image.
	 *
	 * @param object The object to get an image descriptor for.
	 */
	public ImageDescriptor getImageDescriptor(Object object) {
		return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_VERSIONS_CATEGORY);
	}

	/**
	 * Returns the name of this element.  This will typically
	 * be used to assign a label to this object when displayed
	 * in the UI.  Returns an empty string if there is no appropriate
	 * name for this object.
	 *
	 * @param object The object to get a label for.
	 */
	public String getLabel(Object o) {
		return Policy.bind("VersionCategory.Versions_1"); //$NON-NLS-1$
	}

	/**
	 * Returns the logical parent of the given object in its tree.
	 * Returns null if there is no parent, or if this object doesn't
	 * belong to a tree.
	 *
	 * @param object The object to get the parent for.
	 */
	public Object getParent(Object o) {
		return repository;
	}
	
	/**
	 * Return the repository the given element belongs to.
	 */
	public ICVSRepositoryLocation getRepository(Object o) {
		return repository;
	}
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#isNeedsProgress()
	 */
	public boolean isNeedsProgress() {
		return true;
	}

}
