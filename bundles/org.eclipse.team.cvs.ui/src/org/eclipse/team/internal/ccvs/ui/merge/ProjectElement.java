package org.eclipse.team.internal.ccvs.ui.merge;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.util.ArrayList;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ProjectElement implements IAdaptable, IWorkbenchAdapter {
	ICVSFolder project;
	TagRootElement branches;
	TagRootElement versions;
	int includeFlags;
	
	public static final int INCLUDE_HEAD_TAG = 1;
	public static final int INCLUDE_BASE_TAG = 2;
	public static final int INCLUDE_BRANCHES = 4;
	public static final int INCLUDE_VERSIONS = 8;
	public static final int INCLUDE_ALL_TAGS = INCLUDE_HEAD_TAG | INCLUDE_BASE_TAG | INCLUDE_BRANCHES | INCLUDE_VERSIONS;
	
	public ProjectElement(ICVSFolder project, int includeFlags) {
		this.project = project;
		this.includeFlags = includeFlags;
		if (this.includeFlags == 0) this.includeFlags = INCLUDE_ALL_TAGS;
		if ((includeFlags & INCLUDE_BRANCHES) > 0) {	
			branches = new TagRootElement(project, CVSTag.BRANCH);
		}
		if ((includeFlags & INCLUDE_VERSIONS) > 0) {
			versions = new TagRootElement(project, CVSTag.VERSION);
		}
	}
	
	public Object[] getChildren(Object o) {
		ArrayList children = new ArrayList(4);
		if ((includeFlags & INCLUDE_HEAD_TAG) > 0) {
			children.add(new TagElement(CVSTag.DEFAULT));
		}
		if ((includeFlags & INCLUDE_BASE_TAG) > 0) {
			children.add(new TagElement(CVSTag.BASE));
		}
		if ((includeFlags & INCLUDE_BRANCHES) > 0) {
			children.add(branches);
		}
		if ((includeFlags & INCLUDE_VERSIONS) > 0) {
			children.add(versions);
		}
		return (Object[]) children.toArray(new Object[children.size()]);
	}
	public int getIncludeFlags() {
		return includeFlags;
	}
	public TagRootElement getBranches() {
		return branches;
	}
	public TagRootElement getVersions() {
		return versions;
	}	
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}
	public String getLabel(Object o) {
		return null;
	}
	public Object getParent(Object o) {
		return null;
	}
}
