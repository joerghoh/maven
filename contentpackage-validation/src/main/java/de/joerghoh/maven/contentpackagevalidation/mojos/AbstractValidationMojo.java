/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.joerghoh.maven.contentpackagevalidation.mojos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ZipArchive;
import org.apache.jackrabbit.vault.fs.io.ZipStreamArchive;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

public abstract class AbstractValidationMojo extends AbstractMojo {
	
	protected static final String TARGET_EXTENSION = ".zip";
	
	private static final String SUBPACKAGE_EXPRESSION = "/jcr_root/etc/packages/.*.zip";
	
	
	List<ContentPackageEntry> getFileContent (File contentPackage) {
		ZipArchive archive = new ZipArchive(contentPackage);
		return getArchiveContent (archive, contentPackage.getName());

	}
	
	/**
	 * 
	 * @param archive
	 * @param filename the filename of the archive
	 * @return
	 */
	List<ContentPackageEntry> getArchiveContent (Archive archive, String archiveFilename) {
		List<ContentPackageEntry> content = new ArrayList<ContentPackageEntry>();
		
		try {
			archive.open(true);
			Archive.Entry root = archive.getRoot();
			analyzeEntry (root, "", archive, archiveFilename, content);
			
		} catch (IOException e) {
			getLog().error("Caught exception while analyzing content package", e);
		} finally {
			archive.close();
		}
		return content;
		
	}
	
	
	
	void analyzeEntry (Archive.Entry entry, String path, Archive archive,String archiveFilename, List<ContentPackageEntry> content) {
		if (! entry.isDirectory()) {
			ContentPackageEntry cpe = new ContentPackageEntry(path,entry, archive, archiveFilename);
			content.add(cpe);
		}
		
		if (path.matches(SUBPACKAGE_EXPRESSION)) {
			// recurse into a subpackage
			ZipStreamArchive zsa = null;
			try {
				zsa = new ZipStreamArchive(archive.openInputStream(entry));
				zsa.open(true);
				Archive.Entry rootNode = zsa.getRoot();
				String filename = String.format("%s:%s", archiveFilename,path);
				analyzeEntry (rootNode, "", zsa, filename, content);

			} catch (IOException e1) {
				String msg = String.format("Error while extracting subpackage %s", path);
				getLog().error(msg, e1);
			}finally {
				if (zsa != null) {
					zsa.close();
				}
			}
		}
	
		// recurse down the tree
		entry.getChildren().forEach(c -> {
			String childPath = path + "/" + c.getName();
			String msg = String.format("Checking node %s in archive %s", childPath, archiveFilename);
			getLog().debug(msg);
			analyzeEntry(c,childPath, archive, archiveFilename, content);
		});
		
	}
	
	
	
	String buildContentPackageName(MavenProject project) {
		String finalName = project.getBuild().getFinalName();
		return finalName + TARGET_EXTENSION;
	}
	
	boolean isContentPackage(MavenProject project) {
		return "content-package".equals(project.getPackaging());
	}

}
