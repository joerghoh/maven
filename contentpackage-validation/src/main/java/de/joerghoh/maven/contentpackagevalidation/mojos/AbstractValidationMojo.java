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
		
		if (path.matches(SUBPACKAGE_EXPRESSION)) {
			// recurse into a subpackage
			ZipStreamArchive zsa = null;
			try {
				zsa = new ZipStreamArchive(archive.openInputStream(e));
				zsa.open(true);
				Archive.Entry rootNode = zsa.getRoot();
				String filename = String.format("%s:%s", archiveFilename,path);
				analyzeEntry (rootNode, "", zsa, filename, content);

			} catch (IOException e1) {
				String msg = String.format("Error while extracting subpackage %s", path);
				getLog().error(msg);
			}finally {
				if (zsa != null) {
					zsa.close();
				}
			}
		}
	
		// recurse down the tree
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
