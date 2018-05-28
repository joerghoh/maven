package de.joerghoh.maven.contentpackagevalidation.mojos;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo (name="dumpContent", defaultPhase=LifecyclePhase.VERIFY )
public class ContentDumpMojo extends AbstractValidationMojo {
		
	@Parameter (property="validation.filename", defaultValue="${project.build.directory}/${project.build.finalName}")
	private File target;
	
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		if (!target.getName().endsWith(TARGET_EXTENSION)) {
			target = new File (target.getAbsolutePath() + TARGET_EXTENSION);
		}

		getFileContent(target).forEach(cpe -> getLog().info(cpe.getPath()));
		getLog().debug("Using " + target.getAbsolutePath() + " as input");
		
	}
	
}
