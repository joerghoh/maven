package de.joerghoh.maven.contentpackagevalidation.mojos;

import org.apache.jackrabbit.vault.fs.io.Archive;

public class ContentPackageEntry {
	
	private String path;
	private Archive.Entry entry;
	private Archive archive;
	private String archiveFilename;
	
	public ContentPackageEntry (String path,Archive.Entry entry, Archive archive, String archiveFilename) {
		this.path = path;
		this.entry = entry;
		this.archive = archive;
		this.archiveFilename = archiveFilename;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public Archive.Entry getEntry() {
		return entry;
	}

	public Archive getArchive() {
		return archive;
	}
	
	public String getArchiveFilename() {
		return archiveFilename;
	}
}
