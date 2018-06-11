package de.joerghoh.maven.contentpackagevalidation.mojos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

		reportViolations(validatePackage(target));
		
	}

	/**
	 * Validate the content package
	 * @param zipFile the input file
	 * @return the policy violations
	 */
	private List<String> validatePackage(File zipFile) {
		List<String> policyViolations = new ArrayList<>();
		List<ContentPackageEntry> violations = getFileContent(zipFile).stream()
			.filter (cpe -> applyPathFilterRules(cpe,policyViolations))
			.filter (cpe -> checkForSubpackages(cpe, policyViolations))
			.collect(Collectors.toList());
		return policyViolations;
	}
	
	/**
	 * report policy violations
	 * @param policyViolations
	 * @throws MojoExecutionException
	 */
	private void reportViolations(List<String> policyViolations) throws MojoExecutionException {
		if (policyViolations.size() > 0) {
			String msg = String.format("%d violation(s) against policy (%s)", policyViolations.size(),String.join(",", filteredPaths)); 
			getLog().warn(msg);
			policyViolations.forEach(s -> getLog().warn(s));
			if (breakBuild) {
				throw new MojoExecutionException("policy violation detected, please check build logs");
			}
		}
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
		return !isSubPackage;
	}
	

	

	
	
}
