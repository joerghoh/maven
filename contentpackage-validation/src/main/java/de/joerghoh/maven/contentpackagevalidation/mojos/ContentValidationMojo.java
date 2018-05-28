package de.joerghoh.maven.contentpackagevalidation.mojos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ZipStreamArchive;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo (name="validate", defaultPhase=LifecyclePhase.VERIFY, requiresProject=false )
public class ContentValidationMojo extends AbstractValidationMojo {
	
	private static final String SUBPACKAGE_EXPRESSION = "/jcr_root/etc/packages/.*.zip";
	
	
	@Parameter (property="validation.filteredPaths")
	ArrayList<String> filteredPaths;
	
	@Parameter (property="validation.filename", defaultValue="${project.build.directory}/${project.build.finalName}")
	private File target;
	
	@Parameter(property="validation.breakBuildOnValiationFailures", defaultValue="false")
	private boolean breakBuild;
	
	@Parameter(property="validation.allowSubpackages", defaultValue="true")
	private boolean allowSubpackages;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		if (filteredPaths.size() == 0 && allowSubpackages) {
			getLog().info("No filters and no subpackage restriction given, skipping");
			return;
		}
		
		if (!target.getName().endsWith(TARGET_EXTENSION)) {
			target = new File (target.getAbsolutePath() + TARGET_EXTENSION);
		}
		List<String> policyViolations = new ArrayList<>();
		
		
		validatePackage(target, policyViolations);
		if (policyViolations.size() > 0) {
			getLog().warn(policyViolations.size() + " violation(s) against policy (" + String.join(",", filteredPaths) + ")");
			policyViolations.forEach(s -> getLog().warn(s));
			if (breakBuild) {
				throw new MojoExecutionException("policy violation detected, please check build logs");
			}
		}
		
	}

	private void validatePackage(File zipFile, List<String> policyViolations ) {
		List<ContentPackageEntry> violations = getFileContent(zipFile).stream()
			.filter (cpe -> applyPathFilterRules(cpe,policyViolations))
			.filter (cpe -> checkForSubpackages(cpe, policyViolations))
			.collect(Collectors.toList());
	}
	
	private void validateArchive (Archive archive, String filename, List<String> policyViolations) {
		getLog().info("Checking package " + filename);
		List<ContentPackageEntry> violations = getArchiveContent(archive, filename).stream()
				.filter (cpe -> applyPathFilterRules(cpe,policyViolations))
				.filter (cpe -> checkForSubpackages(cpe, policyViolations))
				.collect(Collectors.toList());
	}
	

	boolean applyPathFilterRules (ContentPackageEntry cpe, List<String> policyViolations) {
		
		boolean violatesPolicy = filteredPaths.stream()
				.filter((String regex) -> cpe.getPath().matches(regex))
				.findFirst().isPresent();
		if (violatesPolicy) {
			String msg = String.format("[%s] detected violation of path rules: %s", cpe.getArchiveFilename(),cpe.getPath());
			policyViolations.add(msg);
		}
		return !violatesPolicy;
	}
	
	boolean checkForSubpackages (ContentPackageEntry cpe, List<String> policyViolations) {
		boolean isSubPackage = cpe.getPath().matches(SUBPACKAGE_EXPRESSION);
		if (isSubPackage && !allowSubpackages) {
			String msg = String.format("detected subpackage at: %s", cpe.getPath());
			policyViolations.add(msg);
		}
		if (isSubPackage) {
			// recurse
			try {
				ZipStreamArchive zsa = new ZipStreamArchive(cpe.getArchive().openInputStream(cpe.getEntry()));
				//Archive subArchive = cpe.getArchive().getSubArchive(cpe.getPath(), true);
				validateArchive (zsa,String.format("%s:%s", cpe.getArchiveFilename(),cpe.getPath()),policyViolations);
			} catch (IOException ioe) {
				String msg = String.format("Exception during extraction of subpackage %s of archive %s", cpe.getPath(),cpe.getArchive());
				getLog().error(msg);
			}
		}
		return !isSubPackage;
	}
	

	

	
	
}
