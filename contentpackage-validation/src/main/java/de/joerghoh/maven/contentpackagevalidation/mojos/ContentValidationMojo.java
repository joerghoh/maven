/*
 * 
 * 
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
	
	@Parameter (property="validation.whitelistedPaths")
	ArrayList<String> whitelistedPaths;
	
	@Parameter (property="validation.filename", defaultValue="${project.build.directory}/${project.build.finalName}")
	private File target;
	
	@Parameter(property="validation.breakBuildOnValiationFailures", defaultValue="false")
	private boolean breakBuild;
	
	@Parameter(property="validation.allowSubpackages", defaultValue="true")
	private boolean allowSubpackages;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		if (!target.getName().endsWith(TARGET_EXTENSION)) {
			target = new File (target.getAbsolutePath() + TARGET_EXTENSION);
		}
		if (!target.exists()) {
			getLog().error(String.format("File %s does not exist", target.getAbsolutePath()));
			return;
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
			.filter (cpe -> filterPathViolations(cpe,policyViolations))
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
			String msg = String.format("%d violation(s) against policy (%s)", policyViolations.size(), getPolicyString()); 
			getLog().warn(msg);
			policyViolations.forEach(s -> getLog().warn(s));
			if (breakBuild) {
				throw new MojoExecutionException("policy violation detected, please check build logs");
			}
		}
	}
	
	/**
	 * validate a content package entry against the path filter rules
	 * @param cpe
	 * @param policyViolations the list of violations
	 * @return false if the violation is deteced, true otherwise
	 */
	boolean filterPathViolations (ContentPackageEntry cpe, List<String> policyViolations) {
		
		List<String> positiveStatements = new ArrayList<>();
		List<String> negativeStatements = new ArrayList<>();
		
		whitelistedPaths.forEach(regex -> {
			//getLog().info(String.format("checking %s", regex));
			if (regex.startsWith("!")) {
				negativeStatements.add(regex.substring(1));
			} else {
				positiveStatements.add(regex);
				
			}
		});
			
		// there must be at least 1 match on the positiveStatement list
		boolean positiveMatch = positiveStatements.stream()
				.filter((String regex) -> cpe.getPath().matches(regex))
				.findFirst().isPresent();
		
		// but no match on the negativeStatement list
		boolean negativeMatch = negativeStatements.stream()
				.filter((String regex) -> cpe.getPath().matches(regex))
				.findFirst().isPresent();
		
		//getLog().info(String.format("%s: positiveMatch=%s, negativeMatch=%s",cpe.getPath(),positiveMatch, negativeMatch));
		boolean isCompliant = (positiveMatch && !negativeMatch);
		if (! isCompliant) {
			String msg = String.format("[%s] detected violation of path rules: %s", cpe.getArchiveFilename(),cpe.getPath());
			getLog().debug(msg);
			policyViolations.add(msg);
		}
		return isCompliant;
	}
	
	boolean checkForSubpackages (ContentPackageEntry cpe, List<String> policyViolations) {
		boolean isSubPackage = cpe.getPath().matches(SUBPACKAGE_EXPRESSION);
		if (isSubPackage && !allowSubpackages) {
			String msg = String.format("detected subpackage at: %s", cpe.getPath());
			policyViolations.add(msg);
		}
		return !isSubPackage;
	}
	

	String getPolicyString() {
		return String.format("whitelisted paths = [%s], allowSubpackages = %s",String.join(",", whitelistedPaths),allowSubpackages );
	}
	

	
	
}
