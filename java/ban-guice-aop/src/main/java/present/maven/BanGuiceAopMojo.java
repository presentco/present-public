package present.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Allow "com.google.inject:guice:no_aop" but not "com.google.inject:guice". Generating classes
 * slows down App Engine and we don't want two different versions of Guice in the classpath.
 */
@Mojo(name = "ban-guice-aop", requiresDependencyResolution = ResolutionScope.COMPILE)
public class BanGuiceAopMojo extends AbstractMojo {
  @Parameter(
      defaultValue = "${project}",
      required = true,
      readonly = true)
  private MavenProject project;

  @Override public void execute() throws MojoExecutionException, MojoFailureException {
    for (Artifact artifact : project.getArtifacts()) {
      if (artifact.getGroupId().equals("com.google.inject")
          && artifact.getArtifactId().equals("guice")
          && !"no_aop".equals(artifact.getClassifier())) {
        throw new MojoFailureException("Found Guice without required 'no_aop' classifier: "
            + artifact.getDependencyTrail());
      }
    }
  }
}
