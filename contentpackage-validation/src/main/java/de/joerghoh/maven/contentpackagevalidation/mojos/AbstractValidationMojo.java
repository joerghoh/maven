package de.joerghoh.maven.contentpackagevalidation.mojos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ZipArchive;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

public abstract class AbstractValidationMojo extends AbstractMojo {
	
	protected static final String TARGET_EXTENSION = ".zip";
	
	
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
//			archive.close();
		}
		//getLog().info("inside getArchiveContent for " + archiveFilename + " with entries " + content.size() );
		return content;
		
	}
	
	
	
	void analyzeEntry (Archive.Entry e, String path, Archive archive,String archiveFilename, List<ContentPackageEntry> content) {
		if (! e.isDirectory()) {
			ContentPackageEntry cpe = new ContentPackageEntry(path,e, archive, archiveFilename);
			content.add(cpe);
		}
		// recurse
		e.getChildren().forEach(c -> {
			String childPath = path + "/" + c.getName();
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
