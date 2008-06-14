package no.bekk.fitnesse.plugins.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import fitnesse.wiki.PageData;
import fitnesse.wikitext.WidgetBuilder;
import fitnesse.wikitext.widgets.ClasspathWidget;
import fitnesse.wikitext.widgets.LineBreakWidget;
import fitnesse.wikitext.widgets.ParentWidget;
import fitnesse.wikitext.widgets.TextWidget;

public class PomWidget extends ClasspathWidget {

	static {
		PageData.classpathWidgetBuilder = new WidgetBuilder(new Class[] { ClasspathWidget.class, PomWidget.class });
	}

	public static final String REGEXP = "^!pom [^\r\n]*";
	private static final Pattern pattern = Pattern.compile("^!pom (.*)");

	List<String> prioritzedClasspathElements = new ArrayList<String>();
	List<String> classpathElements = new ArrayList<String>();

	private String pomFile;
	boolean generateText;

	private ParentWidget parent;
	private MavenEmbedder mavenEmbedder;
	MavenProject mavenProject;
	private Matcher matcher;
	boolean embedderStarted;

	public PomWidget(ParentWidget parent, String inputText) throws Exception {
		super(parent, "");
		this.parent = parent;
		matcher = pattern.matcher(inputText);
		mavenEmbedder = new MavenEmbedder();
	}

	PomWidget insertLineBreak() {
			new LineBreakWidget(parent, "");
		return this;
	}

	PomWidget startMavenEmbedder() throws MavenEmbedderException {
		mavenEmbedder.setClassLoader(Thread.currentThread().getContextClassLoader());
		mavenEmbedder.start();
		embedderStarted = true;
		return this;
	}

	PomWidget stopMavenEmbedder() throws MavenEmbedderException {
		if (embedderStarted) {
			mavenEmbedder.stop();
			embedderStarted = false;
		}
		return this;
	}

	PomWidget readMavenProjectFromPom() throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException {
		if (matcher.find()) {
			pomFile = matcher.group(1);
			mavenProject = mavenEmbedder.readProjectWithDependencies(new File(pomFile));
		}
		return this;
	}

	PomWidget findOutputDirs() {
		Build mavenBuild = mavenProject.getBuild();
		if (mavenBuild != null) {
			String outputDir = mavenBuild.getOutputDirectory();
			String testOurputDir = mavenBuild.getTestOutputDirectory();
			classpathElements.add(outputDir);
			classpathElements.add(testOurputDir);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	PomWidget findArtifacts() {
		Set<Artifact> artifacts = mavenProject.getArtifacts();
		if (artifacts != null) {
			for (Artifact artifact : artifacts) {
				File file = artifact.getFile();
				if (file != null) {
					classpathElements.add(file.getAbsolutePath());
				}
			}
		}
		return this;
	}

	private void handleError(Exception e, ParentWidget parent) {
		e.printStackTrace();
		new TextWidget(parent, e.toString());
		new LineBreakWidget(parent, "");
		new TextWidget(parent, "(Full stacktrace in FitNesse log.)");
	}

	public static class Builder {
		private MavenEmbedder mavenEmbedder;
		private MavenProject mavenProject;
		public String inputText;

		public Builder(String inputText) {
			this.inputText = inputText;
		}

		public Builder mavenEmbedder(MavenEmbedder mavenEmbedder) {
			this.mavenEmbedder = mavenEmbedder;
			return this;
		}

		public Builder mavenProject(MavenProject mavenProject) {
			this.mavenProject = mavenProject;
			return this;
		}

		public PomWidget build() throws Exception {
			return new PomWidget(this);
		}
	}

	private PomWidget(Builder builder) throws Exception {
		super(null, builder.inputText);
		this.mavenEmbedder = builder.mavenEmbedder;
		this.mavenProject = builder.mavenProject;

		this.matcher = pattern.matcher(builder.inputText);
	}
}
